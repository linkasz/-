package com.xiaomanjun.sleepdownschedule.core.ui.designsystem

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.xiaomanjun.sleepdownschedule.R
import com.xiaomanjun.sleepdownschedule.model.ScheduleConfigEntity

/** Week and month navigation share the original week material and a 48dp hit target. */
@Composable
fun AppDirectionButton(direction: Int, label: String, config: ScheduleConfigEntity, backdrop: Backdrop?,
    enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    AppGlassIconButton(backdrop, config, label, onClick, modifier, size = 34.dp, enabled = enabled) {
        Icon(painterResource(R.drawable.ic_arrow_back), null, tint = androidx.compose.material3.LocalContentColor.current,
            modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = if (direction > 0) 180f else 0f })
    }
}
