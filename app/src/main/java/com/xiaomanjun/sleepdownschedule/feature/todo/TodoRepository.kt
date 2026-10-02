package com.xiaomanjun.sleepdownschedule.feature.todo

import com.xiaomanjun.sleepdownschedule.data.local.CalendarEventLocalIdStore
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow

class TodoRepository @Inject constructor(private val dao: TodoDao) {
    val items: Flow<List<TodoItemEntity>> = dao.observeItems()
    val groups: Flow<List<TodoGroupEntity>> = dao.observeGroups()
    val deletedItems: Flow<List<TodoItemEntity>> = dao.observeDeletedItems()
    val rootsWithSubtasks: Flow<List<TodoWithSubtasks>> = dao.observeRootItemsWithSubtasks()

    suspend fun ensureDefaultGroup() {
        // Ungrouped is a virtual filter, never a persisted default group.
    }

    suspend fun save(draft: TodoDraft): Long {
        val entity = entityForSave(draft)
        return dao.upsert(entity).takeIf { it > 0L } ?: entity.id
    }

    suspend fun saveWithSubtasks(draft: TodoDraft, subtaskTitles: List<String>): Long {
        val parent = entityForSave(draft)
        val subtasks = subtaskTitles.map { title ->
            entityForSave(TodoDraft(title = title, groupId = draft.groupId, courseId = draft.courseId))
        }
        return dao.upsertWithSubtasks(parent, subtasks)
    }

    private suspend fun entityForSave(draft: TodoDraft): TodoItemEntity {
        val title = draft.title.trim()
        require(title.isNotEmpty()) { "待办标题不能为空" }
        require(draft.endAt == null || (!draft.allDay && draft.dueAt != null && draft.endAt > draft.dueAt)) { "结束时间必须晚于开始时间" }
        require(draft.reminderMode in setOf("NONE", "LEGACY", "BEFORE", "ALL_DAY")) { "未知提醒方式" }
        require(draft.reminderOffsetMinutes >= 0 && draft.reminderTimeMinutes in 0..1439) { "提醒时间无效" }
        val now = System.currentTimeMillis()
        val existing = if (draft.id > 0) dao.getById(draft.id) else null
        require(draft.id == 0L || existing != null) { "该待办已不存在" }
        require(existing?.deletedAt == null) { "请先从最近删除恢复待办" }
        if (draft.endAt != null && draft.dueAt != null) {
            val zone = java.time.ZoneId.systemDefault()
            require(java.time.Instant.ofEpochMilli(draft.endAt).atZone(zone).toLocalDate() ==
                java.time.Instant.ofEpochMilli(draft.dueAt).atZone(zone).toLocalDate()) { "时间段必须在同一天内" }
        }
        return TodoItemEntity(
            id = draft.id,
            title = title,
            description = draft.description.trim(),
            dueAt = draft.dueAt,
            allDay = draft.allDay,
            priority = draft.priority.coerceIn(0, 3),
            isPinned = draft.isPinned,
            groupId = draft.groupId,
            parentId = draft.parentId,
            courseId = draft.courseId,
            repeatRule = draft.repeatRule,
            endAt = draft.endAt,
            reminderMode = draft.reminderMode,
            reminderOffsetMinutes = draft.reminderOffsetMinutes,
            reminderTimeMinutes = draft.reminderTimeMinutes,
            persistentReminder = draft.persistentReminder,
            strongReminder = draft.strongReminder,
            isCompleted = existing?.isCompleted ?: false,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            completedAt = existing?.completedAt,
            // Calendar provider row IDs are device-local and live only in the excluded local store.
            calendarEventId = null,
            calendarSyncToken = existing?.calendarSyncToken,
            calendarSyncState = existing?.calendarSyncState ?: TodoCalendarSyncState.PENDING
        )
    }

    suspend fun toggle(item: TodoItemEntity): TodoItemEntity? = dao.toggleCompleted(item, System.currentTimeMillis())
    suspend fun pin(item: TodoItemEntity) = dao.setPinned(item.id, !item.isPinned, System.currentTimeMillis())
    suspend fun delete(item: TodoItemEntity) {
        val active = dao.getActiveItems()
        val ids = todoDescendantIds(item.id, active)
        dao.archiveItems(ids, java.util.UUID.randomUUID().toString(), System.currentTimeMillis())
    }
    suspend fun restore(item: TodoItemEntity) {
        val all = dao.getAllItems()
        val current = all.firstOrNull { it.id == item.id && it.deletedAt != null } ?: return
        dao.restoreItems(todoRestoreIds(current, all), System.currentTimeMillis())
    }
    suspend fun purge(item: TodoItemEntity) {
        val all = dao.getAllItems()
        val descendants = todoDescendantIds(item.id, all).toSet()
        val ids = all.filter { it.id in descendants && it.deletedAt != null && it.deletionBatch == item.deletionBatch }.map { it.id }
        dao.purgeExpired(ids)
    }
    suspend fun expiredItems(now: Long): List<TodoItemEntity> = dao.getAllItems().filter { it.deletedAt != null && it.deletedAt <= now - TODO_TRASH_RETENTION_MS }
    suspend fun purgeExpired(ids: List<Long>) = dao.purgeExpired(ids)
    suspend fun saveGroup(name: String) {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "分组名称不能为空" }
        dao.upsertGroup(TodoGroupEntity(name = trimmed, position = dao.getGroups().size))
    }
    suspend fun deleteGroup(group: TodoGroupEntity) = dao.deleteGroup(group.id)
    suspend fun observeBetween(from: Long, to: Long): Flow<List<TodoItemEntity>> = dao.observeItemsBetween(from, to)
    suspend fun getById(id: Long): TodoItemEntity? = dao.getById(id)
    suspend fun setCalendarSyncData(
        id: Long,
        eventId: Long?,
        token: String?,
        state: String
    ) {
        if (eventId != null && token != null) CalendarEventLocalIdStore.put(token, eventId)
        dao.setCalendarSyncData(id, token, state, System.currentTimeMillis())
    }
    suspend fun itemsForSync(): List<TodoItemEntity> = dao.observeItems().first()
}

internal const val TODO_TRASH_RETENTION_MS = 30L * 24 * 60 * 60 * 1000

internal fun todoRestoreIds(item: TodoItemEntity, items: List<TodoItemEntity>): List<Long> {
    val ids = items.filter { it.deletedAt != null && item.deletionBatch != null && it.deletionBatch == item.deletionBatch }.map { it.id }.toMutableSet()
    ids.add(item.id)
    var parent = item.parentId
    while (parent != null) {
        val row = items.firstOrNull { it.id == parent } ?: break
        if (row.deletedAt != null) ids.add(row.id)
        parent = row.parentId
    }
    return ids.toList()
}

internal fun todoDescendantIds(root: Long, items: List<TodoItemEntity>): List<Long> {
    val ids = linkedSetOf(root)
    var added: Boolean
    do { added = false; items.forEach { if (it.parentId in ids && ids.add(it.id)) added = true } } while (added)
    return ids.toList()
}
