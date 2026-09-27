package com.xiaomanjun.sleepdownschedule.feature.todo

import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow

class TodoRepository @Inject constructor(private val dao: TodoDao) {
    val items: Flow<List<TodoItemEntity>> = dao.observeItems()
    val groups: Flow<List<TodoGroupEntity>> = dao.observeGroups()
    val rootsWithSubtasks: Flow<List<TodoWithSubtasks>> = dao.observeRootItemsWithSubtasks()

    suspend fun ensureDefaultGroup() {
        dao.insertGroup(TodoGroupEntity(name = "收件箱"))
    }

    suspend fun save(draft: TodoDraft): Long {
        val title = draft.title.trim()
        require(title.isNotEmpty()) { "待办标题不能为空" }
        val now = System.currentTimeMillis()
        val existing = if (draft.id > 0) dao.getById(draft.id) else null
        val entity = TodoItemEntity(
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
            isCompleted = existing?.isCompleted ?: false,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            completedAt = existing?.completedAt,
            calendarEventId = existing?.calendarEventId
        )
        val savedId = dao.upsert(entity)
        return savedId.takeIf { it > 0L } ?: entity.id
    }

    suspend fun toggle(item: TodoItemEntity): TodoItemEntity? = dao.toggleCompleted(item, System.currentTimeMillis())
    suspend fun pin(item: TodoItemEntity) = dao.setPinned(item.id, !item.isPinned, System.currentTimeMillis())
    suspend fun delete(item: TodoItemEntity) = dao.deleteItem(item.id)
    suspend fun saveGroup(name: String) {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "分组名称不能为空" }
        dao.upsertGroup(TodoGroupEntity(name = trimmed, position = dao.getGroups().size))
    }
    suspend fun deleteGroup(group: TodoGroupEntity) = dao.deleteGroup(group.id)
    suspend fun observeBetween(from: Long, to: Long): Flow<List<TodoItemEntity>> = dao.observeItemsBetween(from, to)
    suspend fun getById(id: Long): TodoItemEntity? = dao.getById(id)
    suspend fun setCalendarEventId(id: Long, eventId: Long?) =
        dao.setCalendarEventId(id, eventId, System.currentTimeMillis())
    suspend fun itemsForSync(): List<TodoItemEntity> = dao.observeItems().first()
}
