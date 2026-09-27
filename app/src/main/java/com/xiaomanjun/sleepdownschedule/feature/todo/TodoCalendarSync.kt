package com.xiaomanjun.sleepdownschedule.feature.todo

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class TodoCalendarSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: TodoRepository
) {
    fun hasCalendarPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED

    suspend fun syncAll(): Int = withContext(Dispatchers.IO) {
        calendarMutationMutex.withLock {
            check(hasCalendarPermission()) { "需要读取和写入日历权限" }
            val calendarId = findWritableCalendar() ?: error("没有可写入的系统日历")
            var count = 0
            repository.itemsForSync().forEach { item ->
                if (item.isCompleted || item.dueAt == null || item.parentId != null) {
                    if (item.calendarEventId != null) removeLocked(item)
                    return@forEach
                }
                val due = requireNotNull(item.dueAt)
                val zone = ZoneId.systemDefault()
                val start = if (item.allDay) Instant.ofEpochMilli(due).atZone(zone).toLocalDate()
                    .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli() else due
                val end = if (item.allDay) start + DAY_MILLIS else due + EVENT_DURATION_MILLIS
                val values = ContentValues().apply {
                    put(CalendarContract.Events.CALENDAR_ID, calendarId)
                    put(CalendarContract.Events.TITLE, item.title)
                    put(CalendarContract.Events.DESCRIPTION, item.description)
                    put(CalendarContract.Events.DTSTART, start)
                    put(CalendarContract.Events.DTEND, end)
                    put(CalendarContract.Events.EVENT_TIMEZONE, if (item.allDay) "UTC" else zone.id)
                    put(CalendarContract.Events.ALL_DAY, if (item.allDay) 1 else 0)
                    put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
                }
                val existing = item.calendarEventId?.let { id ->
                    context.contentResolver.update(
                        CalendarContract.Events.CONTENT_URI,
                        values,
                        "${CalendarContract.Events._ID} = ?",
                        arrayOf(id.toString())
                    )
                } ?: 0
                if (existing == 0) {
                    val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                        ?: error("系统日历拒绝写入：${item.title}")
                    repository.setCalendarEventId(item.id, uri.lastPathSegment?.toLongOrNull())
                }
                count++
            }
            count
        }
    }

    suspend fun remove(item: TodoItemEntity) = withContext(Dispatchers.IO) {
        calendarMutationMutex.withLock { removeLocked(item) }
    }

    private suspend fun removeLocked(item: TodoItemEntity) {
        if (!hasCalendarPermission()) return
        item.calendarEventId?.let { id ->
            context.contentResolver.delete(
                CalendarContract.Events.CONTENT_URI,
                "${CalendarContract.Events._ID} = ?",
                arrayOf(id.toString())
            )
            repository.setCalendarEventId(item.id, null)
        }
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

    fun autoSyncEnabled(): Boolean = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        .getBoolean(KEY_AUTO_SYNC, false)

    fun setAutoSync(enabled: Boolean) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_AUTO_SYNC, enabled).apply()
    }

    companion object {
        private const val PREFERENCES = "schedule_plus_calendar"
        private const val KEY_AUTO_SYNC = "auto_sync"
        private const val EVENT_DURATION_MILLIS = 30 * 60 * 1000L
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
        private val calendarMutationMutex = Mutex()
    }
}
