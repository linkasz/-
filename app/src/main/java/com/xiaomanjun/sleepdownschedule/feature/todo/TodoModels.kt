package com.xiaomanjun.sleepdownschedule.feature.todo

import android.net.Uri

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Locally stored lists that users can use to organize work. */
@Entity(
    tableName = "todo_groups",
    indices = [Index(value = ["name"], unique = true)]
)
data class TodoGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(defaultValue = "0") val colorArgb: Long = 0L,
    @ColumnInfo(defaultValue = "0") val position: Int = 0
)

/**
 * One task or subtask. Course and group links are nullable so removing either parent keeps the
 * user's task while clearing only the stale association.
 */
@Entity(
    tableName = "todo_items",
    foreignKeys = [
        ForeignKey(
            entity = TodoGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = TodoItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = com.xiaomanjun.sleepdownschedule.model.CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("groupId"),
        Index("parentId"),
        Index("dueAt"),
        Index("courseId"),
        Index(value = ["calendarSyncToken"], unique = true)
    ]
)
data class TodoItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    @ColumnInfo(defaultValue = "") val description: String = "",
    val dueAt: Long? = null,
    @ColumnInfo(defaultValue = "0") val allDay: Boolean = false,
    @ColumnInfo(defaultValue = "1") val priority: Int = 1,
    @ColumnInfo(defaultValue = "0") val isCompleted: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isPinned: Boolean = false,
    val groupId: Long? = null,
    val parentId: Long? = null,
    val courseId: Long? = null,
    @ColumnInfo(defaultValue = "'NONE'") val repeatRule: String = "NONE",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val calendarEventId: Long? = null,
    val calendarSyncToken: String? = null,
    @ColumnInfo(defaultValue = "'PENDING'") val calendarSyncState: String = TodoCalendarSyncState.PENDING,
    val endAt: Long? = null,
    @ColumnInfo(defaultValue = "'LEGACY'") val reminderMode: String = "LEGACY",
    @ColumnInfo(defaultValue = "0") val reminderOffsetMinutes: Int = 0,
    @ColumnInfo(defaultValue = "480") val reminderTimeMinutes: Int = 480,
    @ColumnInfo(defaultValue = "0") val persistentReminder: Boolean = false,
    @ColumnInfo(defaultValue = "0") val strongReminder: Boolean = false,
    val deletedAt: Long? = null,
    val deletionBatch: String? = null
)

object TodoCalendarSyncState {
    const val PENDING = "PENDING"
    const val CREATING = "CREATING"
    const val LINKED = "LINKED"
    const val NEEDS_CONFIRMATION = "NEEDS_CONFIRMATION"
    const val SKIPPED = "SKIPPED"
    const val LOCAL_ONLY = "LOCAL_ONLY"

    val all = setOf(PENDING, CREATING, LINKED, NEEDS_CONFIRMATION, SKIPPED, LOCAL_ONLY)
}

data class TodoWithSubtasks(
    @androidx.room.Embedded val task: TodoItemEntity,
    @androidx.room.Relation(parentColumn = "id", entityColumn = "parentId")
    val subtasks: List<TodoItemEntity>
)

data class TodoDraft(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val dueAt: Long? = null,
    val allDay: Boolean = false,
    val priority: Int = 1,
    val isPinned: Boolean = false,
    val groupId: Long? = null,
    val parentId: Long? = null,
    val courseId: Long? = null,
    val repeatRule: String = "NONE",
    val endAt: Long? = null,
    val reminderMode: String = "NONE",
    val reminderOffsetMinutes: Int = 0,
    val reminderTimeMinutes: Int = 480,
    val persistentReminder: Boolean = false,
    val strongReminder: Boolean = false
)

enum class TodoFilterKind { ALL, UNGROUPED, GROUP, DELETED }

data class TodoFilter(val kind: TodoFilterKind = TodoFilterKind.ALL, val groupId: Long? = null) {
    fun matches(item: TodoItemEntity): Boolean = when (kind) {
        TodoFilterKind.ALL -> item.deletedAt == null
        TodoFilterKind.UNGROUPED -> item.deletedAt == null && item.groupId == null
        TodoFilterKind.GROUP -> item.deletedAt == null && item.groupId == groupId
        TodoFilterKind.DELETED -> item.deletedAt != null
    }
}

enum class TodoPage { Tasks, Calendar, Insights }

/** One-shot data delivered by shares, notifications, widgets or in-app course actions. */
data class TodoEntryRequest(
    val id: Long,
    val destination: TodoPage = TodoPage.Tasks,
    val todoId: Long? = null,
    val courseId: Long? = null,
    val startWithNewTask: Boolean = false,
    val sharedText: String = "",
    val imageUris: List<Uri> = emptyList(),
    val screenshotPath: String? = null,
    val requestShizukuPermission: Boolean = false,
    val returnToCourse: Boolean = false
)

fun repeatLabel(value: String): String = when (value) {
    "DAILY" -> "每天"
    "WEEKLY" -> "每周"
    "MONTHLY" -> "每月"
    else -> "不重复"
}

fun priorityLabel(value: Int): String = when (value.coerceIn(0, 3)) {
    0 -> "低"
    1 -> "普通"
    2 -> "高"
    else -> "紧急"
}
