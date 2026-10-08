package com.xiaomanjun.sleepdownschedule.domain.schedule

import com.xiaomanjun.sleepdownschedule.model.*
import com.xiaomanjun.sleepdownschedule.feature.home.day.dayCoursesForScheduleDate
import com.xiaomanjun.sleepdownschedule.feature.home.day.weekCourseBuckets
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class CalendarCourseProjectionTest {
    private val today = LocalDate.of(2026, 9, 30)
    private val monday = LocalDate.of(2026, 9, 28)
    private val course = CourseEntity(
        id = 17, name = "日历验证课程", teacher = "教师", location = "A101",
        weekday = 1, periods = listOf(1), weeks = listOf(2),
        weekParity = WeekParity.EVEN, note = "备注"
    )
    private val manual = defaultConfig().copy(autoCurrentWeek = false, currentWeek = 1, totalWeeks = 4)

    @Test fun manualCalendarFindsCoursesOutsideTheCurrentWeek() {
        val state = AppState(courses = listOf(course), config = manual)
        assertTrue(coursesForDate(state, monday, today).isEmpty())
        assertEquals(listOf(course), coursesForDate(state, monday.plusWeeks(1), today))
        assertTrue(coursesForDate(state, monday.plusWeeks(2), today).isEmpty())
        assertTrue(coursesForDate(state, monday.minusWeeks(1), today).isEmpty())
        assertTrue(coursesForDate(state, monday.plusWeeks(4), today).isEmpty())
    }

    @Test fun manualMonthMatchesEachWeekGridIncludingParity() {
        val courses = listOf(course, course.copy(id = 18, weekday = 3, weeks = (1..4).toList(), weekParity = WeekParity.ODD))
        val state = AppState(courses = courses, config = manual)
        for (week in 1..4) {
            val buckets = weekCourseBuckets(courses, week, manual, today)
            for (day in 1..7) {
                val date = monday.plusWeeks((week - 1).toLong()).plusDays((day - 1).toLong())
                assertEquals("week=$week day=$day", buckets.byWeekday[day].orEmpty().map { it.id },
                    coursesForDate(state, date, today).map { it.id })
            }
        }
    }

    @Test fun dayProjectionAdvancesFromManualWeekAndMatchesWeekGrid() {
        val courses = listOf(
            course.copy(weeks = listOf(1)),
            course.copy(id = 19, weeks = listOf(2)),
            course.copy(id = 20, weeks = listOf(2), weekParity = WeekParity.ODD)
        )
        val date = monday.plusWeeks(1)
        val periods = listOf(PeriodEntity(periodIndex = 1, startTime = "08:00", endTime = "08:50"))
        val week = adjustedTeachingWeekForDate(manual, date, today)

        assertEquals(2, week)
        assertEquals(
            weekCourseBuckets(courses, requireNotNull(week), manual, today).byWeekday[1].orEmpty().map { it.id },
            dayCoursesForScheduleDate(courses, periods, manual, date, today).map { it.id }
        )
        assertEquals(listOf(19L), dayCoursesForScheduleDate(courses, periods, manual, date, today).map { it.id })
    }

    @Test fun dayProjectionUsesWeekGridMakeupAndCancellationRules() {
        val source = monday.plusWeeks(1)
        val makeupDate = source.plusDays(5)
        val cancelledDate = source.plusDays(1)
        val config = manual.copy(scheduleAdjustmentsJson = encodeScheduleAdjustments(listOf(
            ScheduleAdjustment(source.toString()),
            ScheduleAdjustment(makeupDate.toString(), source.toString()),
            ScheduleAdjustment(cancelledDate.toString())
        )))
        val periods = listOf(PeriodEntity(periodIndex = 1, startTime = "08:00", endTime = "08:50"))

        assertEquals(
            listOf(course.copy(weekday = makeupDate.dayOfWeek.toChineseWeekday())),
            dayCoursesForScheduleDate(listOf(course), periods, config, makeupDate, today)
        )
        // Match the week page's existing presentation: cancelled occurrences remain visible as placeholders.
        val tuesdayCourse = course.copy(id = 21, weekday = 2)
        assertEquals(listOf(tuesdayCourse), dayCoursesForScheduleDate(
            listOf(tuesdayCourse), periods, config, cancelledDate, today
        ))
    }

    @Test fun manualMakeupUsesSourceWeekAndRestDayIsEmpty() {
        val source = monday.plusWeeks(1)
        val target = source.plusDays(5)
        val config = manual.copy(scheduleAdjustmentsJson = encodeScheduleAdjustments(listOf(
            ScheduleAdjustment(source.toString()), ScheduleAdjustment(target.toString(), source.toString())
        )))
        val state = AppState(courses = listOf(course), config = config)
        assertTrue(coursesForDate(state, source, today).isEmpty())
        assertEquals(listOf(course), coursesForDate(state, target, today))
    }

    @Test fun automaticDatesRespectOpeningDateLeapDayAndFinalWeek() {
        val leapDay = LocalDate.of(2028, 2, 29)
        val config = manual.copy(autoCurrentWeek = true, termStartDate = "2028-02-29", totalWeeks = 2)
        val first = course.copy(weekday = 2, weeks = listOf(1), weekParity = WeekParity.ODD)
        val second = course.copy(id = 18, weekday = 2)
        val state = AppState(courses = listOf(first, second), config = config)
        assertEquals(listOf(first), coursesForDate(state, leapDay, today))
        assertEquals(listOf(second), coursesForDate(state, leapDay.plusWeeks(1), today))
        assertTrue(coursesForDate(state, leapDay.minusDays(1), today).isEmpty())
        assertTrue(coursesForDate(state, leapDay.plusWeeks(2), today).isEmpty())
    }
}
