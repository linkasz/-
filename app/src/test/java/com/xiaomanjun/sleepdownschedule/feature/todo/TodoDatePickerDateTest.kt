package com.xiaomanjun.sleepdownschedule.feature.todo

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TodoDatePickerDateTest {
    @Test
    fun leapDayRoundTripsThroughThePickerValue() {
        val date = LocalDate.of(2024, 2, 29)

        assertEquals(date, todoDatePickerDate(requireNotNull(todoDatePickerMillis(date))))
    }

    @Test
    fun yearBoundaryRoundTripsWithoutShiftingTheSelectedDate() {
        listOf(LocalDate.of(2025, 12, 31), LocalDate.of(2026, 1, 1)).forEach { date ->
            assertEquals(date, todoDatePickerDate(requireNotNull(todoDatePickerMillis(date))))
        }
    }

    @Test
    fun emptyDateRemainsAnUnselectedPicker() {
        assertEquals(null, todoDatePickerMillis(null))
    }
}
