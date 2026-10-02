package com.xiaomanjun.sleepdownschedule.feature.todo

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class TodoLifecyclePolicyTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val due = LocalDate.of(2026, 10, 1).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    @Test fun filterStatesAreUnambiguousAndArchivedTasksStayOut() {
        val plain = TodoItemEntity(1, "未分组")
        val grouped = plain.copy(id = 2, groupId = 8)
        assertTrue(TodoFilter().matches(plain)); assertTrue(TodoFilter().matches(grouped))
        assertTrue(TodoFilter(TodoFilterKind.UNGROUPED).matches(plain))
        assertFalse(TodoFilter(TodoFilterKind.UNGROUPED).matches(grouped))
        assertTrue(TodoFilter(TodoFilterKind.GROUP, 8).matches(grouped))
        assertFalse(TodoFilter().matches(grouped.copy(deletedAt = 1)))
        assertTrue(TodoFilter(TodoFilterKind.DELETED).matches(grouped.copy(deletedAt = 1)))
    }
    @Test fun restorationDoesNotRevivePreviouslyDeletedChildren() {
        val root = TodoItemEntity(1, "父", deletedAt = 10, deletionBatch = "new")
        val child = TodoItemEntity(2, "本次子", parentId = 1, deletedAt = 10, deletionBatch = "new")
        val earlier = child.copy(id = 3, title = "先前删除", deletedAt = 5, deletionBatch = "old")
        assertEquals(setOf(1L, 2L), todoRestoreIds(root, listOf(root, child, earlier)).toSet())
        assertEquals(setOf(1L, 2L, 3L), todoDescendantIds(1, listOf(root, child, earlier)).toSet())
    }
    @Test fun reminderModesAndArchivedStateAreRespected() {
        val task = TodoItemEntity(1, "提醒", dueAt = due)
        assertNull(todoReminderAt(task.copy(reminderMode = "NONE"), 10, zone))
        assertEquals(due - 600_000, todoReminderAt(task, 10, zone))
        assertEquals(due - 300_000, todoReminderAt(task.copy(reminderMode = "BEFORE", reminderOffsetMinutes = 5), 99, zone))
        assertNull(todoReminderAt(task.copy(deletedAt = 1), 10, zone))
        assertNull(todoReminderAt(task.copy(isCompleted = true), 10, zone))
        assertNull(todoReminderAt(task.copy(parentId = 2), 10, zone))
    }
    @Test fun allDayReminderUsesLocalDateAcrossDstBoundary() {
        val dst = ZoneId.of("America/New_York")
        val start = LocalDate.of(2026, 3, 9).atStartOfDay(dst).toInstant().toEpochMilli()
        val task = TodoItemEntity(1, "全天", dueAt = start, allDay = true, reminderMode = "ALL_DAY", reminderOffsetMinutes = 1440, reminderTimeMinutes = 480)
        assertEquals(LocalDate.of(2026, 3, 8).atTime(8, 0).atZone(dst).toInstant().toEpochMilli(), todoReminderAt(task, 0, dst))
    }
    @Test fun thirtyDaysIsAnExactBoundaryAndRepeatKeepsDuration() {
        assertEquals(2_592_000_000L, TODO_TRASH_RETENTION_MS)
        val task = TodoItemEntity(1, "重复", dueAt = due, endAt = due + 3_600_000, reminderMode = "BEFORE", reminderOffsetMinutes = 30)
        assertEquals(3_600_000L, nextOccurrence(task.endAt, "WEEKLY", zone)!! - nextOccurrence(task.dueAt, "WEEKLY", zone)!!)
        assertNotEquals(todoReminderSignature(task), todoReminderSignature(task.copy(reminderMode = "NONE")))
    }
}
