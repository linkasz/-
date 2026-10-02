package com.kyant.backdrop.catalog.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidToggleStateTest {
    @Test
    fun tapTogglesFromTheLatestSelectedTarget() {
        assertTrue(liquidToggleReleaseTarget(didDrag = false, dragFraction = 0f, currentTarget = false))
        assertFalse(liquidToggleReleaseTarget(didDrag = false, dragFraction = 1f, currentTarget = true))
    }

    @Test
    fun dragSettlesUsingTheLatestPointerFraction() {
        assertTrue(liquidToggleReleaseTarget(didDrag = true, dragFraction = 0.76f, currentTarget = false))
        assertFalse(liquidToggleReleaseTarget(didDrag = true, dragFraction = 0.24f, currentTarget = true))
    }
}
