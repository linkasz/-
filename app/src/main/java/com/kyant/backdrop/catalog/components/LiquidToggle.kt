// Based on Kyant0/AndroidLiquidGlass catalog components, Apache-2.0.
// Modified for SleepDown-Schedule.
package com.kyant.backdrop.catalog.components

import com.xiaomanjun.sleepdownschedule.glass.ui.*

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import kotlin.math.abs
import kotlin.math.sign
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.catalog.utils.DampedDragAnimation
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.xiaomanjun.sleepdownschedule.glass.GlassBackdropDomain
import com.xiaomanjun.sleepdownschedule.glass.GlassEffectFrame
import com.xiaomanjun.sleepdownschedule.glass.GlassMaterialRole
import com.xiaomanjun.sleepdownschedule.glass.GlassMaterialSpec
import com.xiaomanjun.sleepdownschedule.glass.glassBackdropProducer
import com.xiaomanjun.sleepdownschedule.glass.rememberGlassCombinedBackdrop
import com.xiaomanjun.sleepdownschedule.glass.rememberGlassLayerBackdrop
import com.xiaomanjun.sleepdownschedule.glass.rememberGlassSurfaceDescriptor
import com.xiaomanjun.sleepdownschedule.glass.sleepDownGlassSurface
import kotlinx.coroutines.flow.collectLatest

internal fun liquidToggleReleaseTarget(
    didDrag: Boolean,
    dragFraction: Float,
    currentTarget: Boolean
): Boolean = if (didDrag) dragFraction >= 0.5f else !currentTarget

@Composable
fun LiquidToggle(
    selected: () -> Boolean,
    onSelect: (Boolean) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    enabled: Boolean = true
) {
    val isLightTheme = !isSystemInDarkTheme()
    val motionEnabled = rememberGlassMotionEnabled()
    val accentColor =
        if (isLightTheme) Color(0xFF34C759)
        else Color(0xFF30D158)
    val trackColor =
        if (isLightTheme) Color(0xFF787878).copy(0.2f)
        else Color(0xFF787880).copy(0.36f)

    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val touchSlopPx = LocalViewConfiguration.current.touchSlop
    val trackWidth = if (compact) 52.dp else 64.dp
    val trackHeight = if (compact) 24.dp else 28.dp
    val thumbWidth = if (compact) 32.dp else 40.dp
    val thumbHeight = if (compact) 20.dp else 24.dp
    val dragWidth = with(density) { (trackWidth - thumbWidth - 4.dp).toPx() }
    val animationScope = rememberCoroutineScope()
    val latestSelected = rememberUpdatedState(selected)
    val latestOnSelect = rememberUpdatedState(onSelect)
    val latestEnabled = rememberUpdatedState(enabled)
    var didDrag by remember { mutableStateOf(false) }
    var horizontalDrag by remember { mutableStateOf(false) }
    var accumulatedDragX by remember { mutableFloatStateOf(0f) }
    var accumulatedDragY by remember { mutableFloatStateOf(0f) }
    var fraction by remember { mutableFloatStateOf(if (selected()) 1f else 0f) }
    var targetSelected by remember { mutableStateOf(selected()) }
    val dampedDragAnimation = remember(animationScope, compact, dragWidth, isLtr) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = fraction,
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = if (compact) 1.2f else 1.5f,
            onDragStarted = {
                didDrag = false
                horizontalDrag = false
                accumulatedDragX = 0f
                accumulatedDragY = 0f
            },
            onDragStopped = {
                targetSelected = if (latestEnabled.value) {
                    liquidToggleReleaseTarget(didDrag, fraction, targetSelected)
                } else {
                    latestSelected.value()
                }
                fraction = if (targetSelected) 1f else 0f
                if (latestEnabled.value) latestOnSelect.value(targetSelected)
                didDrag = false
                horizontalDrag = false
                accumulatedDragX = 0f
                accumulatedDragY = 0f
            },
            onDragCancelled = {
                targetSelected = latestSelected.value()
                fraction = if (targetSelected) 1f else 0f
                didDrag = false
                horizontalDrag = false
                accumulatedDragX = 0f
                accumulatedDragY = 0f
                animateToValue(fraction)
            },
            onDrag = { _, dragAmount ->
                if (latestEnabled.value) {
                    val nextAccumulatedX = accumulatedDragX + dragAmount.x
                    val nextAccumulatedY = accumulatedDragY + dragAmount.y
                    if (!didDrag && maxOf(abs(nextAccumulatedX), abs(nextAccumulatedY)) > touchSlopPx) {
                        didDrag = true
                        horizontalDrag = abs(nextAccumulatedX) > abs(nextAccumulatedY)
                        if (horizontalDrag) {
                            val beyondSlop = nextAccumulatedX - sign(nextAccumulatedX) * touchSlopPx
                            fraction = (fraction + (if (isLtr) beyondSlop else -beyondSlop) / dragWidth)
                                .fastCoerceIn(0f, 1f)
                        }
                    } else if (didDrag && horizontalDrag) {
                        val delta = dragAmount.x / dragWidth
                        fraction =
                            if (isLtr) (fraction + delta).fastCoerceIn(0f, 1f)
                            else (fraction - delta).fastCoerceIn(0f, 1f)
                    }
                    accumulatedDragX = nextAccumulatedX
                    accumulatedDragY = nextAccumulatedY
                }
            }
        )
    }
    LaunchedEffect(dampedDragAnimation) {
        snapshotFlow { fraction }
            .collectLatest { fraction ->
                dampedDragAnimation.updateValue(fraction)
            }
    }
    LaunchedEffect(dampedDragAnimation) {
        snapshotFlow { latestSelected.value() }
            .collectLatest { isSelected ->
                targetSelected = isSelected
                val target = if (isSelected) 1f else 0f
                if (kotlin.math.abs(target - dampedDragAnimation.value) > 0.001f) {
                    fraction = target
                    dampedDragAnimation.animateToValue(target)
                }
            }
    }

    val trackBackdrop = rememberGlassLayerBackdrop(
        domain = GlassBackdropDomain.ChromeCombined,
        providerId = "liquid-toggle-track"
    )
    val material = remember {
        GlassMaterialSpec(
            role = GlassMaterialRole.Control,
            blur = 8.dp,
            lensHeight = 5.dp,
            lensAmount = 10.dp,
            surfaceAlpha = 1f,
            borderAlpha = 0f,
            highlightAlpha = 0.62f,
            shadowAlpha = 0.05f,
            innerShadowAlpha = 1f,
            chromaticAberration = false,
            depthEffect = false
        )
    }
    val descriptor = rememberGlassSurfaceDescriptor(
        debugLabel = "LiquidToggleThumb",
        domain = GlassBackdropDomain.ChromeCombined,
        materialRole = GlassMaterialRole.Control,
        sceneKey = "liquid-toggle-thumb"
    )

    Box(
        modifier
            // The visual thumb is narrower than the track. Attach the gesture to the full
            // switch bounds so taps on either end of the track behave like a native switch.
            .then(dampedDragAnimation.modifier)
            .semantics {
                role = Role.Switch
                toggleableState = if (latestSelected.value()) {
                    androidx.compose.ui.state.ToggleableState.On
                } else {
                    androidx.compose.ui.state.ToggleableState.Off
                }
                stateDescription = if (latestSelected.value()) "已开启" else "已关闭"
                if (!latestEnabled.value) disabled()
                onClick(label = "切换开关") {
                    if (!latestEnabled.value) return@onClick false
                    latestOnSelect.value(!latestSelected.value())
                    true
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .glassBackdropProducer(trackBackdrop)
                .clip(Capsule())
                .drawBehind {
                    val fraction = dampedDragAnimation.value
                    drawRect(lerp(trackColor, accentColor, fraction))
                }
                .size(trackWidth, trackHeight)
        )

        Box(
            Modifier
                .graphicsLayer {
                    val fraction = dampedDragAnimation.value
                    val padding = 2f.dp.toPx()
                    translationX =
                        if (isLtr) lerp(padding, padding + dragWidth, fraction)
                        else lerp(-padding, -(padding + dragWidth), fraction)
                }
                .sleepDownGlassSurface(
                    backdrop = rememberGlassCombinedBackdrop(
                        backdrop,
                        rememberBackdrop(trackBackdrop) { drawBackdrop ->
                            val progress = dampedDragAnimation.pressProgress
                            val scaleX = lerp(2f / 3f, 0.75f, progress)
                            val scaleY = lerp(0f, 0.75f, progress)
                            scale(scaleX, scaleY) {
                                drawBackdrop()
                            }
                        }
                    ),
                    descriptor = descriptor,
                    material = material,
                    shape = { Capsule() },
                    effectFrame = GlassEffectFrame(blur = null),
                    effectsOverride = {
                        val progress = dampedDragAnimation.pressProgress
                        vibrancy()
                        blur(8f.dp.toPx() * (1f - progress))
                        lens(
                            5f.dp.toPx() * progress,
                            10f.dp.toPx() * progress,
                            chromaticAberration = false
                        )
                    },
                    highlightOverride = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Ambient.copy(
                            width = Highlight.Ambient.width / 1.5f,
                            blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                            alpha = progress * 0.45f
                        )
                    },
                    shadowOverride = {
                        Shadow(
                            radius = 4f.dp,
                            color = Color.Black.copy(alpha = 0.05f)
                        )
                    },
                    innerShadowOverride = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = 4f.dp * progress,
                            alpha = progress
                        )
                    },
                    additionalLayerBlock = {
                        scaleX = if (motionEnabled) dampedDragAnimation.scaleX else 1f
                        scaleY = if (motionEnabled) dampedDragAnimation.scaleY else 1f
                        val velocity = dampedDragAnimation.velocity / 50f
                        if (motionEnabled) {
                            scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                            scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                        }
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(Color.White.copy(alpha = 1f - progress))
                    }
                )
                .size(thumbWidth, thumbHeight)
        )
    }
}
