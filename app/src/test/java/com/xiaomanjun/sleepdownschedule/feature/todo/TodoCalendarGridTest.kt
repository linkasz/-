package com.xiaomanjun.sleepdownschedule.feature.todo

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoCalendarGridTest {
    @Test
    fun mondayFirstGridUsesOnlyTheWeeksRequiredByTheMonth() {
        val fourWeeks = todoCalendarGridDates(YearMonth.of(2021, 2))
        val fiveWeeks = todoCalendarGridDates(YearMonth.of(2026, 9))
        val sixWeeks = todoCalendarGridDates(YearMonth.of(2026, 8))

        assertEquals(28, fourWeeks.size)
        assertEquals(35, fiveWeeks.size)
        assertEquals(42, sixWeeks.size)
    }

    @Test
    fun gridDatesAreContinuousAndContainEachVisibleMonthDateOnce() {
        val month = YearMonth.of(2026, 9)
        val dates = todoCalendarGridDates(month)

        assertEquals(LocalDate.of(2026, 8, 31), dates.first())
        assertEquals(1, dates.first().dayOfWeek.value)
        assertTrue(dates.zipWithNext().all { (first, next) -> first.plusDays(1) == next })
        assertEquals(month.lengthOfMonth(), dates.count { YearMonth.from(it) == month })
    }

    @Test
    fun leapFebruaryStillUsesACompleteMondayFirstMonthGrid() {
        val leapFebruary = YearMonth.of(2024, 2)
        val dates = todoCalendarGridDates(leapFebruary)

        assertEquals(35, dates.size)
        assertEquals(29, dates.count { YearMonth.from(it) == leapFebruary })
        assertEquals(1, dates.first().dayOfWeek.value)
        assertEquals(7, dates.last().dayOfWeek.value)
    }

    @Test
    fun horizontalSwipeChangesMonthOnlyAfterThresholdAndKeepsDirectionConsistent() {
        val month = YearMonth.of(2026, 9)
        assertEquals(YearMonth.of(2026, 10), todoCalendarMonthSwipeTarget(month, -80f, 64f))
        assertEquals(YearMonth.of(2026, 8), todoCalendarMonthSwipeTarget(month, 80f, 64f))
        assertEquals(null, todoCalendarMonthSwipeTarget(month, -40f, 64f))
    }

    @Test
    fun monthNavigationPreservesSelectedDayAndClampsAtMonthEnd() {
        assertEquals(
            LocalDate.of(2026, 9, 30),
            todoCalendarDateForMonth(YearMonth.of(2026, 9), LocalDate.of(2026, 8, 30))
        )
        assertEquals(
            LocalDate.of(2024, 2, 29),
            todoCalendarDateForMonth(YearMonth.of(2024, 2), LocalDate.of(2024, 1, 31))
        )
        assertEquals(
            LocalDate.of(2025, 2, 28),
            todoCalendarDateForMonth(YearMonth.of(2025, 2), LocalDate.of(2025, 1, 31))
        )
    }
}
