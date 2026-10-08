package com.xiaomanjun.sleepdownschedule.app.ui

import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Test

class DockAccentColorTest {
    @Test
    fun wallpaperLightGlassUsesTheSharedCourseDockBlue() {
        assertEquals(0xFF006FD6.toInt(), dockSelectedAccentColor(lightGlass = true).toArgb())
    }

    @Test
    fun wallpaperDarkGlassUsesTheSharedBrightDockBlue() {
        assertEquals(0xFF0091FF.toInt(), dockSelectedAccentColor(lightGlass = false).toArgb())
    }
}
