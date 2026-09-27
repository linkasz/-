// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.overlay

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import top.yukonga.miuix.kmp.layout.DialogContentLayout
import top.yukonga.miuix.kmp.layout.DialogDefaults
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.DialogLayout

/**
 * A dialog with a title, a summary, and other contents.
 *
 * @param show Whether the [OverlayDialog] is shown.
 * @param modifier The modifier to be applied to the [OverlayDialog].
 * @param backgroundModifier The modifier drawn across the window behind the dialog surface.
 * @param title The title of the [OverlayDialog].
 * @param titleColor The color of the title.
 * @param summary The summary of the [OverlayDialog].
 * @param summaryColor The color of the summary.
 * @param backgroundColor The background color of the [OverlayDialog].
 * @param enableWindowDim Whether to enable window dimming when the [OverlayDialog] is shown.
 * @param onDismissRequest Will called when the user tries to dismiss the Dialog by clicking outside or pressing the back button.
 * @param onDismissFinished The callback when the [OverlayDialog] is completely dismissed.
 * @param outsideMargin The margin outside the [OverlayDialog].
 * @param insideMargin The margin inside the [OverlayDialog].
 * @param defaultWindowInsetsPadding Whether to apply default window insets padding to the [OverlayDialog].
 * @param renderInRootScaffold Whether to render the dialog in the root (outermost) Scaffold.
 *   When true (default), the dialog covers the full screen. When false, it renders within the
 *   current Scaffold's bounds.
 * @param animationProgressState Optional observer for the dialog's enter/exit progress.
 * @param enablePredictiveBackAnimation Whether back gesture progress scales or translates the dialog.
 * @param excludeFromBackdropCapture Whether this glass dialog needs the root scaffold's complete
 *   underlay capture. The popup host is structurally outside that producer.
 * @param content The [Composable] content of the [OverlayDialog].
 */
@Composable
fun OverlayDialog(
    show: Boolean,
    modifier: Modifier = Modifier,
    surfaceModifier: Modifier = Modifier,
    backgroundModifier: Modifier = Modifier,
    title: String? = null,
    titleColor: Color = DialogDefaults.titleColor(),
    summary: String? = null,
    summaryColor: Color = DialogDefaults.summaryColor(),
    backgroundColor: Color = DialogDefaults.backgroundColor(),
    enableWindowDim: Boolean = true,
    onDismissRequest: (() -> Unit)? = null,
    onDismissFinished: (() -> Unit)? = null,
    outsideMargin: DpSize = DialogDefaults.outsideMargin,
    insideMargin: DpSize = DialogDefaults.insideMargin,
    defaultWindowInsetsPadding: Boolean = true,
    renderInRootScaffold: Boolean = true,
    forceCenter: Boolean = false,
    animationProgressState: MutableFloatState? = null,
    enablePredictiveBackAnimation: Boolean = true,
    excludeFromBackdropCapture: Boolean = true,
    content: @Composable () -> Unit,
) {
    DialogContentLayout(
        show = show,
        titleColor = titleColor,
        summaryColor = summaryColor,
        backgroundColor = backgroundColor,
        outsideMargin = outsideMargin,
        insideMargin = insideMargin,
        popupHost = { visible, hostContent ->
            val visibleState = remember { mutableStateOf(false) }
            visibleState.value = visible
            DialogLayout(
                visible = visibleState,
                enableWindowDim = false,
                enterTransition = EnterTransition.None,
                exitTransition = ExitTransition.None,
                enableAutoLargeScreen = false,
                excludeFromBackdropCapture = excludeFromBackdropCapture,
                renderInRootScaffold = renderInRootScaffold,
            ) {
                hostContent()
            }
        },
        modifier = modifier,
        surfaceModifier = surfaceModifier,
        backgroundModifier = backgroundModifier,
        title = title,
        summary = summary,
        enableWindowDim = enableWindowDim,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        defaultWindowInsetsPadding = defaultWindowInsetsPadding,
        forceCenter = forceCenter,
        animationProgressState = animationProgressState,
        enablePredictiveBackAnimation = enablePredictiveBackAnimation,
        content = content,
    )
}
