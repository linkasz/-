package com.xiaomanjun.sleepdownschedule.feature.todo

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
        Index("courseId")
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
    val calendarEventId: Long? = null
)

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
    val repeatRule: String = "NONE"
)

enum class TodoPage { Tasks, Calendar, Insights }

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
