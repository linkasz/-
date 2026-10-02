package com.kyant.backdrop.catalog.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DockDragMathTest {
    @Test
    fun slowAndFastDragsAccumulateFromLatestTarget() {
        val slowTarget = (0 until 40).fold(0f) { target, _ ->
            dockDragTarget(target, dragAmountPx = 10f, tabWidthPx = 100f, isLtr = true, tabsCount = 5)
        }
        val fastTarget = dockDragTarget(
            currentTarget = 0f,
            dragAmountPx = 400f,
            tabWidthPx = 100f,
            isLtr = true,
            tabsCount = 5
        )

        assertEquals(4f, slowTarget, 0.0001f)
        assertEquals(4f, fastTarget, 0.0001f)
    }

    @Test
    fun dragTargetMirrorsForRightToLeftAndClampsBothEdges() {
        assertEquals(
            2f,
            dockDragTarget(3f, 100f, 100f, isLtr = false, tabsCount = 5),
            0.0001f
        )
        assertEquals(4f, dockDragTarget(3.5f, 400f, 100f, isLtr = true, tabsCount = 5), 0.0001f)
        assertEquals(0f, dockDragTarget(0.5f, 400f, 100f, isLtr = false, tabsCount = 5), 0.0001f)
    }

    @Test
    fun dragMappingUsesCurrentWindowTabWidth() {
        assertEquals(1f, dockDragTarget(0f, 100f, 100f, isLtr = true, tabsCount = 5), 0.0001f)
        assertEquals(0.5f, dockDragTarget(0f, 100f, 200f, isLtr = true, tabsCount = 5), 0.0001f)
    }

    @Test
    fun zeroWidthKeepsCurrentTargetAndSettlingClampsToAvailableTabs() {
        assertEquals(2.25f, dockDragTarget(2.25f, 80f, 0f, isLtr = true, tabsCount = 5), 0.0001f)
        assertEquals(0, dockSettledIndex(-1f, tabsCount = 5))
        assertEquals(3, dockSettledIndex(2.5f, tabsCount = 5))
        assertEquals(4, dockSettledIndex(8f, tabsCount = 5))
    }

    @Test
    fun criticalSpringMovesTowardTargetWithoutOvershoot() {
        var state = DockSpringState(value = 0f, velocity = 0f)
        repeat(60) {
            state = dockSpringStep(state.value, state.velocity, target = 3f, deltaSeconds = 1f / 60f)
            assertTrue(state.value in 0f..3f)
        }
        assertEquals(3f, state.value, 0.01f)
        assertEquals(0f, state.velocity, 0.1f)
    }

    @Test
    fun springUsesOnlyTheNewestTargetAtEachFrame() {
        var state = DockSpringState(value = 0f, velocity = 0f)
        repeat(12) {
            state = dockSpringStep(state.value, state.velocity, target = 1f, deltaSeconds = 1f / 120f)
        }
        repeat(12) {
            state = dockSpringStep(state.value, state.velocity, target = 4f, deltaSeconds = 1f / 120f)
        }
        repeat(48) {
            state = dockSpringStep(state.value, state.velocity, target = 2f, deltaSeconds = 1f / 120f)
        }
        assertEquals(2f, state.value, 0.01f)
        assertEquals(0f, state.velocity, 0.1f)
    }

    @Test
    fun longFrameIsCappedAndInvalidDeltaDoesNotChangeSpringState() {
        val original = DockSpringState(value = 1f, velocity = 2f)
        assertEquals(original, dockSpringStep(1f, 2f, 4f, deltaSeconds = 0f))
        assertEquals(original, dockSpringStep(1f, 2f, 4f, deltaSeconds = Float.NaN))
        val capped = dockSpringStep(1f, 2f, 4f, deltaSeconds = 1f)
        val expected = dockSpringStep(1f, 2f, 4f, deltaSeconds = 0.032f)
        assertEquals(expected.value, capped.value, 0.0001f)
        assertEquals(expected.velocity, capped.velocity, 0.0001f)
    }
}
