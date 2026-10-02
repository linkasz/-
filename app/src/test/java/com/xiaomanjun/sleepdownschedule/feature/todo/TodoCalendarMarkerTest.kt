package com.xiaomanjun.sleepdownschedule.feature.todo

import org.junit.Assert.assertEquals
import org.junit.Test

class TodoCalendarMarkerTest {
    @Test
    fun oneMarkerResolvesToItsEvent() {
        assertEquals(CalendarMarkerLookup.Unique(42L), classifyCalendarMarkerMatches(listOf(42L)))
    }

    @Test
    fun repeatedSameRowIsNotMistakenForMultipleEvents() {
        assertEquals(CalendarMarkerLookup.Unique(42L), classifyCalendarMarkerMatches(listOf(42L, 42L)))
    }

    @Test
    fun multipleMarkedEventsAreReportedAsAmbiguous() {
        assertEquals(CalendarMarkerLookup.Ambiguous(listOf(42L, 43L)), classifyCalendarMarkerMatches(listOf(42L, 43L)))
    }

    @Test
    fun noMarkedEventIsReportedAsMissing() {
        assertEquals(CalendarMarkerLookup.Missing, classifyCalendarMarkerMatches(emptyList()))
    }

    @Test
    fun missingMarkerChoosesTheSafestConfirmationForAvailableIdentity() {
        assertEquals(
            TodoCalendarIssueType.LEGACY_ARCHIVE,
            missingCalendarMarkerIssueType(hasStableToken = false, hasDeviceLocalEventId = false)
        )
        assertEquals(
            TodoCalendarIssueType.MISSING_EVENT,
            missingCalendarMarkerIssueType(hasStableToken = true, hasDeviceLocalEventId = false)
        )
        assertEquals(
            TodoCalendarIssueType.MARKER_NOT_PERSISTED,
            missingCalendarMarkerIssueType(hasStableToken = true, hasDeviceLocalEventId = true)
        )
    }
}
