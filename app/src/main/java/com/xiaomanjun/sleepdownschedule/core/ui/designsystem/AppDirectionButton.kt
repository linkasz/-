package com.xiaomanjun.sleepdownschedule.core.ui.designsystem

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.*
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.catalog.components.LiquidButton
import com.xiaomanjun.sleepdownschedule.R
import com.xiaomanjun.sleepdownschedule.glass.ui.*
import com.xiaomanjun.sleepdownschedule.model.ScheduleConfigEntity
import com.xiaomanjun.sleepdownschedule.model.normalizedHomeChromeBlurScale

/** Week and month navigation share the original week material and a 48dp hit target. */
@Composable
fun AppDirectionButton(direction: Int, label: String, config: ScheduleConfigEntity, backdrop: Backdrop?,
    enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val light = glassUsesLightStyle(config)
    val foreground = if (light) Color.Black else Color.White
    Box(modifier.size(48.dp).clickable(enabled = enabled, onClickLabel = label, onClick = onClick).semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        if (backdrop != null) LiquidButton(
            onClick = { if (enabled) onClick() }, backdrop = backdrop,
            modifier = Modifier.size(34.dp).graphicsLayer { alpha = if (enabled) 1f else .35f }.clearAndSetSemantics { },
            isInteractive = enabled, surfaceColor = (if (light) Color(0xFFF2F4F8) else Color(0xFF121212))
                .copy(alpha = if (light) .54f else .45f),
            height = 34.dp, contentPadding = PaddingValues(0.dp),
            blurRadius = 2.dp * normalizedHomeChromeBlurScale(config.homeChromeBlurScale),
            lensHeight = 12.dp, lensAmount = 24.dp, chromaticAberration = false
        ) {
            Icon(painterResource(R.drawable.ic_arrow_back), label, tint = foreground,
                modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = if (direction > 0) 180f else 0f })
        } else GlassPill(null, config, Modifier.size(34.dp).graphicsLayer { alpha = if (enabled) 1f else .35f }.clearAndSetSemantics { },
            onClick = if (enabled) onClick else null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_arrow_back), label, tint = foreground,
                    modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = if (direction > 0) 180f else 0f })
            }
        }
    }
}
