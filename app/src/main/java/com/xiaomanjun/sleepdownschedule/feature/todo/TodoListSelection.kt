package com.xiaomanjun.sleepdownschedule.feature.todo

/** Counts and both list hosts share the same source and root-task filtering. */
internal fun todoListSelection(state: TodoUiState, filter: TodoFilter, showCompleted: Boolean, courseId: Long?): List<TodoItemEntity> {
    val source = if (filter.kind == TodoFilterKind.DELETED) state.deletedItems else state.items
    val ids = source.mapTo(hashSetOf()) { it.id }
    return source.filter { item ->
        (item.parentId == null || filter.kind == TodoFilterKind.DELETED && item.parentId !in ids) &&
            filter.matches(item) &&
            (filter.kind == TodoFilterKind.DELETED || showCompleted || !item.isCompleted) &&
            (courseId == null || item.courseId == courseId)
    }
}
