package com.xiaomanjun.sleepdownschedule.core.ui.designsystem

import com.xiaomanjun.sleepdownschedule.ScheduleConfigEntity
import com.xiaomanjun.sleepdownschedule.glass.*
import com.xiaomanjun.sleepdownschedule.glass.ui.appUsesDarkTheme
import com.xiaomanjun.sleepdownschedule.glass.ui.rememberGlassAppearance
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import com.xiaomanjun.sleepdownschedule.glass.ui.glassUsesLightStyle
import com.xiaomanjun.sleepdownschedule.glass.ui.courseCardGlassEffectFrame
import com.xiaomanjun.sleepdownschedule.glass.ui.courseGlassTintAlpha
import com.xiaomanjun.sleepdownschedule.glass.ui.courseCardBrightnessAttenuation
import com.xiaomanjun.sleepdownschedule.glass.ui.presetCourseCardHighlight
import com.xiaomanjun.sleepdownschedule.core.performance.LocalGlassQuality

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.components.LiquidButton
import com.kyant.backdrop.catalog.utils.InteractiveHighlight
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedCornerStyle
import com.kyant.shapes.RoundedRectangle
import kotlin.math.max
import top.yukonga.miuix.kmp.utils.MiuixPopupBackdropCapture

internal val LocalCenteredDialogSceneBackdrop = compositionLocalOf<Backdrop?> { null }
internal val LocalCenteredDialogRenderInRootScaffold = compositionLocalOf { true }

/** Complete root-scene producer; the popup host is a sibling and therefore cannot self-sample. */
@Composable
internal fun rememberCenteredDialogSceneBackdrop(providerId: String): LayerBackdrop =
    rememberGlassLayerBackdrop(
        domain = GlassBackdropDomain.DialogBridge,
        providerId = providerId
    ) { drawContent() }

@Composable
internal fun Modifier.centeredDialogSceneProducer(backdrop: LayerBackdrop): Modifier = then(
    if (MiuixPopupBackdropCapture.backdropCaptureNeeded) {
        Modifier.glassBackdropProducer(backdrop)
    } else {
        Modifier
    }
)

/**
 * Crossfades alternate centered-dialog content while the Miuix shell interpolates its measured
 * size.  Keeping the transition inside the real dialog content means its backdrop and input host
 * remain stable rather than being rebuilt for each picker style.
 */
@Composable
internal fun <T> CenteredDialogContentTransition(
    targetState: T,
    modifier: Modifier = Modifier,
    label: String = "CenteredDialogContentTransition",
    smoothResize: Boolean = false,
    content: @Composable (T) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            fadeIn(tween(durationMillis = 160, delayMillis = 24)) togetherWith
                fadeOut(tween(durationMillis = 100)) using if (smoothResize) {
                    SizeTransform(clip = true) { _, _ -> tween(260, easing = FastOutSlowInEasing) }
                } else SizeTransform(clip = false)
        },
        label = label,
        content = { state -> content(state) }
    )
}

/**
 * Canonical shell for compact selector/editor dialogs.
 *
 * The content remains a free [ColumnScope] so a date picker, multi-column period editor or text
 * editor can keep its own density without cloning the window, material and spacing contract.
 */
@Composable
fun SleepDownPickerDialog(
    show: Boolean,
    title: String,
    onDismissRequest: () -> Unit,
    backdrop: Backdrop?,
    config: ScheduleConfigEntity,
    onDismissFinished: (() -> Unit)? = null,
    enableWindowDim: Boolean = false,
    renderInRootScaffold: Boolean? = null,
    contentSpacing: Dp = SleepDownDesignTokens.QuickSheet.PickerContentSpacing,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    blurRadius: Dp = 28.dp,
    titleAction: (@Composable () -> Unit)? = null,
    contentTransitionKey: Any? = null,
    contentForState: (@Composable ColumnScope.(Any?) -> Unit)? = null,
    scrollableContent: Boolean = false,
    smoothContentResize: Boolean = false,
    bottomActions: (@Composable () -> Unit)? = null,
    readableSurface: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val visuals = rememberCenteredDialogVisuals(
        backdrop = backdrop,
        config = config,
        blurRadius = blurRadius,
        enableForegroundHighlight = false
    )
    val resolvedRenderInRootScaffold = renderInRootScaffold
        ?: LocalCenteredDialogRenderInRootScaffold.current
    val foreground = sleepDownPanelForegroundColor(config)
    top.yukonga.miuix.kmp.overlay.OverlayDialog(
        show = show,
        title = null,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        enableWindowDim = enableWindowDim,
        backgroundColor = Color.Transparent,
        forceCenter = true,
        // Nested pickers sit above another form: a readable inner tint prevents its text showing through.
        // Keep the shared blur, rounded glass edge and entrance animation underneath this tint.
        surfaceModifier = if (readableSurface) visuals.surfaceModifier.background(
            (if (appUsesDarkTheme(config)) Color(0xFF1D1F23) else Color(0xFFFAFBFC)).copy(alpha = 0.96f)
        ) else visuals.surfaceModifier,
        backgroundModifier = visuals.backgroundModifier,
        animationProgressState = visuals.animationProgress,
        enablePredictiveBackAnimation = false,
        excludeFromBackdropCapture = true,
        renderInRootScaffold = resolvedRenderInRootScaffold,
        outsideMargin = DpSize(
            SleepDownDesignTokens.QuickSheet.HorizontalMargin,
            SleepDownDesignTokens.QuickSheet.VerticalMargin
        ),
        insideMargin = DpSize(
            SleepDownDesignTokens.CenteredDialog.ContentPadding,
            SleepDownDesignTokens.CenteredDialog.AlertVerticalPadding
        )
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides foreground,
            com.xiaomanjun.sleepdownschedule.glass.ui.LocalReadablePanelControls provides readableSurface
        ) {
            Column(
                modifier = Modifier
                    .padding(contentPadding)
                    .padding(
                        top = SleepDownDesignTokens.QuickSheet.PickerVerticalInset -
                            SleepDownDesignTokens.CenteredDialog.AlertVerticalPadding
                    ),
                verticalArrangement = Arrangement.spacedBy(contentSpacing)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.width(40.dp))
                    if (contentTransitionKey == null) {
                        Text(
                            text = title,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = foreground,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    } else {
                        CenteredDialogContentTransition(
                            targetState = title,
                            modifier = Modifier.weight(1f),
                            label = "picker-dialog-title"
                        ) { displayedTitle ->
                            Text(
                                text = displayedTitle,
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = foreground,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                    Box(Modifier.width(40.dp), contentAlignment = Alignment.Center) {
                        titleAction?.invoke()
                    }
                }
                val bodyModifier = if (scrollableContent) {
                    Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                } else Modifier
                if (contentTransitionKey == null) {
                    if (scrollableContent) Column(bodyModifier, content = content) else content()
                } else {
                    CenteredDialogContentTransition(
                        targetState = contentTransitionKey,
                        modifier = bodyModifier,
                        smoothResize = smoothContentResize,
                        label = "picker-dialog-content"
                    ) { displayedState ->
                        Column(verticalArrangement = Arrangement.spacedBy(contentSpacing)) {
                            if (contentForState != null) contentForState(displayedState) else content()
                        }
                    }
                }
                bottomActions?.invoke()
            }
        }
    }
}

internal class CenteredDialogVisuals(
    val surfaceModifier: Modifier,
    val backgroundModifier: Modifier,
    val animationProgress: MutableFloatState
)

/**
 * Shared visual state for modified Miuix centered dialogs.
 *
 * The background pass deliberately uses one prebuilt plain-blur chain and no
 * lens/vibrancy/shadow chain. Only its opacity follows the dialog's animation progress, avoiding
 * effect reconstruction during the opening and closing frames.
 */
@Composable
internal fun rememberCenteredDialogVisuals(
    backdrop: Backdrop?,
    config: ScheduleConfigEntity,
    blurRadius: Dp,
    maxWidth: Dp = SleepDownDesignTokens.CenteredDialog.Width,
    enableForegroundHighlight: Boolean = true
): CenteredDialogVisuals {
    val animationProgress = remember { mutableFloatStateOf(0f) }
    val completeUnderlayBackdrop = LocalCenteredDialogSceneBackdrop.current ?: backdrop
    val density = LocalDensity.current
    val highlightScope = rememberCoroutineScope()
    val dark = appUsesDarkTheme(config)
    val highlightRadiusPx = with(density) { 112.dp.toPx() }
    val foregroundHighlight = remember(highlightScope, highlightRadiusPx, dark) {
        InteractiveHighlight(
            animationScope = highlightScope,
            radius = { highlightRadiusPx },
            ambientAlpha = 0f,
            spotAlpha = if (dark) 0.11f else 0.16f,
            fallbackAlpha = if (dark) 0.13f else 0.18f
        )
    }
    val centeredShape = remember {
        RoundedRectangle(
            cornerRadius = SleepDownDesignTokens.CenteredDialog.Corner,
            style = RoundedCornerStyle.Continuous
        )
    }
    val highlightModifier = if (enableForegroundHighlight) {
        Modifier
            .then(foregroundHighlight.gestureModifier)
            .then(foregroundHighlight.foregroundModifier)
    } else {
        Modifier
    }
    return CenteredDialogVisuals(
        surfaceModifier = Modifier
            .clip(centeredShape)
            .then(highlightModifier)
            .centeredDialogBackdropModifier(
                backdrop = completeUnderlayBackdrop,
                config = config,
                blurRadius = blurRadius,
                maxWidth = maxWidth
            ),
        backgroundModifier = Modifier.centeredDialogBackgroundBlur(
            backdrop = completeUnderlayBackdrop,
            animationProgress = animationProgress,
            dark = dark
        ),
        animationProgress = animationProgress
    )
}

@Composable
internal fun Modifier.centeredDialogBackgroundBlur(
    backdrop: Backdrop?,
    animationProgress: State<Float>,
    dark: Boolean
): Modifier {
    val blurRadius = SleepDownDesignTokens.CenteredDialog.BackgroundBlur
    val material = remember { GlassMaterialSpec.simpleBlur(blurRadius) }
    val descriptor = rememberGlassSurfaceDescriptor(
        debugLabel = "CenteredDialogBackgroundBlur",
        domain = GlassBackdropDomain.DialogBridge,
        materialRole = GlassMaterialRole.SimpleBlur,
        sceneKey = "centered-dialog-background"
    )
    val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && backdrop != null) {
        Modifier
            .graphicsLayer { alpha = animationProgress.value.coerceIn(0f, 1f) }
            .sleepDownGlassSurface(
                backdrop = backdrop,
                descriptor = descriptor,
                material = material,
                shape = { RectangleShape },
                // A full-window blur was still running at native resolution on this route.
                // Use the same sampled material path as editors; dim remains full resolution.
                effectFrame = GlassEffectFrame(blur = blurRadius),
                backdropSampleScale = 0.5f
            )
    } else {
        Modifier
    }
    val dimAlpha = if (dark) 0.24f else 0.16f
    return this
        .then(blurModifier)
        .drawWithCache {
            onDrawWithContent {
                drawContent()
                drawRect(
                    Color.Black.copy(
                        alpha = dimAlpha * animationProgress.value.coerceIn(0f, 1f)
                    )
                )
            }
        }
}

/**
 * Canonical SleepDown material for every modified Miuix centered dialog, including alerts and
 * selector/editor dialogs. Keeping this as a separate entry point prevents a centered picker from
 * silently falling back to the bottom-sheet material contract.
 */
@Composable
internal fun Modifier.centeredDialogBackdropModifier(
    backdrop: Backdrop?,
    config: ScheduleConfigEntity,
    blurRadius: Dp,
    maxWidth: Dp = SleepDownDesignTokens.CenteredDialog.Width
): Modifier = fillMaxWidth()
    .widthIn(max = maxWidth)
    .quickSheetBackdropModifier(
        backdrop = backdrop,
        config = config,
        blurRadius = blurRadius,
        centered = true
    )

/** Canonical glass/fallback surface for centered dialogs, bottom sheets and nested sheet cards. */
@Composable
internal fun Modifier.quickSheetBackdropModifier(
    backdrop: Backdrop?,
    config: ScheduleConfigEntity,
    blurRadius: Dp,
    inner: Boolean = false,
    centered: Boolean = false,
    courseCardStyle: Boolean = false
): Modifier {
    val shape = when {
        inner -> RoundedRectangle(SleepDownDesignTokens.QuickSheet.InnerCorner)
        centered -> RoundedRectangle(
            cornerRadius = SleepDownDesignTokens.CenteredDialog.Corner,
            style = RoundedCornerStyle.Continuous
        )
        else -> com.kyant.shapes.UnevenRoundedRectangle(
            topStart = SleepDownDesignTokens.QuickSheet.BottomCorner,
            topEnd = SleepDownDesignTokens.QuickSheet.BottomCorner
        )
    }
    val dark = appUsesDarkTheme(config)
    if (courseCardStyle) {
        val lightGlass = glassUsesLightStyle(config)
        val hasWallpaper = !config.wallpaperUri.isNullOrBlank()
        val quality = LocalGlassQuality.current
        val baseColor = if (lightGlass) Color.White else Color(0xFF202124)
        val rimColor = if (lightGlass) Color.Black.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.24f)
        val shapeBorderColor = if (lightGlass) Color.White.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.18f)
        val blur = (config.courseCardBlur * 0.52f).coerceIn(0f, 12f)
        val tokens = GlassMaterialSpec.courseCard(blur)
        val effectFrame = courseCardGlassEffectFrame(
            tokens = tokens,
            liveBlur = blur,
            quality = quality,
            hasWallpaper = hasWallpaper,
            refractionStrength = config.courseCardRefractionStrength
        )
        val tintStrength = if (hasWallpaper && config.courseCardOutlineLightEnabled) 0.75f else config.cardAlpha
        val tintAlpha = courseGlassTintAlpha(tintStrength, quality, hasWallpaper) *
            courseCardBrightnessAttenuation(
                config.wallpaperBrightness,
                hasWallpaper && config.courseCardOutlineLightEnabled
            )
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || backdrop == null) {
            return this
                .shadow(8.dp, shape, clip = false)
                .clip(shape)
                .background(baseColor.copy(alpha = if (hasWallpaper) 0.96f else 0.98f))
                .border(0.8.dp, rimColor, shape)
        }
        val material = tokens.copy(
            surfaceAlpha = tintAlpha,
            borderAlpha = 0.22f,
            highlightAlpha = effectFrame.highlight?.alpha ?: tokens.highlightAlpha
        )
        val descriptor = rememberGlassSurfaceDescriptor(
            debugLabel = "TodoEditorCourseGlassSheet",
            domain = GlassBackdropDomain.DialogBridge,
            materialRole = GlassMaterialRole.CourseCard,
            sceneKey = "todo-editor-course-glass-sheet"
        )
        val outlineEnabled = hasWallpaper && config.courseCardOutlineLightEnabled
        val courseSurface = sleepDownGlassSurface(
            backdrop = backdrop,
            descriptor = descriptor,
            material = material,
            shape = { shape },
            effectFrame = effectFrame,
            backdropSampleScale = 0.82f,
            cacheDecorations = true,
            effectsOverride = {
                effectFrame.blur?.let { blur(it.toPx()) }
                effectFrame.lensHeight?.let { height ->
                    effectFrame.lensAmount?.let { amount ->
                        lens(height.toPx(), amount.toPx(), chromaticAberration = effectFrame.chromaticAberration)
                    }
                }
            },
            onDrawSurface = {
                drawRect(baseColor.copy(alpha = tintAlpha))
                drawRect(
                    Color.White.copy(alpha = if (lightGlass) 0.035f else 0.055f),
                    blendMode = androidx.compose.ui.graphics.BlendMode.Screen
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.White.copy(alpha = if (lightGlass) 0.14f else 0.12f),
                        0.26f to Color.White.copy(alpha = if (lightGlass) 0.035f else 0.025f),
                        1f to Color.Transparent
                    )
                )
                if (outlineEnabled) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.White.copy(alpha = 0.12f * tintAlpha.coerceAtLeast(0.25f)),
                            0.10f to Color.Transparent,
                            0.82f to Color.Transparent,
                            1f to Color.White.copy(alpha = 0.20f * tintAlpha.coerceAtLeast(0.25f))
                        )
                    )
                }
            }
        )
        return this
            .shadow(8.dp, shape, clip = false)
            .clip(shape)
            .then(courseSurface)
            .border(0.8.dp, shapeBorderColor, shape)
            .presetCourseCardHighlight(
                shape = shape,
                alpha = (effectFrame.highlight?.alpha ?: tokens.highlightAlpha) * 0.82f,
                enabled = { true }
            )
    }
    // Centered dialogs are the SleepDown v2 neutral shell with a slightly softer blur. Bottom
    // sheets and their nested quick settings cards keep their original blur budget unchanged.
    val effectiveBlurRadius = when {
        else -> blurRadius.coerceIn(8.dp, 12.dp)
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || backdrop == null) {
        val fallback = this
            .clip(shape)
            .background(
                when {
                    inner && dark -> Color(0xFF25272C)
                    inner -> Color(0xFFF0F3F8)
                    centered && dark -> Color(0xFF1A1A1A)
                    centered -> Color(0xFFEDEEF3)
                    // This is the pre-1.2.0 non-centered sheet fallback.  Keep it local to the
                    // design-system shell so a settings-page helper is not needed in this core
                    // UI layer.
                    else -> if (dark) Color.Black else Color(0xFFEDEEF3)
                }
            )
        return fallback
    }
    val surfaceAlpha = when {
        dark -> 0.38f
        else -> 0.30f
    }
    val lensHeight = when {
        centered -> SleepDownDesignTokens.CenteredDialog.LensHeight
        inner -> 2.dp
        else -> 4.dp
    }
    val lensAmount = when {
        centered -> SleepDownDesignTokens.CenteredDialog.LensAmount
        inner -> 4.dp
        else -> 8.dp
    }
    val shellHighlightAlpha = when {
        centered && dark -> 0.14f
        centered -> 0.20f
        else -> 0.12f
    }
    val material = GlassMaterialSpec.dialog().copy(
        blur = effectiveBlurRadius,
        lensHeight = lensHeight,
        lensAmount = lensAmount,
        surfaceAlpha = surfaceAlpha,
        borderAlpha = 0f,
        highlightAlpha = shellHighlightAlpha,
        shadowAlpha = 0f,
        innerShadowAlpha = 0f,
        depthEffect = false,
        useVibrancy = false
    )
    val descriptor = rememberGlassSurfaceDescriptor(
        debugLabel = when {
            inner -> "QuickSheetInner"
            centered -> "CenteredDialog"
            else -> "QuickSheet"
        },
        domain = GlassBackdropDomain.DialogBridge,
        materialRole = GlassMaterialRole.Dialog
    )
    return sleepDownGlassSurface(
        backdrop = backdrop,
        descriptor = descriptor,
        material = material,
        shape = { shape },
        backdropSampleScale = if (centered) 0.5f else 1f,
        cacheDecorations = centered,
        effectFrame = GlassEffectFrame(
            blur = effectiveBlurRadius,
            lensHeight = lensHeight,
            lensAmount = lensAmount,
            useVibrancy = false,
            depthEffect = false,
            chromaticAberration = false,
            highlight = if (centered) {
                GlassHighlightFrame(
                    style = GlassHighlightStyle.Default,
                    alpha = shellHighlightAlpha
                )
            } else {
                null
            },
            shadowAlpha = null,
            innerShadow = null
        ),
        // The 1.2.0 QuickSheet used Kyant's native lens defaults. Preserve that exact path for
        // bottom sheets and nested sheet cards; forcing the unified frame's depthEffect=false
        // flattened the material and made the same sheet change character across preview pages.
        effectsOverride = {
            blur(effectiveBlurRadius.toPx())
            lens(
                lensHeight.toPx(),
                lensAmount.toPx(),
                chromaticAberration = false
            )
        },
        onDrawSurface = {
            drawRect(
                when {
                    dark -> Color(0xFF111318).copy(alpha = surfaceAlpha)
                    else -> Color(0xFFF8FAFD).copy(alpha = surfaceAlpha)
                }
            )
            if (!centered) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = if (inner) {
                            if (dark) {
                                listOf(Color(0xFF8CB8FF).copy(alpha = 0.12f), Color.Transparent)
                            } else {
                                listOf(Color.White.copy(alpha = 0.28f), Color.Transparent)
                            }
                        } else {
                            if (dark) {
                                listOf(Color.White.copy(alpha = 0.14f), Color.Transparent)
                            } else {
                                listOf(Color.White.copy(alpha = 0.34f), Color.Transparent)
                            }
                        },
                        center = center,
                        radius = max(size.width, size.height) * if (inner) 0.66f else 0.74f
                    )
                )
            }
        }
    )
}

@Composable
internal fun QuickSheetLiquidAction(
    label: String,
    enabled: Boolean,
    backdrop: Backdrop?,
    config: ScheduleConfigEntity,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    destructive: Boolean = false,
    accented: Boolean = false,
    height: Dp = SleepDownDesignTokens.Button.MinimumTouchSize,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val appearanceBinding = rememberGlassAppearance(config, frozen = pressed)
    val appearance = appearanceBinding.appearance
    val actionModifier = (if (modifier == Modifier) Modifier.width(84.dp) else modifier).then(appearanceBinding.modifier)
    val centeredAction = height == SleepDownDesignTokens.CenteredDialog.ActionHeight
    // A nested action must not lens the text of the settings page through its modal surface.
    if (backdrop != null && !com.xiaomanjun.sleepdownschedule.glass.ui.LocalReadablePanelControls.current) {
        val dark = !appearance.light
        val neutralSurface = if (accented) {
            Color.White.copy(alpha = 0.90f)
        } else if (dark) {
            Color(0xFF111315).copy(alpha = appearance.controlAlpha)
        } else {
            Color.White.copy(alpha = appearance.controlAlpha)
        }
        val actionSurfaceColor = when {
            primary -> SleepDownDesignTokens.Button.Primary.copy(alpha = 0.94f)
            destructive && !centeredAction -> Color(0xFFFF453A).copy(alpha = 0.88f)
            else -> neutralSurface
        }
        val actionTint = when {
            primary -> SleepDownDesignTokens.Button.Primary
            destructive && !centeredAction -> Color(0xFFFF453A)
            accented -> SleepDownDesignTokens.Button.Primary
            else -> Color.Unspecified
        }
        val actionTextColor = when {
            primary -> Color.White
            destructive && !centeredAction -> Color.White
            destructive -> Color(0xFFFF453A)
            accented -> SleepDownDesignTokens.Button.Primary
            else -> appearance.foreground
        }
        LiquidButton(
            onClick = { if (enabled) onClick() },
            backdrop = backdrop,
            modifier = actionModifier.alpha(if (enabled) 1f else .46f).semantics { if (!enabled) disabled() },
            height = height,
            blurRadius = 2.dp,
            lensHeight = 12.dp,
            lensAmount = 24.dp,
            tint = actionTint,
            surfaceColor = actionSurfaceColor,
            contentPadding = PaddingValues(horizontal = 14.dp),
            isInteractive = true,
            clickTargetEnabled = enabled,
            interactionSource = interactionSource,
            staticPressDimAlpha = 0f,
            shape = Capsule(),
            shadowEnabled = true,
            highlightEnabled = enabled
        ) {
            Text(label, color = actionTextColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    } else {
        val dark = appUsesDarkTheme(config)
        val background = when {
            destructive && !centeredAction -> Color(0xFFFF453A)
            primary -> SleepDownDesignTokens.Button.Primary
            accented -> Color.White
            centeredAction && dark -> Color(0xFF363639)
            centeredAction -> Color(0xFFE6E8EC)
            dark -> Color(0xFF30343D)
            else -> Color(0xFFE8ECF3)
        }
        val buttonTextColor = when {
            primary -> Color.White
            destructive && !centeredAction -> Color.White
            destructive -> Color(0xFFFF453A)
            accented -> SleepDownDesignTokens.Button.Primary
            else -> MaterialTheme.colorScheme.onSurface
        }
        Box(
            modifier = actionModifier
                .height(height)
                .clip(
                    Capsule()
                )
                .background(background.copy(alpha = if (enabled) 0.94f else 0.46f))
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = buttonTextColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}
