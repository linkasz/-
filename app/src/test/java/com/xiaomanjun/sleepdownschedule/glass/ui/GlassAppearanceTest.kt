package com.xiaomanjun.sleepdownschedule.glass.ui

import org.junit.Assert.*
import org.junit.Test

class GlassAppearanceTest {
    @Test fun brightnessDeadBandKeepsPolarityUntilBoundaryIsCrossed() {
        assertTrue(resolveGlassAppearance(listOf(.50f), true).light)
        assertFalse(resolveGlassAppearance(listOf(.50f), false).light)
        assertTrue(resolveGlassAppearance(listOf(.57f), false).light)
        assertFalse(resolveGlassAppearance(listOf(.45f), true).light)
    }
    @Test fun invalidSamplesPreserveHostPolarityRatherThanInventingWallpaper() {
        assertTrue(resolveGlassAppearance(listOf(Float.NaN, Float.POSITIVE_INFINITY), true).light)
        assertFalse(resolveGlassAppearance(emptyList(), false).light)
    }
    @Test fun ComplexBackgroundIncreasesReadingBudgetWithinItsLimit() {
        val flat = resolveGlassAppearance(List(256) { .75f }, true)
        val detail = resolveGlassAppearance(List(256) { if (it % 2 == 0) 0f else 1f }, true)
        assertEquals(8f, flat.readingBlur, .001f)
        assertEquals(12f, detail.readingBlur, .001f)
        assertTrue(detail.readingAlpha > flat.readingAlpha)
        assertTrue(detail.controlAlpha < detail.readingAlpha)
    }
    @Test fun SmallControlsAndSpringOvershootNeverExceedTenPercentExpansion() {
        for (height in listOf(8f, 34f, 42f, 48f, 80f)) {
            assertTrue(glassControlPressScale(height, 1.25f) <= 1.10001f)
            assertEquals(1f, glassControlPressScale(height, -1f), .00001f)
        }
        assertEquals(1f+4f/48f, glassControlPressScale(48f, 1f), .00001f)
    }
    @Test fun ReducedMotionAndInvalidGeometryHaveNoVisualExpansion() {
        assertEquals(1f, glassControlPressScale(48f, 1f, false), .00001f)
        assertEquals(1f, glassControlPressScale(0f, 1f), .00001f)
        assertEquals(1f, glassControlPressScale(48f, Float.NaN), .00001f)
    }
}
