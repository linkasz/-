package com.xiaomanjun.sleepdownschedule.glass.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import com.xiaomanjun.sleepdownschedule.model.ScheduleConfigEntity
import com.xiaomanjun.sleepdownschedule.feature.home.day.LocalHomeReadability
import com.xiaomanjun.sleepdownschedule.feature.home.day.sampleVisibleWallpaperColors
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlin.math.sqrt

@Immutable
internal data class GlassAppearance(val light: Boolean, val detail: Float = 0f, val minimumControlAlpha: Float = 0f) {
    val foreground: Color get() = if (light) Color(0xFF172331) else Color.White
    val base: Color get() = if (light) Color.White else Color(0xFF111315)
    val controlAlpha: Float get() = maxOf((if (light) .12f else .18f) + .04f * detail, minimumControlAlpha)
    val readingAlpha: Float get() = (if (light) .30f else .38f) + .12f * detail
    val readingBlur: Float get() = 8f + 4f * detail
}

/** Keep a shared ink legible over mixed wallpaper without making every control opaque. */
internal fun protectGlassControlContrast(appearance: GlassAppearance, samples: List<Color>): GlassAppearance {
    if (samples.isEmpty()) return appearance
    val ink = appearance.foreground.luminance()
    fun readable(alpha: Float): Boolean = samples.all { sample ->
        val background = appearance.base.copy(alpha = alpha).compositeOver(sample.copy(alpha = 1f)).luminance()
        (maxOf(ink, background) + .05f) / (minOf(ink, background) + .05f) >= 4.5f
    }
    var low = appearance.controlAlpha
    if (readable(low)) return appearance
    var high = .72f
    repeat(8) {
        val middle = (low + high) / 2f
        if (readable(middle)) high = middle else low = middle
    }
    return appearance.copy(minimumControlAlpha = high)
}

internal val LocalGlassAppearance = compositionLocalOf<GlassAppearance?> { null }

/** The project's linear-luminance dead band; Web/sRGB thresholds are not interchangeable. */
internal fun resolveGlassAppearance(samples: List<Float>, previousLight: Boolean): GlassAppearance {
    val valid = samples.filter { it.isFinite() }.map { it.coerceIn(0f, 1f) }
    if (valid.isEmpty()) return GlassAppearance(previousLight)
    val mean = valid.average().toFloat()
    val light = if (previousLight) mean >= .46f else mean > .56f
    val variance = valid.sumOf { ((it - mean) * (it - mean)).toDouble() } / valid.size
    return GlassAppearance(light, (sqrt(variance).toFloat() * 3f).coerceIn(0f, 1f))
}

internal data class GlassAppearanceBinding(val appearance: GlassAppearance, val modifier: Modifier)

/** CPU thumbnail only. Geometry can move every frame without invalidating Composition. */
@OptIn(FlowPreview::class)
@Composable
internal fun rememberGlassAppearance(
    config: ScheduleConfigEntity,
    panel: Boolean = LocalReadablePanelControls.current,
    frozen: Boolean = false,
    bounds: Rect? = null
): GlassAppearanceBinding {
    val inherited = LocalGlassAppearance.current
    // A group owns one sampled palette. Resolve it in this composition, not one effect later,
    // so its child surface cannot briefly use yesterday's tint with today's foreground.
    if (inherited != null && !panel) return GlassAppearanceBinding(inherited, Modifier)
    val fallbackLight = if (panel) !appUsesDarkTheme(config) else glassUsesLightStyle(config)
    val wallpaper = LocalHomeReadability.current
    val geometry = remember { MutableStateFlow(Rect.Zero) }
    var appearance by remember(config.id, panel) { mutableStateOf(if (!panel) inherited ?: GlassAppearance(fallbackLight) else GlassAppearance(fallbackLight)) }
    LaunchedEffect(wallpaper, fallbackLight, panel, frozen, bounds, inherited) {
        if (frozen) return@LaunchedEffect
        geometry.debounce(100).collect { measured ->
            val region = (bounds ?: measured).translate(-wallpaper.rootOffsetInWindow)
            val colors = if (!panel && wallpaper.bitmap != null) {
                sampleVisibleWallpaperColors(wallpaper, region, columns = 16, rows = 16)
                    .orEmpty()
            } else emptyList()
            appearance = protectGlassControlContrast(
                resolveGlassAppearance(colors.map { it.luminance() },
                    if (colors.isEmpty()) fallbackLight else appearance.light), colors)
        }
    }
    return GlassAppearanceBinding(appearance,
        Modifier.onGloballyPositioned { geometry.value = it.boundsInWindow() })
}

internal fun glassControlPressScale(heightDp: Float, progress: Float, motionEnabled: Boolean = true, expansionDp: Float = 4f): Float =
    if (!motionEnabled || !heightDp.isFinite() || !progress.isFinite() || !expansionDp.isFinite() || heightDp <= 0f) 1f
    else 1f + minOf(expansionDp.coerceAtLeast(0f) / heightDp, .10f) * progress.coerceIn(0f, 1f)
