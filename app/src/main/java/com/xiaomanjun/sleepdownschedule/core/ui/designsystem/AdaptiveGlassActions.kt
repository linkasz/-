package com.xiaomanjun.sleepdownschedule.core.ui.designsystem

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.xiaomanjun.sleepdownschedule.glass.ui.LocalGlassAppearance
import com.xiaomanjun.sleepdownschedule.glass.ui.rememberGlassAppearance
import com.xiaomanjun.sleepdownschedule.model.ScheduleConfigEntity

internal data class GlassAction(val label: String, val icon: ImageVector, val enabled: Boolean = true,
    val primary: Boolean = false, val destructive: Boolean = false, val onClick: () -> Unit)

/** All labels are measured at the active font scale; the whole row falls back together. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdaptiveGlassActions(actions: List<GlassAction>, backdrop: Backdrop?, config: ScheduleConfigEntity,
    modifier: Modifier = Modifier) {
    // An empty action group has no geometry; avoid dividing available width by zero.
    if (actions.isEmpty()) return
    val interactions = remember(actions.size) { List(actions.size) { MutableInteractionSource() } }
    val pressed = interactions.map { it.collectIsPressedAsState().value }.any { it }
    val measurer = rememberTextMeasurer(); val density = LocalDensity.current
    val style = MaterialTheme.typography.labelLarge
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val width = (maxWidth - 8.dp * (actions.size - 1)) / actions.size
        val icons = actions.any { with(density) { measurer.measure(AnnotatedString(it.label), style).size.width.toDp() + 28.dp > width } }
        val appearanceBinding = rememberGlassAppearance(config, frozen = pressed)
        CompositionLocalProvider(LocalGlassAppearance provides appearanceBinding.appearance) {
        Row(appearanceBinding.modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            actions.forEachIndexed { index, action ->
                // TooltipBox's modifier is inside another layout; Row weights belong on its direct child.
                Box(Modifier.weight(1f)) {
                TooltipBox(TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text(action.label) } }, state = rememberTooltipState()) {
                    QuickSheetLiquidAction(action.label, action.enabled, backdrop, config, Modifier.fillMaxWidth().semantics {
                        contentDescription = action.label
                    }, primary = action.primary, destructive = action.destructive,
                        // Grouped destructive actions keep the neutral material and use red ink.
                        height = SleepDownDesignTokens.CenteredDialog.ActionHeight,
                        interactionSource = interactions[index],
                        content = if (icons) ({ Icon(action.icon, null, Modifier.size(22.dp)) }) else null,
                        onClick = action.onClick)
                }
                }
            }
        }
        }
    }
}
