package com.xiaomanjun.sleepdownschedule.feature.todo

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.xiaomanjun.sleepdownschedule.AppState
import com.xiaomanjun.sleepdownschedule.CourseEntity
import com.xiaomanjun.sleepdownschedule.WeekParity
import com.xiaomanjun.sleepdownschedule.defaultConfig
import com.xiaomanjun.sleepdownschedule.defaultPeriods
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoCalendarUnifiedAgendaTest {
    @Test
    fun calendarRefreshesAfterScheduleSwitchInsertEditAndDelete() {
        val date = LocalDate.of(2026, 9, 28)
        val course = CourseEntity(id = 71, name = "日程", teacher = "教师", location = "教室",
            weekday = 1, periods = listOf(1), weeks = listOf(2), weekParity = WeekParity.EVEN, note = "备注")
        val base = AppState(config = defaultConfig().copy(currentWeek = 1), periods = defaultPeriods(), loaded = true)
        val nextMonday = date.plusWeeks(1)
        fun entries(state: AppState) = buildTodoCalendarAgenda(state, emptyList(), nextMonday, date)
        assertTrue(entries(base).isEmpty())
        val inserted = base.copy(courses = listOf(course))
        assertEquals(course, (entries(inserted).single() as TodoCalendarAgendaEntry.CourseEntry).course)
        val edited = course.copy(name = "更新", periods = listOf(2), customColorArgb = 0xFF123456)
        val updated = entries(inserted.copy(courses = listOf(edited))).single() as TodoCalendarAgendaEntry.CourseEntry
        assertEquals(edited, updated.course)
        assertEquals(LocalTime.of(8, 55), updated.start)
        assertTrue(entries(inserted.copy(courses = emptyList())).isEmpty())
        assertTrue(entries(inserted.copy(config = base.config.copy(id = 2, currentWeek = 3))).isEmpty())
    }

    @Test
    fun calendarDistinguishesLoadingNoCoursesAndOutsideTerm() {
        val date = LocalDate.of(2026, 9, 28)
        assertEquals("课表加载中", todoCalendarCourseStatus(AppState(), date, date))
        assertEquals("当前课表还没有课程", todoCalendarCourseStatus(AppState(loaded = true), date, date))
        val state = AppState(loaded = true, config = defaultConfig().copy(autoCurrentWeek = true, termStartDate = "2026-10-01"))
        assertEquals("暂未开学", todoCalendarCourseStatus(state, date, date))
        assertEquals("学期已结束", todoCalendarCourseStatus(state, date.plusYears(1), date))
    }

    @Test
    fun agendaShowsCoursesForTheSelectedTeachingDateAndSortsAllDayBeforeTimedItems() {
        val date = LocalDate.of(2026, 9, 28)
        val schedule = AppState(
            courses = listOf(
                CourseEntity(
                    name = "高等数学",
                    teacher = null,
                    location = "A101",
                    weekday = 1,
                    periods = listOf(1),
                    weeks = listOf(1),
                    weekParity = WeekParity.ALL,
                    note = null
                )
            ),
            config = defaultConfig().copy(termStartDate = date.toString(), autoCurrentWeek = true),
            periods = defaultPeriods()
        )
        val zone = ZoneId.systemDefault()
        fun at(time: LocalTime) = date.atTime(time).atZone(zone).toInstant().toEpochMilli()
        val allDay = TodoItemEntity(id = 1, title = "全天任务", dueAt = at(LocalTime.MIDNIGHT), allDay = true)
        val earlyTask = TodoItemEntity(id = 2, title = "晨间任务", dueAt = at(LocalTime.of(7, 0)))

        val entries = buildTodoCalendarAgenda(schedule, listOf(earlyTask, allDay), date)

        assertTrue(entries.first() is TodoCalendarAgendaEntry.TaskEntry)
        assertEquals(1L, (entries.first() as TodoCalendarAgendaEntry.TaskEntry).item.id)
        assertEquals(3, entries.size)
        assertEquals(2L, (entries[1] as TodoCalendarAgendaEntry.TaskEntry).item.id)
        assertTrue(entries[2] is TodoCalendarAgendaEntry.CourseEntry)
        assertTrue(buildTodoCalendarAgenda(schedule, emptyList(), date.plusDays(1)).isEmpty())
    }

    @Test
    fun agendaReadsUpdatedCourseDetailsFromTheLatestScheduleState() {
        val date = LocalDate.of(2026, 9, 28)
        val original = CourseEntity(
            name = "旧课程名",
            teacher = "旧教师",
            location = "A101",
            weekday = 1,
            periods = listOf(1),
            weeks = listOf(1),
            weekParity = WeekParity.ALL,
            note = "旧备注"
        )
        val config = defaultConfig().copy(termStartDate = date.toString(), autoCurrentWeek = true)
        val base = AppState(courses = listOf(original), config = config, periods = defaultPeriods())
        val edited = original.copy(
            name = "新课程名",
            teacher = "新教师",
            location = "B203",
            note = "新备注",
            periods = listOf(2)
        )
        val updated = buildTodoCalendarAgenda(base.copy(courses = listOf(edited)), emptyList(), date)

        val course = (updated.single() as TodoCalendarAgendaEntry.CourseEntry).course
        assertEquals("新课程名", course.name)
        assertEquals("新教师", course.teacher)
        assertEquals("B203", course.location)
        assertEquals("新备注", course.note)
        assertEquals(2, course.periods.single())
    }

    @Test
    fun tappingTheOpenDateClosesAgendaAndTappingAnotherDateOpensIt() {
        val openDate = LocalDate.of(2026, 9, 28)

        assertFalse(shouldShowTodoCalendarAgendaAfterDateTap(true, openDate, openDate))
        assertTrue(shouldShowTodoCalendarAgendaAfterDateTap(false, openDate, openDate))
        assertTrue(shouldShowTodoCalendarAgendaAfterDateTap(true, openDate, openDate.plusDays(1)))
    }

    @Test
    fun tapCoordinatesResolveToTheDateCellUnderTheFinger() {
        val firstDate = LocalDate.of(2026, 9, 28)
        val secondDate = firstDate.plusDays(1)
        val cells = mapOf(
            firstDate to Rect(10f, 20f, 60f, 80f),
            secondDate to Rect(60f, 20f, 110f, 80f)
        )

        assertEquals(firstDate, todoCalendarDateAtPosition(cells, Offset(30f, 40f)))
        assertEquals(secondDate, todoCalendarDateAtPosition(cells, Offset(90f, 40f)))
        assertEquals(null, todoCalendarDateAtPosition(cells, Offset(120f, 40f)))
    }

    @Test
    fun groupColorsRemainStableWhenGroupsArriveInADifferentOrder() {
        val groups = listOf(
            TodoGroupEntity(id = 7, name = "学习"),
            TodoGroupEntity(id = 2, name = "生活"),
            TodoGroupEntity(id = 11, name = "工作")
        )
        val palette = listOf(0xFF3366CCL, 0xFF9955CCL, 0xFF22AA88L)

        val first = buildTodoGroupColorAssignments(groups, palette)
        val reordered = buildTodoGroupColorAssignments(groups.reversed(), palette)

        assertEquals(first, reordered)
        assertEquals(0xFF3366CCL, first[7L])
        assertEquals(0xFF9955CCL, first[2L])
    }

    @Test
    fun datePopupClampsToWindowAndChoosesTheSpaceWithMoreRoom() {
        val placement = todoCalendarPopupPlacement(
            anchor = Rect(920f, 1430f, 970f, 1490f),
            rootSize = IntSize(1080, 1600),
            desiredWidth = 900,
            preferredHeight = 520,
            margin = 24,
            minimumPopupHeight = 250
        )

        assertFalse(placement.bottomSheet)
        assertTrue(placement.left >= 24)
        assertTrue(placement.left + placement.width <= 1056)
        assertTrue(placement.top + placement.maxHeight <= 1576)
        assertTrue(placement.top + placement.maxHeight < 1430)
    }

    @Test
    fun datePopupFallsBackToBottomPanelWhenNeitherSideHasEnoughRoom() {
        val placement = todoCalendarPopupPlacement(
            anchor = Rect(480f, 220f, 540f, 280f),
            rootSize = IntSize(1080, 500),
            desiredWidth = 900,
            preferredHeight = 430,
            margin = 16,
            minimumPopupHeight = 250
        )

        assertTrue(placement.bottomSheet)
        assertTrue(placement.top > 0)
        assertTrue(placement.top + placement.maxHeight <= 484)
    }
}
