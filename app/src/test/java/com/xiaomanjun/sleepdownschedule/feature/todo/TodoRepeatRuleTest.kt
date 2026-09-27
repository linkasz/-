package com.xiaomanjun.sleepdownschedule.feature.todo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class TodoRepeatRuleTest {
    @Test
    fun dailyRepeatKeepsLocalTimeAcrossDaylightSavingChange() {
        val zone = ZoneId.of("America/New_York")
        val dueAt = ZonedDateTime.of(LocalDate.of(2025, 3, 8), LocalTime.of(9, 30), zone)
            .toInstant().toEpochMilli()

        val next = nextOccurrence(dueAt, "DAILY", zone)

        assertEquals(
            ZonedDateTime.of(LocalDate.of(2025, 3, 9), LocalTime.of(9, 30), zone).toInstant().toEpochMilli(),
            next
        )
    }

    @Test
    fun weeklyAndMonthlyRulesAdvanceTheirCalendarDate() {
        val zone = ZoneId.of("UTC")
        val weeklyDue = LocalDate.of(2026, 9, 21).atTime(14, 15).atZone(zone).toInstant().toEpochMilli()
        val monthlyDue = LocalDate.of(2024, 1, 31).atTime(8, 0).atZone(zone).toInstant().toEpochMilli()

        assertEquals(
            LocalDate.of(2026, 9, 28).atTime(14, 15).atZone(zone).toInstant().toEpochMilli(),
            nextOccurrence(weeklyDue, "WEEKLY", zone)
        )
        assertEquals(
            LocalDate.of(2024, 2, 29).atTime(8, 0).atZone(zone).toInstant().toEpochMilli(),
            nextOccurrence(monthlyDue, "MONTHLY", zone)
        )
    }

    @Test
    fun missingDueDateAndUnknownRuleDoNotCreateOccurrences() {
        assertNull(nextOccurrence(null, "DAILY", ZoneId.of("UTC")))
        assertNull(nextOccurrence(1L, "CUSTOM", ZoneId.of("UTC")))
    }
}
