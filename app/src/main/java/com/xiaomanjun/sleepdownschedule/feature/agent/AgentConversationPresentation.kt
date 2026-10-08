package com.xiaomanjun.sleepdownschedule.feature.agent

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.DialogLayout

/** Keep the original compact entry alongside a full page, without two chat/voice controllers. */
@Composable
internal fun AgentConversationPresentation(fullScreen: Boolean, alpha: Float, base: Color, content: @Composable () -> Unit) {
    if (fullScreen) {
        Box(Modifier.fillMaxSize().graphicsLayer {
            this.alpha = alpha
            translationY = (1f - alpha) * 12.dp.toPx()
        }.background(base.copy(alpha = 1f))) { content() }
    } else {
        val visible = remember { mutableStateOf(true) }
        // Pickers and forms share this queue, keeping voice menus above the compact entry.
        DialogLayout(visible, enterTransition = EnterTransition.None, exitTransition = ExitTransition.None,
            // A glass assistant consumes the page underlay; never capture its own previous frame.
            excludeFromBackdropCapture = true,
            enableWindowDim = false, enableAutoLargeScreen = false, renderInRootScaffold = true, content = content)
    }
}
