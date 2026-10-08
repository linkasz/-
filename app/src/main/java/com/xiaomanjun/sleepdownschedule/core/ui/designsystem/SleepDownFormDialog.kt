package com.xiaomanjun.sleepdownschedule.core.ui.designsystem

import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.DialogLayout

/** Parent forms and nested pickers share one root host, so child menus cannot sit behind a Dialog window. */
@Composable
internal fun SleepDownFormDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val visible = remember { mutableStateOf(true) }
    // Use the same dialog queue and scaffold as SleepDownPickerDialog, including nested pages.
    DialogLayout(visible, enterTransition = fadeIn(tween(180)) + scaleIn(tween(220), initialScale = .96f),
        exitTransition = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = .98f),
        enableWindowDim = true, enableAutoLargeScreen = false, excludeFromBackdropCapture = true,
        renderInRootScaffold = LocalCenteredDialogRenderInRootScaffold.current) {
        BackHandler(onBack = onDismiss)
        Box(Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss),
            contentAlignment = Alignment.Center) {
            Box(Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = {})) { content() }
        }
    }
}
