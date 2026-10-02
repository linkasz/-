package com.xiaomanjun.sleepdownschedule.feature.agent

import com.xiaomanjun.sleepdownschedule.CourseEntity
import com.xiaomanjun.sleepdownschedule.WeekParity
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class HomeAssistantPolicyTest {
    @Test fun collapsedComposerReturnsToTheInitialCenteredRow() {
        // 56dp row + 16dp capsule inset + 20dp footer, with a 28dp dock gap.
        assertEquals(28f, assistantComposerTop(20f + 92f, 56f, 28f), 0f)
        assertEquals(assistantComposerTop(20f + 92f, 56f, 28f), 20f + 8f, 0f)
    }
    @Test fun outsideTapNeverClosesAnInsideOrConsumedDrag() {
        assertTrue(assistantOutsideTapAllowed(true, false, false))
        assertFalse(assistantOutsideTapAllowed(false, false, false))
        assertFalse(assistantOutsideTapAllowed(true, true, false))
        assertFalse(assistantOutsideTapAllowed(true, false, true))
        assertFalse(assistantOutsideTapAllowed(false, true, true))
    }

    @Test fun resizedDismissShrinksHeightAndWidthTogetherFromTheCurrentFrame() {
        val anchor = androidx.compose.ui.geometry.Rect(250f, 0f, 330f, 24f)
        val current = androidx.compose.ui.geometry.Rect(12f, 20f, 568f, 460f)
        assertEquals(current, assistantClosingFrame(anchor, current, .8f, .9f, .8f, .9f))
        val halfway = assistantClosingFrame(anchor, current, .4f, .45f, .8f, .9f)
        assertEquals((anchor.height + current.height) / 2, halfway.height, .001f)
        assertEquals((anchor.width + current.width) / 2, halfway.width, .001f)
        assertEquals(anchor, assistantClosingFrame(anchor, current, 0f, 0f, .8f, .9f))
    }

    @Test fun continuousHeightRetainsInteriorAndResistsBothEdges() {
        assertEquals(345f, resistedAssistantHeight(345f, 220f, 800f), 0f)
        assertEquals(805f, resistedAssistantHeight(833.3333f, 220f, 800f), .01f)
        assertEquals(205f, resistedAssistantHeight(120f, 220f, 800f), 0f)
        assertEquals(824f, resistedAssistantHeight(2000f, 220f, 800f), 0f)
        assertEquals(196f, resistedAssistantHeight(-1000f, 220f, 800f), 0f)
        assertEquals(80f, constrainedAssistantHeight(120f, 220f, 80f), 0f)
    }
    @Test fun handleSwipeOnlyCommitsTheRequestedDirectionAndDistance() {
        assertTrue(assistantHandleSwipeCommits(24f, false, 24f))
        assertTrue(assistantHandleSwipeCommits(-24f, true, 24f))
        assertFalse(assistantHandleSwipeCommits(-40f, false, 24f))
        assertFalse(assistantHandleSwipeCommits(40f, true, 24f))
        assertFalse(assistantHandleSwipeCommits(23f, false, 24f))
        assertFalse(assistantHandleSwipeCommits(-23f, true, 24f))
    }
    private val day = LocalDate.of(2026, 9, 16)

    private fun slot(id: Long = 1, hour: Int = 8, minute: Int = 0, date: LocalDate = day) = AgentCourseSlot(
        CourseEntity(id, "课程", null, "教学楼", date.dayOfWeek.value, listOf(1, 2), listOf(1), WeekParity.ALL, null, scheduleId = 1),
        date, LocalTime.of(hour, minute), LocalTime.of(hour, minute).plusMinutes(90)
    )

    @Test fun emitsOnlyThreeBoundariesForAMultiPeriodCourse() {
        val events = assistantReminders(listOf(slot()))
        assertEquals(listOf(day.atTime(7, 30), day.atTime(7, 45), day.atTime(8, 0)), events.map { it.at })
        assertEquals(listOf(30, 15, 0), events.map { it.minutesBefore })
    }

    @Test fun crossingABoundaryLateStillShowsItOnce() {
        val events = assistantReminders(listOf(slot()))
        assertTrue(dueAssistantReminders(events, day.atTime(7, 29, 59), emptySet()).isEmpty())
        val due = dueAssistantReminders(events, day.atTime(7, 30, 18), emptySet())
        assertEquals(30, due.single().minutesBefore)
        assertTrue(dueAssistantReminders(events, day.atTime(7, 30, 40), setOf(due.single().key)).isEmpty())
        assertEquals(15, dueAssistantReminders(events, day.atTime(7, 45), setOf(due.single().key)).single().minutesBefore)
    }

    @Test fun returningFromBackgroundDoesNotReplayExpiredStages() {
        val events = assistantReminders(listOf(slot()))
        assertEquals(1, dueAssistantReminders(events, day.atTime(7, 30, 59), emptySet()).size)
        assertTrue(dueAssistantReminders(events, day.atTime(7, 31), emptySet()).isEmpty())
        assertEquals(listOf(0), dueAssistantReminders(events, day.atTime(8, 0, 20), emptySet()).map { it.minutesBefore })
        assertTrue(dueAssistantReminders(events, day.atTime(8, 12), emptySet()).isEmpty())
    }

    @Test fun midnightLeadTimesUseTheOccurrenceDate() {
        val tomorrow = day.plusDays(1)
        val events = assistantReminders(listOf(slot(hour = 0, minute = 10, date = tomorrow)))
        assertEquals(day.atTime(23, 40), events.first().at)
        assertEquals(15, dueAssistantReminders(events, day.atTime(23, 55), emptySet()).single().minutesBefore)
        assertEquals(tomorrow, dueAssistantReminders(events, tomorrow.atTime(0, 10), emptySet()).single().slot.date)
    }

    @Test fun startsTakePriorityOverUpcomingCoursesAtTheSameInstant() {
        val events = assistantReminders(listOf(slot(id = 2, hour = 8, minute = 30), slot(id = 1)))
        val due = dueAssistantReminders(events, day.atTime(8, 0), emptySet())
        assertEquals(listOf(0, 30), due.map { it.minutesBefore })
        assertTrue(dueAssistantReminders(events, day.atTime(8, 0, 20), due.map { it.key }.toSet()).isEmpty())
    }

    @Test fun editsInvalidateTheOldTimeButTextEditsDoNotDuplicateAReminder() {
        val original = slot()
        val seen = assistantReminders(listOf(original)).map { it.key }.toSet()
        val renamed = original.copy(course = original.course.copy(name = "新课程名"))
        assertTrue(dueAssistantReminders(assistantReminders(listOf(renamed)), day.atTime(7, 45), seen).isEmpty())
        val moved = original.copy(start = LocalTime.of(9, 0), end = LocalTime.of(10, 30))
        assertEquals(30, dueAssistantReminders(assistantReminders(listOf(moved)), day.atTime(8, 30), seen).single().minutesBefore)
    }

    @Test fun occurrencesAndSchedulesKeepIndependentIdentity() {
        val first = slot()
        val other = first.copy(course = first.course.copy(scheduleId = 2))
        val repeated = first.copy(date = day.plusDays(7))
        assertEquals(9, assistantReminders(listOf(first, first, other, repeated)).size)
        assertTrue(assistantReminders(emptyList()).isEmpty())
    }

    @Test fun resistanceIncreasesAndDistanceRemainsBounded() {
        val positions = (0..5).map { assistantPullDistance(it * 100f, 150f) }
        val increments = positions.zipWithNext { a, b -> b - a }
        assertTrue(increments.all { it > 0f })
        assertTrue(increments.zipWithNext { a, b -> a > b }.all { it })
        assertTrue(assistantPullDistance(100_000f, 150f) < 150f)
        assertEquals(0f, assistantPullDistance(-30f, 150f), 0f)
    }
}
