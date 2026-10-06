package com.xiaomanjun.sleepdownschedule.core.ui.designsystem

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.xiaomanjun.sleepdownschedule.model.ScheduleConfigEntity
import com.xiaomanjun.sleepdownschedule.glass.ui.*

/** Settings and page chrome share geometry, material, press feedback and accessible hit targets. */
@Composable
fun AppGlassIconButton(backdrop: Backdrop?, config: ScheduleConfigEntity, label: String, onClick: () -> Unit,
    modifier: Modifier = Modifier, size: Dp = SleepDownDesignTokens.SecondaryPage.BackButtonSize,
    wallpaperAdaptive: Boolean = false, enabled: Boolean = true, selected: Boolean = false,
    surfaceOverride: Color? = null, content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val appearanceBinding = rememberGlassAppearance(config, frozen = pressed)
    val appearance = appearanceBinding.appearance
    val foreground = if (selected) Color.White else appearance.foreground
    val base = surfaceOverride ?: appearance.base
    Box(modifier.size(maxOf(size, SleepDownDesignTokens.Button.MinimumTouchSize))
        .semantics { contentDescription = label; role = Role.Button; this.selected = selected; if (!enabled) disabled() }
        .then(appearanceBinding.modifier)
        .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        androidx.compose.runtime.CompositionLocalProvider(LocalGlassAppearance provides appearance) {
        GlassSurface(backdrop, config, Modifier.size(size)
            .border(SleepDownDesignTokens.Button.BorderWidth,
                foreground.copy(alpha = if (enabled) .3f else .12f), CircleShape),
            shape = CircleShape, tokens = GlassTokens.pill().copy(surfaceAlpha = appearance.controlAlpha,
                blur = 2.dp * com.xiaomanjun.sleepdownschedule.model.normalizedHomeChromeBlurScale(config.homeChromeBlurScale)),
            baseSurfaceColorOverride = base, selected = selected,
            selectedSurfaceColorOverride = SleepDownDesignTokens.Button.Primary,
            restingDecorations = true, interactionSource = interaction, enabled = enabled) {
            androidx.compose.runtime.CompositionLocalProvider(LocalContentColor provides foreground.copy(alpha = if (enabled) 1f else .4f)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
            }
        }
        }
    }
}
