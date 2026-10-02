package com.xiaomanjun.sleepdownschedule.feature.todo

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.xiaomanjun.sleepdownschedule.data.local.CalendarEventLocalIdStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class TodoCalendarIssueType {
    LEGACY_ARCHIVE,
    MISSING_EVENT,
    DUPLICATE_EVENTS,
    INTERRUPTED_INSERT,
    MARKER_NOT_PERSISTED
}

data class TodoCalendarSyncIssue(
    val todoId: Long,
    val title: String,
    val type: TodoCalendarIssueType,
    val localEventId: Long? = null
)

data class TodoCalendarSyncReport(
    val syncedCount: Int,
    val issues: List<TodoCalendarSyncIssue>
)

internal sealed interface CalendarMarkerLookup {
    data object Missing : CalendarMarkerLookup
    data class Unique(val eventId: Long) : CalendarMarkerLookup
    data class Ambiguous(val eventIds: List<Long>) : CalendarMarkerLookup
}

internal fun classifyCalendarMarkerMatches(eventIds: List<Long>): CalendarMarkerLookup {
    val ids = eventIds.distinct()
    return when {
        ids.isEmpty() -> CalendarMarkerLookup.Missing
        ids.size == 1 -> CalendarMarkerLookup.Unique(ids.single())
        else -> CalendarMarkerLookup.Ambiguous(ids)
    }
}

internal fun missingCalendarMarkerIssueType(
    hasStableToken: Boolean,
    hasDeviceLocalEventId: Boolean
): TodoCalendarIssueType = when {
    !hasStableToken -> TodoCalendarIssueType.LEGACY_ARCHIVE
    hasDeviceLocalEventId -> TodoCalendarIssueType.MARKER_NOT_PERSISTED
    else -> TodoCalendarIssueType.MISSING_EVENT
}

class TodoCalendarSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: TodoRepository
) {
    init {
        CalendarEventLocalIdStore.initialize(context)
    }

    fun hasCalendarPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED

    suspend fun syncAll(): TodoCalendarSyncReport = withContext(Dispatchers.IO) {
        calendarMutationMutex.withLock {
            check(hasCalendarPermission()) { "需要读取和写入日历权限" }
            drainPendingRemovalsLocked()
            val calendarId = findWritableCalendar() ?: error("没有可写入的系统日历")
            var count = 0
            val issues = mutableListOf<TodoCalendarSyncIssue>()
            repository.itemsForSync().forEach { item ->
                if (item.isCompleted || item.dueAt == null || item.parentId != null) {
                    if (item.calendarEventId != null || item.calendarSyncToken != null) removeLocked(item)
                    return@forEach
                }
                if (item.calendarSyncState == TodoCalendarSyncState.SKIPPED) return@forEach

                when (item.calendarSyncState) {
                    TodoCalendarSyncState.NEEDS_CONFIRMATION -> {
                        when (val match = item.calendarSyncToken?.let(::findEventsByMarker)
                            ?.let(::classifyCalendarMarkerMatches) ?: CalendarMarkerLookup.Missing) {
                            is CalendarMarkerLookup.Unique -> count += updateAndVerify(item, item.calendarSyncToken!!, match.eventId, calendarId, issues)
                            is CalendarMarkerLookup.Ambiguous -> issues += issue(item, TodoCalendarIssueType.DUPLICATE_EVENTS)
                            CalendarMarkerLookup.Missing -> issues += issue(
                                item,
                                missingCalendarMarkerIssueType(
                                    hasStableToken = item.calendarSyncToken != null,
                                    hasDeviceLocalEventId = item.calendarEventId != null ||
                                        item.calendarSyncToken?.let(CalendarEventLocalIdStore::get) != null
                                )
                            )
                        }
                    }
                    TodoCalendarSyncState.CREATING -> {
                        val token = item.calendarSyncToken
                        when (val match = token?.let(::findEventsByMarker)
                            ?.let(::classifyCalendarMarkerMatches) ?: CalendarMarkerLookup.Missing) {
                            is CalendarMarkerLookup.Unique -> count += updateAndVerify(item, token!!, match.eventId, calendarId, issues)
                            is CalendarMarkerLookup.Ambiguous -> issues += markNeedsConfirmation(item, TodoCalendarIssueType.DUPLICATE_EVENTS)
                            CalendarMarkerLookup.Missing -> issues += markNeedsConfirmation(item, TodoCalendarIssueType.INTERRUPTED_INSERT)
                        }
                    }
                    TodoCalendarSyncState.LINKED, TodoCalendarSyncState.LOCAL_ONLY -> {
                        val token = item.calendarSyncToken
                        val localId = item.calendarEventId ?: token?.let(CalendarEventLocalIdStore::get)
                        if (token == null) {
                            issues += markNeedsConfirmation(item, TodoCalendarIssueType.LEGACY_ARCHIVE)
                        } else if (item.calendarSyncState == TodoCalendarSyncState.LOCAL_ONLY && localId != null) {
                            count += updateLocalOnly(item, token, localId, calendarId, issues)
                        } else if (localId != null) {
                            count += updateAndVerify(item, token, localId, calendarId, issues)
                        } else {
                            when (val match = classifyCalendarMarkerMatches(findEventsByMarker(token))) {
                                is CalendarMarkerLookup.Unique -> count += updateAndVerify(item, token, match.eventId, calendarId, issues)
                                is CalendarMarkerLookup.Ambiguous -> issues += markNeedsConfirmation(item, TodoCalendarIssueType.DUPLICATE_EVENTS)
                                CalendarMarkerLookup.Missing -> issues += markNeedsConfirmation(item, TodoCalendarIssueType.MISSING_EVENT)
                            }
                        }
                    }
                    else -> count += syncPending(item, calendarId, issues)
                }
            }
            TodoCalendarSyncReport(count, issues)
        }
    }

    /** Called only after the user explicitly chooses how an unresolved restored task is handled. */
    suspend fun resolveReview(todoId: Long, createNew: Boolean, keepLocalEvent: Boolean = false) {
        val item = repository.getById(todoId) ?: return
        if (item.deletedAt != null) return
        val token = item.calendarSyncToken
        val nextState = when {
            !createNew -> TodoCalendarSyncState.SKIPPED
            keepLocalEvent && token != null && CalendarEventLocalIdStore.get(token) != null -> TodoCalendarSyncState.LOCAL_ONLY
            else -> TodoCalendarSyncState.PENDING
        }
        if (nextState == TodoCalendarSyncState.PENDING && token != null) {
            CalendarEventLocalIdStore.remove(token)
        }
        repository.setCalendarSyncData(
            id = item.id,
            eventId = null,
            token = token,
            state = nextState
        )
    }

    suspend fun remove(item: TodoItemEntity) = withContext(Dispatchers.IO) {
        calendarMutationMutex.withLock { removeLocked(item) }
    }

    suspend fun archive(item: TodoItemEntity) = withContext(Dispatchers.IO) {
        calendarMutationMutex.withLock {
            val rows = repository.itemsForSync()
            val ids = todoDescendantIds(item.id, rows).toSet()
            repository.delete(item)
            rows.filter { it.id in ids }.forEach { removeLocked(it) }
        }
    }

    suspend fun restore(item: TodoItemEntity) = withContext(Dispatchers.IO) {
        calendarMutationMutex.withLock {
            // Reconcile pending owned-event removals before allowing a restored task to sync.
            // Without permission retain its token and block automatic creation until cleanup succeeds.
            if (hasCalendarPermission()) drainPendingRemovalsLocked()
            repository.restore(item)
        }
    }

    suspend fun purge(item: TodoItemEntity) = withContext(Dispatchers.IO) {
        calendarMutationMutex.withLock {
            val current = repository.getById(item.id)?.takeIf { it.deletedAt != null }
                ?: return@withLock
            removeLocked(current)
            repository.purge(current)
        }
    }

    suspend fun cleanupTrash(now: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        calendarMutationMutex.withLock {
            val expired = repository.expiredItems(now)
            expired.forEach { removeLocked(it) }
            repository.purgeExpired(expired.map { it.id })
        }
    }

    private suspend fun syncPending(
        item: TodoItemEntity,
        calendarId: Long,
        issues: MutableList<TodoCalendarSyncIssue>
    ): Int {
        if (item.calendarSyncState != TodoCalendarSyncState.PENDING) {
            issues += markNeedsConfirmation(item, TodoCalendarIssueType.INTERRUPTED_INSERT)
            return 0
        }
        val token = item.calendarSyncToken ?: UUID.randomUUID().toString()
        // Persist the identity and in-flight marker before touching the external provider. If the
        // process dies after insert, the next pass can find the event or stop for confirmation.
        repository.setCalendarSyncData(item.id, null, token, TodoCalendarSyncState.CREATING)
        when (val match = classifyCalendarMarkerMatches(findEventsByMarker(token))) {
            is CalendarMarkerLookup.Unique -> return updateAndVerify(item, token, match.eventId, calendarId, issues)
            is CalendarMarkerLookup.Ambiguous -> {
                issues += markNeedsConfirmation(item.copy(calendarSyncToken = token), TodoCalendarIssueType.DUPLICATE_EVENTS)
                return 0
            }
            CalendarMarkerLookup.Missing -> Unit
        }

        val values = eventValues(item, calendarId, token)
        val eventUri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            ?: error("系统日历拒绝写入：${item.title}")
        val eventId = eventUri.lastPathSegment?.toLongOrNull()
            ?: error("系统日历未返回有效事件 ID：${item.title}")
        return when (val verification = classifyCalendarMarkerMatches(findEventsByMarker(token))) {
            is CalendarMarkerLookup.Unique -> {
                if (verification.eventId == eventId) {
                    repository.setCalendarSyncData(item.id, null, token, TodoCalendarSyncState.LINKED)
                    1
                } else {
                    issues += markNeedsConfirmation(item.copy(calendarSyncToken = token, calendarEventId = eventId), TodoCalendarIssueType.DUPLICATE_EVENTS)
                    0
                }
            }
            is CalendarMarkerLookup.Ambiguous -> {
                issues += markNeedsConfirmation(item.copy(calendarSyncToken = token, calendarEventId = eventId), TodoCalendarIssueType.DUPLICATE_EVENTS)
                0
            }
            CalendarMarkerLookup.Missing -> {
                CalendarEventLocalIdStore.put(token, eventId)
                issues += markNeedsConfirmation(item.copy(calendarSyncToken = token, calendarEventId = eventId), TodoCalendarIssueType.MARKER_NOT_PERSISTED)
                0
            }
        }
    }

    private suspend fun updateAndVerify(
        item: TodoItemEntity,
        token: String,
        eventId: Long,
        calendarId: Long,
        issues: MutableList<TodoCalendarSyncIssue>
    ): Int {
        val updated = context.contentResolver.update(
            CalendarContract.Events.CONTENT_URI,
            eventValues(item, calendarId, token),
            "${CalendarContract.Events._ID} = ?",
            arrayOf(eventId.toString())
        )
        if (updated == 0) {
            return when (val match = classifyCalendarMarkerMatches(findEventsByMarker(token))) {
                is CalendarMarkerLookup.Unique -> if (match.eventId == eventId) {
                    issues += markNeedsConfirmation(item.copy(calendarSyncToken = token), TodoCalendarIssueType.MISSING_EVENT)
                    0
                } else updateAndVerify(item.copy(calendarEventId = null), token, match.eventId, calendarId, issues)
                is CalendarMarkerLookup.Ambiguous -> {
                    issues += markNeedsConfirmation(item.copy(calendarSyncToken = token), TodoCalendarIssueType.DUPLICATE_EVENTS)
                    0
                }
                CalendarMarkerLookup.Missing -> {
                    issues += markNeedsConfirmation(item.copy(calendarSyncToken = token), TodoCalendarIssueType.MISSING_EVENT)
                    0
                }
            }
        }
        return when (val match = classifyCalendarMarkerMatches(findEventsByMarker(token))) {
            is CalendarMarkerLookup.Unique -> if (match.eventId == eventId) {
                CalendarEventLocalIdStore.remove(token)
                repository.setCalendarSyncData(item.id, null, token, TodoCalendarSyncState.LINKED)
                1
            } else {
                issues += markNeedsConfirmation(item.copy(calendarSyncToken = token, calendarEventId = eventId), TodoCalendarIssueType.DUPLICATE_EVENTS)
                1
            }
            is CalendarMarkerLookup.Ambiguous -> {
                issues += markNeedsConfirmation(item.copy(calendarSyncToken = token, calendarEventId = eventId), TodoCalendarIssueType.DUPLICATE_EVENTS)
                1
            }
            CalendarMarkerLookup.Missing -> {
                CalendarEventLocalIdStore.put(token, eventId)
                issues += markNeedsConfirmation(item.copy(calendarSyncToken = token, calendarEventId = eventId), TodoCalendarIssueType.MARKER_NOT_PERSISTED)
                1
            }
        }
    }

    private suspend fun updateLocalOnly(
        item: TodoItemEntity,
        token: String,
        eventId: Long,
        calendarId: Long,
        issues: MutableList<TodoCalendarSyncIssue>
    ): Int {
        val updated = context.contentResolver.update(
            CalendarContract.Events.CONTENT_URI,
            eventValues(item, calendarId, token),
            "${CalendarContract.Events._ID} = ?",
            arrayOf(eventId.toString())
        )
        if (updated == 0) {
            issues += markNeedsConfirmation(item.copy(calendarSyncToken = token), TodoCalendarIssueType.MISSING_EVENT)
            return 0
        }
        when (val match = classifyCalendarMarkerMatches(findEventsByMarker(token))) {
            is CalendarMarkerLookup.Unique -> if (match.eventId == eventId) {
                CalendarEventLocalIdStore.remove(token)
                repository.setCalendarSyncData(item.id, null, token, TodoCalendarSyncState.LINKED)
            }
            is CalendarMarkerLookup.Ambiguous -> issues += markNeedsConfirmation(
                item.copy(calendarEventId = eventId, calendarSyncToken = token),
                TodoCalendarIssueType.DUPLICATE_EVENTS
            )
            CalendarMarkerLookup.Missing -> {
                CalendarEventLocalIdStore.put(token, eventId)
                issues += markNeedsConfirmation(item.copy(calendarSyncToken = token), TodoCalendarIssueType.MARKER_NOT_PERSISTED)
            }
        }
        return 1
    }

    private fun eventValues(item: TodoItemEntity, calendarId: Long, token: String): ContentValues {
        val due = requireNotNull(item.dueAt)
        val zone = ZoneId.systemDefault()
        val start = if (item.allDay) Instant.ofEpochMilli(due).atZone(zone).toLocalDate()
            .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli() else due
        val end = if (item.allDay) start + DAY_MILLIS else item.endAt ?: (due + EVENT_DURATION_MILLIS)
        return ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, item.title)
            put(CalendarContract.Events.DESCRIPTION, item.description)
            put(CalendarContract.Events.DTSTART, start)
            put(CalendarContract.Events.DTEND, end)
            put(CalendarContract.Events.EVENT_TIMEZONE, if (item.allDay) "UTC" else zone.id)
            put(CalendarContract.Events.ALL_DAY, if (item.allDay) 1 else 0)
            put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
            put(CalendarContract.Events.CUSTOM_APP_PACKAGE, context.packageName)
            put(CalendarContract.Events.CUSTOM_APP_URI, markerUri(token))
        }
    }

    private fun markerUri(token: String): String = "scheduleplus://todo/$token"

    private fun findEventsByMarker(token: String): List<Long> {
        val marker = markerUri(token)
        return context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            arrayOf(CalendarContract.Events._ID),
            "${CalendarContract.Events.CUSTOM_APP_PACKAGE} = ? AND ${CalendarContract.Events.CUSTOM_APP_URI} = ?",
            arrayOf(context.packageName, marker),
            null
        )?.use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getLong(0)) }
        }.orEmpty()
    }

    private suspend fun removeLocked(item: TodoItemEntity) {
        val token = item.calendarSyncToken
        val id = item.calendarEventId ?: token?.let(CalendarEventLocalIdStore::get)
        if (id == null && token == null) return
        if (!hasCalendarPermission()) {
            if (id != null) enqueuePendingRemoval(id)
            if (token != null) enqueuePendingTokenRemoval(token)
            if (token != null) CalendarEventLocalIdStore.remove(token)
            repository.setCalendarSyncData(item.id, null, token, TodoCalendarSyncState.PENDING)
            return
        }
        try {
            if (id != null) {
                deleteCalendarEvent(id)
            } else if (token != null) {
                when (val match = classifyCalendarMarkerMatches(findEventsByMarker(token))) {
                    is CalendarMarkerLookup.Unique -> deleteCalendarEvent(match.eventId)
                    is CalendarMarkerLookup.Ambiguous -> {
                        repository.setCalendarSyncData(item.id, null, token, TodoCalendarSyncState.NEEDS_CONFIRMATION)
                        return
                    }
                    CalendarMarkerLookup.Missing -> Unit
                }
            }
            if (id != null) clearPendingRemoval(id)
            if (token != null) clearPendingTokenRemoval(token)
            if (token != null) CalendarEventLocalIdStore.remove(token)
            repository.setCalendarSyncData(item.id, null, token, TodoCalendarSyncState.PENDING)
        } catch (error: RuntimeException) {
            Log.w(TAG, "Could not remove todo calendar event for task ${item.id}; cleanup will be retried", error)
            if (id != null) enqueuePendingRemoval(id)
            if (token != null) enqueuePendingTokenRemoval(token)
            if (token != null) CalendarEventLocalIdStore.remove(token)
            repository.setCalendarSyncData(item.id, null, token, TodoCalendarSyncState.PENDING)
        }
    }

    private suspend fun drainPendingRemovalsLocked() {
        pendingRemovalIds().forEach { id ->
            try {
                deleteCalendarEvent(id)
            } catch (error: RuntimeException) {
                throw IllegalStateException("无法清理此前同步的日历事项，请检查日历权限后重试", error)
            }
            clearPendingRemoval(id)
        }
        pendingTokenRemovals().forEach { token ->
            try {
                when (val match = classifyCalendarMarkerMatches(findEventsByMarker(token))) {
                    is CalendarMarkerLookup.Unique -> deleteCalendarEvent(match.eventId)
                    is CalendarMarkerLookup.Ambiguous -> throw IllegalStateException("发现多个关联日历事项，请先在系统日历中处理重复项")
                    CalendarMarkerLookup.Missing -> Unit
                }
            } catch (error: RuntimeException) {
                throw IllegalStateException("无法清理此前同步的日历事项，请检查日历权限后重试", error)
            }
            clearPendingTokenRemoval(token)
        }
    }

    private fun deleteCalendarEvent(id: Long) {
        context.contentResolver.delete(
            CalendarContract.Events.CONTENT_URI,
            "${CalendarContract.Events._ID} = ?",
            arrayOf(id.toString())
        )
    }

    private fun pendingRemovalIds(): Set<Long> = pendingRemovalPreferences()
        .getStringSet(KEY_PENDING_REMOVAL_IDS, emptySet())
        .orEmpty()
        .mapNotNull { it.toLongOrNull() }
        .toSet()

    private fun enqueuePendingRemoval(id: Long) = updatePendingSet(KEY_PENDING_REMOVAL_IDS, id.toString(), true)
    private fun clearPendingRemoval(id: Long) = updatePendingSet(KEY_PENDING_REMOVAL_IDS, id.toString(), false)
    private fun pendingTokenRemovals(): Set<String> = pendingRemovalPreferences()
        .getStringSet(KEY_PENDING_REMOVAL_TOKENS, emptySet()).orEmpty()
    private fun enqueuePendingTokenRemoval(token: String) = updatePendingSet(KEY_PENDING_REMOVAL_TOKENS, token, true)
    private fun clearPendingTokenRemoval(token: String) = updatePendingSet(KEY_PENDING_REMOVAL_TOKENS, token, false)

    private fun updatePendingSet(key: String, value: String, add: Boolean) {
        val values = pendingRemovalPreferences().getStringSet(key, emptySet()).orEmpty().toMutableSet()
        if (add) values.add(value) else values.remove(value)
        val committed = pendingRemovalPreferences().edit().putStringSet(key, values.toSet()).commit()
        check(committed) { "无法保存待清理的系统日历事项，请稍后重试" }
    }

    private fun findWritableCalendar(): Long? {
        val projection = arrayOf(CalendarContract.Calendars._ID)
        val sort = "${CalendarContract.Calendars.IS_PRIMARY} DESC, ${CalendarContract.Calendars._ID} ASC"
        return context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?",
            arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
            sort
        )?.use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null }
    }

    fun autoSyncEnabled(): Boolean = calendarPreferences().getBoolean(KEY_AUTO_SYNC, false)

    fun setAutoSync(enabled: Boolean) {
        calendarPreferences().edit().putBoolean(KEY_AUTO_SYNC, enabled).apply()
    }

    private suspend fun markNeedsConfirmation(
        item: TodoItemEntity,
        type: TodoCalendarIssueType
    ): TodoCalendarSyncIssue {
        repository.setCalendarSyncData(
            item.id,
            item.calendarEventId,
            item.calendarSyncToken,
            TodoCalendarSyncState.NEEDS_CONFIRMATION
        )
        return issue(item, type)
    }

    private fun issue(item: TodoItemEntity, type: TodoCalendarIssueType) = TodoCalendarSyncIssue(
        todoId = item.id,
        title = item.title,
        type = type,
        localEventId = item.calendarEventId ?: item.calendarSyncToken?.let(CalendarEventLocalIdStore::get)
    )

    private fun calendarPreferences() = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private fun pendingRemovalPreferences() = context.getSharedPreferences(PENDING_PREFERENCES, Context.MODE_PRIVATE)

    companion object {
        private const val PREFERENCES = "schedule_plus_calendar"
        private const val PENDING_PREFERENCES = "schedule_plus_calendar_pending"
        private const val KEY_AUTO_SYNC = "auto_sync"
        private const val KEY_PENDING_REMOVAL_IDS = "pending_event_removal_ids"
        private const val KEY_PENDING_REMOVAL_TOKENS = "pending_event_removal_tokens"
        private const val EVENT_DURATION_MILLIS = 30 * 60 * 1000L
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
        private const val TAG = "TodoCalendarSync"
        private val calendarMutationMutex = Mutex()
    }
}
