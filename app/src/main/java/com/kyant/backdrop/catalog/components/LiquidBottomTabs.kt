// Based on Kyant0/AndroidLiquidGlass catalog components, Apache-2.0.
// Modified for SleepDown-Schedule.
package com.kyant.backdrop.catalog.components

import com.xiaomanjun.sleepdownschedule.glass.ui.*

import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.animate
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.catalog.utils.InteractiveHighlight
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

@Composable
fun LiquidBottomTabs(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    modifier: Modifier = Modifier,
    containerHeight: Dp = 64f.dp,
    indicatorHeight: Dp = 56f.dp,
    horizontalPadding: Dp = 4f.dp,
    blurRadius: Dp = 8f.dp,
    containerAlpha: Float = 0.4f,
    lensHeight: Dp = 34f.dp,
    lensAmount: Dp = 40f.dp,
    indicatorWidthOverflow: Dp = 0.dp,
    indicatorHeightOverflow: Dp = 0.dp,
    indicatorLensHeight: Dp? = null,
    indicatorLensAmount: Dp? = null,
    officialHighlightAlpha: Float = 1f,
    officialShadowAlpha: Float = 1f,
    officialInnerShadowAlpha: Float = 1f,
    containerShadowEnabled: Boolean = true,
    indicatorShadowEnabled: Boolean = true,
    indicatorInnerShadowEnabled: Boolean = true,
    pressedContentScale: Float = 1.2f,
    movingAccentContent: Boolean = true,
    chromaticAberrationEnabled: Boolean = false,
    isLightThemeOverride: Boolean? = null,
    lightContainerColor: Color = Color(0xFFFAFAFA),
    lightAccentColor: Color = Color(0xFF0088FF),
    darkAccentColor: Color = Color(0xFF0091FF),
    useOfficialGlassParameters: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val isLightTheme = isLightThemeOverride ?: !isSystemInDarkTheme()
    val motionEnabled = rememberGlassMotionEnabled()
    // Theme ownership can change when a dock moves between wallpaper-adaptive Home and the app
    // themed settings page. Keep one Kyant surface alive and crossfade its material after the tab
    // settles; recreating light/dark surfaces at release produces a visible one-frame flash.
    val themeBlend by animateFloatAsState(
        targetValue = if (isLightTheme) 1f else 0f,
        animationSpec = tween(220),
        label = "LiquidBottomTabsThemeBlend"
    )
    val animatedContainerAlpha by animateFloatAsState(
        targetValue = containerAlpha * .72f,
        animationSpec = tween(220),
        label = "LiquidBottomTabsContainerAlpha"
    )
    val accentColor by animateColorAsState(
        targetValue = if (isLightTheme) lightAccentColor else darkAccentColor,
        animationSpec = tween(220),
        label = "LiquidBottomTabsAccentColor"
    )
    val lightContainerSurface = lightContainerColor.copy(alpha = animatedContainerAlpha)
    val darkContainerSurface = Color(0xFF121212).copy(alpha = animatedContainerAlpha)

    val tabsBackdrop = rememberGlassLayerBackdrop(
        domain = GlassBackdropDomain.ChromeCombined,
        providerId = "liquid-bottom-tabs-accent-content"
    )
    val containerMaterial = remember(
        blurRadius,
        lensHeight,
        lensAmount,
        chromaticAberrationEnabled,
        containerShadowEnabled
    ) {
        GlassMaterialSpec(
            role = GlassMaterialRole.Control,
            blur = blurRadius,
            lensHeight = lensHeight,
            lensAmount = lensAmount,
            surfaceAlpha = 1f,
            borderAlpha = 0f,
            highlightAlpha = 1f,
            shadowAlpha = if (containerShadowEnabled) 1f else 0f,
            innerShadowAlpha = 0f,
            chromaticAberration = chromaticAberrationEnabled,
            depthEffect = false
        )
    }
    val movingAccentMaterial = remember(
        containerMaterial,
        useOfficialGlassParameters,
        officialHighlightAlpha
    ) {
        containerMaterial.copy(
            highlightAlpha = if (useOfficialGlassParameters) officialHighlightAlpha else 0.45f
        )
    }
    val selectedLensHeight = indicatorLensHeight ?: if (useOfficialGlassParameters) 10f.dp else 22f.dp
    val selectedLensAmount = indicatorLensAmount ?: if (useOfficialGlassParameters) 14f.dp else 31f.dp
    val indicatorMaterial = remember(
        selectedLensHeight,
        selectedLensAmount,
        useOfficialGlassParameters,
        officialHighlightAlpha,
        officialShadowAlpha,
        officialInnerShadowAlpha,
        indicatorShadowEnabled,
        indicatorInnerShadowEnabled
    ) {
        GlassMaterialSpec(
            role = GlassMaterialRole.Control,
            blur = 0.dp,
            lensHeight = selectedLensHeight,
            lensAmount = selectedLensAmount,
            surfaceAlpha = if (useOfficialGlassParameters) 0.1f else 0.07f,
            borderAlpha = 0f,
            highlightAlpha = if (useOfficialGlassParameters) officialHighlightAlpha else 0.45f,
            shadowAlpha = if (indicatorShadowEnabled) {
                if (useOfficialGlassParameters) officialShadowAlpha else 1f
            } else {
                0f
            },
            innerShadowAlpha = if (indicatorInnerShadowEnabled) {
                if (useOfficialGlassParameters) officialInnerShadowAlpha else 1f
            } else {
                0f
            },
            chromaticAberration = if (useOfficialGlassParameters) true else chromaticAberrationEnabled,
            depthEffect = false,
            useVibrancy = false
        )
    }
    val containerDescriptor = rememberGlassSurfaceDescriptor(
        debugLabel = "LiquidBottomTabsContainer",
        domain = GlassBackdropDomain.ChromeCombined,
        materialRole = GlassMaterialRole.Control,
        sceneKey = "liquid-bottom-tabs-container"
    )
    val movingAccentDescriptor = rememberGlassSurfaceDescriptor(
        debugLabel = "LiquidBottomTabsMovingAccent",
        domain = GlassBackdropDomain.ChromeCombined,
        materialRole = GlassMaterialRole.Control,
        sceneKey = "liquid-bottom-tabs-moving-accent"
    )
    val indicatorDescriptor = rememberGlassSurfaceDescriptor(
        debugLabel = "LiquidBottomTabsIndicator",
        domain = GlassBackdropDomain.ChromeCombined,
        materialRole = GlassMaterialRole.Control,
        sceneKey = "liquid-bottom-tabs-indicator"
    )

    BoxWithConstraints(
        modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val tabWidth = with(density) {
            (constraints.maxWidth.toFloat() - horizontalPadding.toPx() * 2f) / tabsCount
        }
        val dragOffsetPx = remember { mutableFloatStateOf(0f) }
        val offsetSettleJob = remember { arrayOfNulls<Job>(1) }
        val panelOffset by remember(density, constraints.maxWidth) {
            derivedStateOf {
                val fraction = if (constraints.maxWidth > 0) {
                    (dragOffsetPx.floatValue / constraints.maxWidth).fastCoerceIn(-1f, 1f)
                } else {
                    0f
                }
                with(density) {
                    4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val latestTabWidth = rememberUpdatedState(tabWidth)
        val latestIsLtr = rememberUpdatedState(isLtr)
        val latestOnTabSelected = rememberUpdatedState(onTabSelected)
        val animationScope = rememberCoroutineScope()
        var currentIndex by remember {
            mutableIntStateOf(selectedTabIndex().fastCoerceIn(0, tabsCount - 1))
        }
        val dragAnimationRef = remember(animationScope) { arrayOfNulls<DockDragAnimation>(1) }
        val interactiveHighlight = remember(animationScope) {
            InteractiveHighlight(
                animationScope = animationScope,
                position = { size, _ ->
                    val selectedValue = dragAnimationRef[0]?.value ?: selectedTabIndex().toFloat()
                    val currentTabWidth = latestTabWidth.value
                    Offset(
                        if (latestIsLtr.value) (selectedValue + 0.5f) * currentTabWidth + panelOffset
                        else size.width - (selectedValue + 0.5f) * currentTabWidth + panelOffset,
                        size.height / 2f
                    )
                }
            )
        }
        val dockDragAnimation = remember(animationScope) {
            DockDragAnimation(
                animationScope = animationScope,
                initialValue = selectedTabIndex().toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 78f / 56f,
                onDragStarted = {
                    offsetSettleJob[0]?.cancel()
                    interactiveHighlight.updateExternal(Offset.Zero, pressed = true)
                },
                onDragStopped = {
                    val targetIndex = dockSettledIndex(targetValue, tabsCount)
                    val selectionChanged = currentIndex != targetIndex
                    currentIndex = targetIndex
                    animateToValue(targetIndex.toFloat(), releaseAfterAnimation = false)
                    if (selectionChanged) latestOnTabSelected.value(targetIndex)
                    interactiveHighlight.updateExternal(Offset.Zero, pressed = false)
                    val releaseOffset = dragOffsetPx.floatValue
                    offsetSettleJob[0] = animationScope.launch {
                        animate(
                            initialValue = releaseOffset,
                            targetValue = 0f,
                            animationSpec = spring(1f, 300f, 0.5f)
                        ) { value, _ ->
                            dragOffsetPx.floatValue = value
                        }
                    }
                },
                onDrag = { _, dragAmount ->
                    updateValue(
                        dockDragTarget(
                            currentTarget = targetValue,
                            dragAmountPx = dragAmount.x,
                            tabWidthPx = latestTabWidth.value,
                            isLtr = latestIsLtr.value,
                            tabsCount = tabsCount
                        )
                    )
                    dragOffsetPx.floatValue += dragAmount.x
                }
            )
        }
        dragAnimationRef[0] = dockDragAnimation
        LaunchedEffect(selectedTabIndex()) {
            val index = selectedTabIndex().fastCoerceIn(0, tabsCount - 1)
            if (currentIndex != index || abs(dockDragAnimation.targetValue - index.toFloat()) > 0.01f) {
                currentIndex = index
                dockDragAnimation.animateToValue(index.toFloat())
            }
        }

        Row(
            Modifier
                .graphicsLayer {
                    translationX = panelOffset
                }
                .sleepDownGlassSurface(
                    backdrop = backdrop,
                    descriptor = containerDescriptor,
                    material = containerMaterial,
                    shape = { Capsule() },
                    effectFrame = GlassEffectFrame(blur = null),
                    effectsOverride = {
                        vibrancy()
                        blur(blurRadius.toPx())
                        lens(
                            lensHeight.toPx(),
                            lensAmount.toPx(),
                            chromaticAberration = chromaticAberrationEnabled
                        )
                    },
                    highlightOverride = { Highlight.Default },
                    additionalLayerBlock = {
                        val progress = dockDragAnimation.pressProgress
                        val scale = if (motionEnabled) lerp(1f, 1f + 16f.dp.toPx() / size.width, progress) else 1f
                        scaleX = scale
                        scaleY = scale
                    },
                    shadowOverride = if (containerShadowEnabled) ({ Shadow.Default }) else null,
                    onDrawSurface = {
                        drawRect(darkContainerSurface, alpha = 1f - themeBlend)
                        drawRect(lightContainerSurface, alpha = themeBlend)
                    }
                )
                .then(interactiveHighlight.modifier)
                .height(containerHeight)
                .fillMaxWidth()
                .padding(horizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )

        if (movingAccentContent) {
            CompositionLocalProvider(
                LocalLiquidBottomTabScale provides {
                    lerp(1f, pressedContentScale, dockDragAnimation.pressProgress)
                }
            ) {
                Row(
                    Modifier
                        .clearAndSetSemantics {}
                        .alpha(0f)
                        .glassBackdropProducer(tabsBackdrop)
                        .graphicsLayer {
                            translationX = panelOffset
                        }
                        .sleepDownGlassSurface(
                            backdrop = backdrop,
                            descriptor = movingAccentDescriptor,
                            material = movingAccentMaterial,
                            shape = { Capsule() },
                            effectFrame = GlassEffectFrame(blur = null),
                            effectsOverride = {
                                val progress = dockDragAnimation.pressProgress
                                vibrancy()
                                blur(blurRadius.toPx())
                                lens(
                                    lensHeight.toPx() * progress,
                                    lensAmount.toPx() * progress,
                                    chromaticAberration = chromaticAberrationEnabled
                                )
                            },
                            highlightOverride = {
                                val progress = dockDragAnimation.pressProgress
                                Highlight.Default.copy(alpha = progress * if (useOfficialGlassParameters) officialHighlightAlpha else 0.45f)
                            },
                            shadowOverride = if (containerShadowEnabled) ({ Shadow.Default }) else null,
                            onDrawSurface = {
                                drawRect(darkContainerSurface, alpha = 1f - themeBlend)
                                drawRect(lightContainerSurface, alpha = themeBlend)
                            }
                        )
                        .then(interactiveHighlight.modifier)
                        .height(indicatorHeight)
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding)
                        .graphicsLayer(colorFilter = ColorFilter.tint(accentColor)),
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }
        }

        Box(
            Modifier
                .padding(horizontal = horizontalPadding)
                .graphicsLayer {
                    val progress = dockDragAnimation.pressProgress
                    val widthOverflowPx = indicatorWidthOverflow.toPx() * progress
                    translationX =
                        if (isLtr) dockDragAnimation.value * tabWidth + panelOffset - widthOverflowPx / 2f
                        else size.width - (dockDragAnimation.value + 1f) * tabWidth + panelOffset + widthOverflowPx / 2f
                }
                .then(dockDragAnimation.modifier)
                .sleepDownGlassSurface(
                    backdrop = rememberGlassCombinedBackdrop(backdrop, tabsBackdrop),
                    descriptor = indicatorDescriptor,
                    material = indicatorMaterial,
                    shape = { Capsule() },
                    effectFrame = GlassEffectFrame(blur = null),
                    effectsOverride = {
                        val progress = dockDragAnimation.pressProgress
                        lens(
                            selectedLensHeight.toPx() * progress,
                            selectedLensAmount.toPx() * progress,
                            chromaticAberration = if (useOfficialGlassParameters) true else chromaticAberrationEnabled
                        )
                    },
                    highlightOverride = {
                        val progress = dockDragAnimation.pressProgress
                        Highlight.Default.copy(alpha = progress * if (useOfficialGlassParameters) officialHighlightAlpha else 0.45f)
                    },
                    shadowOverride = if (indicatorShadowEnabled) {
                        {
                            val progress = dockDragAnimation.pressProgress
                            Shadow(alpha = progress * if (useOfficialGlassParameters) officialShadowAlpha else 1f)
                        }
                    } else null,
                    innerShadowOverride = if (indicatorInnerShadowEnabled) {
                        {
                            val progress = dockDragAnimation.pressProgress
                            InnerShadow(
                                radius = 8f.dp * progress,
                                alpha = progress * if (useOfficialGlassParameters) officialInnerShadowAlpha else 1f
                            )
                        }
                    } else null,
                    additionalLayerBlock = {
                        scaleX = if (motionEnabled) dockDragAnimation.scaleX else 1f
                        scaleY = if (motionEnabled) dockDragAnimation.scaleY else 1f
                        val velocity = dockDragAnimation.velocity / 10f
                        if (motionEnabled) {
                            scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                            scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                        }
                    },
                    onDrawSurface = {
                        val progress = dockDragAnimation.pressProgress
                        val neutralAlpha = if (useOfficialGlassParameters) 0.1f else 0.07f
                        drawRect(
                            Color.White.copy(alpha = neutralAlpha),
                            alpha = (1f - themeBlend) * (1f - progress)
                        )
                        drawRect(
                            Color.Black.copy(alpha = neutralAlpha),
                            alpha = themeBlend * (1f - progress)
                        )
                        drawRect(Color.Black.copy(alpha = 0.03f * progress))
                    }
                )
                .height(indicatorHeight + indicatorHeightOverflow * dockDragAnimation.pressProgress)
                .then(
                    Modifier.width(with(density) { tabWidth.toDp() } + indicatorWidthOverflow * dockDragAnimation.pressProgress)
                )
        )
    }
}
