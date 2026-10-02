package com.xiaomanjun.sleepdownschedule.glass.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.xiaomanjun.sleepdownschedule.feature.home.day.LocalHomeReadability
import com.xiaomanjun.sleepdownschedule.feature.home.day.sampleVisibleWallpaperColors
import com.xiaomanjun.sleepdownschedule.model.ScheduleConfigEntity

/** One polarity for the shell, editor and voice controls; independent of the settings theme. */
@Immutable
internal data class AssistantGlassPalette(val light: Boolean) {
    val base: Color get() = if (light) Color.White else Color.Black
    val foreground: Color get() = if (light) Color(0xFF17263B) else Color.White
    val accent: Color get() = if (light) Color(0xFF087EF1) else Color(0xFF67BEFF)
    val controlTokens: GlassTokens get() = GlassTokens.pill().copy(
        blur = 2.dp, lensHeight = 12.dp, lensAmount = 24.dp,
        surfaceAlpha = if (light) .42f else .46f,
        highlightAlpha = .16f, shadowAlpha = .08f, innerShadowAlpha = .08f
    )
    // Thin clear controls share the Siri-style composer's refraction and readable adaptive polarity.
    val composerTokens: GlassTokens get() = controlTokens.copy(
        surfaceAlpha = if (light) .12f else .18f,
        highlightAlpha = .22f, innerShadowAlpha = .04f
    )
}

@Composable
internal fun rememberAssistantGlassPalette(config: ScheduleConfigEntity): AssistantGlassPalette {
    val wallpaper = LocalHomeReadability.current
    val fallback = LocalAdaptiveGlass.current
    val hasWallpaper = !config.wallpaperUri.isNullOrBlank()
    val themeLight = !appUsesDarkTheme(config)
    // Reuse the crop-aware CPU thumbnail. Never read pixels back from the rendered glass,
    // which would include its own tint and create a color feedback loop.
    val luminance = remember(wallpaper, hasWallpaper) {
        if (!hasWallpaper) null else sampleVisibleWallpaperColors(wallpaper,
            Rect(0f, 0f, wallpaper.rootSize.width.toFloat(), wallpaper.rootSize.height * .25f),
            columns = 9, rows = 5)?.map { it.luminance() }?.average()?.toFloat()
    }
    val fallbackLight = if (hasWallpaper) fallback.lightGlass else themeLight
    var light by remember(hasWallpaper) { mutableStateOf(luminance?.let { it >= .46f } ?: fallbackLight) }
    LaunchedEffect(luminance, fallbackLight) {
        // A dead band keeps brightness/crop previews from flipping polarity at the threshold.
        light = luminance?.let { if (light) it >= .40f else it >= .52f } ?: fallbackLight
    }
    return remember(light) { AssistantGlassPalette(light) }
}
