package com.xiaomanjun.sleepdownschedule.feature.todo

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todo_items WHERE deletedAt IS NULL ORDER BY isPinned DESC, CASE WHEN dueAt IS NULL THEN 1 ELSE 0 END, dueAt, createdAt DESC")
    fun observeItems(): Flow<List<TodoItemEntity>>

    @Query("SELECT * FROM todo_groups ORDER BY position, name")
    fun observeGroups(): Flow<List<TodoGroupEntity>>

    @Transaction
    @Query("SELECT * FROM todo_items WHERE parentId IS NULL AND deletedAt IS NULL ORDER BY isPinned DESC, CASE WHEN dueAt IS NULL THEN 1 ELSE 0 END, dueAt, createdAt DESC")
    fun observeRootItemsWithSubtasks(): Flow<List<TodoWithSubtasks>>

    @Query("SELECT * FROM todo_items WHERE deletedAt IS NULL AND dueAt >= :fromInclusive AND dueAt < :toExclusive ORDER BY dueAt, priority DESC")
    fun observeItemsBetween(fromInclusive: Long, toExclusive: Long): Flow<List<TodoItemEntity>>

    @Query("SELECT * FROM todo_groups ORDER BY position, name")
    suspend fun getGroups(): List<TodoGroupEntity>

    @Query("SELECT * FROM todo_items ORDER BY id")
    suspend fun getAllItems(): List<TodoItemEntity>

    @Query("SELECT * FROM todo_items WHERE deletedAt IS NULL ORDER BY id")
    suspend fun getActiveItems(): List<TodoItemEntity>

    @Query("SELECT * FROM todo_items WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeDeletedItems(): Flow<List<TodoItemEntity>>

    @Query("UPDATE todo_items SET deletedAt = :now, deletionBatch = :batch, updatedAt = :now WHERE id IN (:ids) AND deletedAt IS NULL")
    suspend fun archiveItems(ids: List<Long>, batch: String, now: Long)

    @Query("UPDATE todo_items SET deletedAt = NULL, deletionBatch = NULL, calendarEventId = NULL, calendarSyncState = 'PENDING', updatedAt = :now WHERE id IN (:ids)")
    suspend fun restoreItems(ids: List<Long>, now: Long)

    @Query("DELETE FROM todo_items WHERE id IN (:ids) AND deletedAt IS NOT NULL")
    suspend fun purgeItems(ids: List<Long>)

    @Query("UPDATE todo_items SET parentId = NULL WHERE parentId IN (:ids) AND id NOT IN (:ids)")
    suspend fun detachSurvivingChildren(ids: List<Long>)

    @Transaction
    suspend fun purgeExpired(ids: List<Long>) {
        if (ids.isEmpty()) return
        detachSurvivingChildren(ids)
        purgeItems(ids)
    }

    @Query("SELECT * FROM todo_groups ORDER BY id")
    suspend fun getAllGroups(): List<TodoGroupEntity>

    @Query("SELECT * FROM todo_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TodoItemEntity?

    @Upsert
    suspend fun upsert(item: TodoItemEntity): Long

    @Transaction
    suspend fun upsertWithSubtasks(parent: TodoItemEntity, subtasks: List<TodoItemEntity>): Long {
        if (parent.id > 0L) {
            val current = getById(parent.id)
            require(current != null) { "该待办已不存在" }
            require(current.deletedAt == null) { "请先从最近删除恢复待办" }
        }
        val parentId = upsert(parent).takeIf { it > 0L } ?: parent.id
        check(parentId > 0L) { "待办保存未返回有效 ID" }
        subtasks.forEach { upsert(it.copy(parentId = parentId)) }
        return parentId
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGroup(group: TodoGroupEntity): Long

    @Upsert
    suspend fun upsertGroup(group: TodoGroupEntity): Long

    @Query("UPDATE todo_items SET isCompleted = :completed, completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :id AND deletedAt IS NULL")
    suspend fun setCompleted(id: Long, completed: Boolean, completedAt: Long?, updatedAt: Long)

    @Query("UPDATE todo_items SET isPinned = :pinned, updatedAt = :updatedAt WHERE id = :id AND deletedAt IS NULL")
    suspend fun setPinned(id: Long, pinned: Boolean, updatedAt: Long)

    @Query("UPDATE todo_items SET calendarEventId = NULL, calendarSyncToken = :token, calendarSyncState = :state, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setCalendarSyncData(id: Long, token: String?, state: String, updatedAt: Long)

    @Query("DELETE FROM todo_items WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Query("DELETE FROM todo_groups WHERE id = :id")
    suspend fun deleteGroup(id: Long)

    @Query("DELETE FROM todo_items")
    suspend fun deleteAllItems()

    @Query("DELETE FROM todo_groups")
    suspend fun deleteAllGroups()

    @Transaction
    suspend fun toggleCompleted(item: TodoItemEntity, now: Long): TodoItemEntity? {
        val current = getById(item.id) ?: return null
        if (current.deletedAt != null) return null
        val completed = !current.isCompleted
        setCompleted(current.id, completed, if (completed) now else null, now)
        if (!completed || current.repeatRule == "NONE") return null
        val nextDue = nextOccurrence(current.dueAt, current.repeatRule) ?: return null
        return current.copy(
            id = 0,
            dueAt = nextDue,
            endAt = nextOccurrence(current.endAt, current.repeatRule),
            isCompleted = false,
            createdAt = now,
            updatedAt = now,
            completedAt = null,
            calendarEventId = null,
            calendarSyncToken = null,
            calendarSyncState = TodoCalendarSyncState.PENDING
        ).also { upsert(it) }
    }
}

internal fun nextOccurrence(
    dueAt: Long?,
    rule: String,
    zone: java.time.ZoneId = java.time.ZoneId.systemDefault()
): Long? {
    dueAt ?: return null
    val dateTime = java.time.Instant.ofEpochMilli(dueAt).atZone(zone).toLocalDateTime()
    val next = when (rule) {
        "DAILY" -> dateTime.plusDays(1)
        "WEEKLY" -> dateTime.plusWeeks(1)
        "MONTHLY" -> dateTime.plusMonths(1)
        else -> return null
    }
    return next.atZone(zone).toInstant().toEpochMilli()
}
