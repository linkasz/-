package com.xiaomanjun.sleepdownschedule.core.ui.designsystem

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.xiaomanjun.sleepdownschedule.glass.ui.*
import com.xiaomanjun.sleepdownschedule.model.ScheduleConfigEntity

data class GlassTab(val id: String, val label: String, val icon: ImageVector, val count: Int? = null)

/** Measure labels at the actual font scale; never wrap a segmented action row. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveGlassTabs(tabs: List<GlassTab>, selectedId: String, backdrop: Backdrop?, config: ScheduleConfigEntity,
    modifier: Modifier = Modifier, enabled: Boolean = true, onSelect: (String) -> Unit) {
    if (tabs.isEmpty()) return
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelLarge
    val density = androidx.compose.ui.platform.LocalDensity.current
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val cell = (maxWidth - 8.dp * (tabs.size - 1)) / tabs.size
        val iconOnly = tabs.any { with(density) {
            measurer.measure(AnnotatedString(it.label + (it.count?.let { n -> " $n" } ?: "")), style).size.width.toDp() + 24.dp > cell
        } }
        val appearanceBinding = rememberGlassAppearance(config)
        CompositionLocalProvider(LocalGlassAppearance provides appearanceBinding.appearance) {
        Row(appearanceBinding.modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tabs.forEach { tab ->
                val selected = tab.id == selectedId
                val foreground = if (selected) Color.White else if (LocalReadablePanelControls.current) LocalContentColor.current
                    else sleepDownGlassForegroundColor(config)
                // Keep equal widths on the direct Row child, outside TooltipBox's internal layout.
                Box(Modifier.weight(1f)) {
                TooltipBox(positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text(tab.label + (tab.count?.let { " · $it" } ?: "")) } }, state = rememberTooltipState()) {
                    GlassPill(backdrop, config, Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
                        contentDescription = tab.label + (tab.count?.let { "，${it}项" } ?: ""); this.selected = selected; role = Role.Tab
                        if (!enabled) disabled()
                    }, selected = selected, selectedSurfaceColorOverride = SleepDownDesignTokens.Button.Primary,
                        onClick = if (enabled) ({ onSelect(tab.id) }) else null) {
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            if (iconOnly) Icon(tab.icon, null, Modifier.size(20.dp), tint = foreground)
                            else Text(tab.label, color = foreground, style = style, maxLines = 1)
                            tab.count?.let { Text(" $it", color = foreground, style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                }
                }
            }
        }
        }
    }
}
