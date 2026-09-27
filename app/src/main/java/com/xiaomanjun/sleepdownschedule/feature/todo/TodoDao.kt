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
    @Query("SELECT * FROM todo_items ORDER BY isPinned DESC, CASE WHEN dueAt IS NULL THEN 1 ELSE 0 END, dueAt, createdAt DESC")
    fun observeItems(): Flow<List<TodoItemEntity>>

    @Query("SELECT * FROM todo_groups ORDER BY position, name")
    fun observeGroups(): Flow<List<TodoGroupEntity>>

    @Transaction
    @Query("SELECT * FROM todo_items WHERE parentId IS NULL ORDER BY isPinned DESC, CASE WHEN dueAt IS NULL THEN 1 ELSE 0 END, dueAt, createdAt DESC")
    fun observeRootItemsWithSubtasks(): Flow<List<TodoWithSubtasks>>

    @Query("SELECT * FROM todo_items WHERE dueAt >= :fromInclusive AND dueAt < :toExclusive ORDER BY dueAt, priority DESC")
    fun observeItemsBetween(fromInclusive: Long, toExclusive: Long): Flow<List<TodoItemEntity>>

    @Query("SELECT * FROM todo_groups ORDER BY position, name")
    suspend fun getGroups(): List<TodoGroupEntity>

    @Query("SELECT * FROM todo_items ORDER BY id")
    suspend fun getAllItems(): List<TodoItemEntity>

    @Query("SELECT * FROM todo_groups ORDER BY id")
    suspend fun getAllGroups(): List<TodoGroupEntity>

    @Query("SELECT * FROM todo_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TodoItemEntity?

    @Upsert
    suspend fun upsert(item: TodoItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGroup(group: TodoGroupEntity): Long

    @Upsert
    suspend fun upsertGroup(group: TodoGroupEntity): Long

    @Query("UPDATE todo_items SET isCompleted = :completed, completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean, completedAt: Long?, updatedAt: Long)

    @Query("UPDATE todo_items SET isPinned = :pinned, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean, updatedAt: Long)

    @Query("UPDATE todo_items SET calendarEventId = :eventId, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setCalendarEventId(id: Long, eventId: Long?, updatedAt: Long)

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
        val completed = !item.isCompleted
        setCompleted(item.id, completed, if (completed) now else null, now)
        if (!completed || item.repeatRule == "NONE") return null
        val nextDue = nextOccurrence(item.dueAt, item.repeatRule) ?: return null
        return item.copy(
            id = 0,
            dueAt = nextDue,
            isCompleted = false,
            createdAt = now,
            updatedAt = now,
            completedAt = null,
            calendarEventId = null
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
