package com.xiaomanjun.sleepdownschedule.core.ui.designsystem

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
    val adaptive = rememberAssistantGlassPalette(config)
    val light = if (wallpaperAdaptive) adaptive.light else glassUsesLightStyle(config)
    val foreground = if (selected) Color.White else if (light) Color(0xFF172331) else Color.White
    val base = surfaceOverride ?: if (light) Color.White else Color(0xFF111315)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) .92f else 1f, spring(dampingRatio = .72f, stiffness = 520f), label = "glassButtonPress")
    Box(modifier.size(maxOf(size, SleepDownDesignTokens.Button.MinimumTouchSize))
        .semantics { contentDescription = label; role = Role.Button; this.selected = selected; if (!enabled) disabled() }
        .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        GlassSurface(backdrop, config, Modifier.size(size).graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .border(SleepDownDesignTokens.Button.BorderWidth,
                foreground.copy(alpha = if (enabled) .3f else .12f), CircleShape),
            shape = CircleShape, tokens = adaptive.controlTokens.copy(surfaceAlpha = if (light) .58f else .64f),
            baseSurfaceColorOverride = base, selected = selected,
            selectedSurfaceColorOverride = SleepDownDesignTokens.Button.Primary,
            restingDecorations = true) {
            androidx.compose.runtime.CompositionLocalProvider(LocalContentColor provides foreground.copy(alpha = if (enabled) 1f else .4f)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
            }
        }
    }
}
