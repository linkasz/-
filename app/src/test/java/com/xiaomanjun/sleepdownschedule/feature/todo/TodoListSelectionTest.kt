package com.xiaomanjun.sleepdownschedule.feature.todo

import org.junit.Assert.*
import org.junit.Test

class TodoListSelectionTest {
    private val state = TodoUiState(items = listOf(TodoItemEntity(1, "未分组"),
        TodoItemEntity(2, "分组", groupId = 8, courseId = 5), TodoItemEntity(3, "子任务", parentId = 2),
        TodoItemEntity(4, "完成", isCompleted = true)), deletedItems = listOf(
        TodoItemEntity(5, "删除的主任务", deletedAt = 1), TodoItemEntity(6, "同批子任务", parentId = 5, deletedAt = 1),
        TodoItemEntity(7, "独立删除子任务", parentId = 1, deletedAt = 2)), loading = false)
    @Test fun allUngroupedAndDeletedAreVisibleFromTheSameSourcesAndCounts() {
        assertEquals(listOf(1L, 2L), todoListSelection(state, TodoFilter(), false, null).map { it.id })
        assertEquals(listOf(1L), todoListSelection(state, TodoFilter(TodoFilterKind.UNGROUPED), false, null).map { it.id })
        assertEquals(listOf(5L, 7L), todoListSelection(state, TodoFilter(TodoFilterKind.DELETED), false, null).map { it.id })
        assertEquals(listOf(2L), todoListSelection(state, TodoFilter(TodoFilterKind.GROUP, 8), false, null).map { it.id })
    }
    @Test fun completedAndCourseFiltersDoNotLeakIntoAGroupSelectionWithoutACourseFilter() {
        assertEquals(listOf(2L), todoListSelection(state, TodoFilter(), true, 5).map { it.id })
        assertEquals(listOf(1L, 2L, 4L), todoListSelection(state, TodoFilter(), true, null).map { it.id })
        assertEquals(listOf(5L, 7L), todoListSelection(state, TodoFilter(TodoFilterKind.DELETED), false, null).map { it.id })
    }
}
