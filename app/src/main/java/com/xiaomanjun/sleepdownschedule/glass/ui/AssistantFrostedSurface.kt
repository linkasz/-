package com.xiaomanjun.sleepdownschedule.glass.ui

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.runtimeShaderEffect
import com.xiaomanjun.sleepdownschedule.ScheduleConfigEntity
import com.xiaomanjun.sleepdownschedule.core.performance.LocalGlassQuality
import com.xiaomanjun.sleepdownschedule.glass.GlassBackdropDomain
import com.xiaomanjun.sleepdownschedule.glass.GlassEffectFrame
import com.xiaomanjun.sleepdownschedule.glass.GlassMaterialRole
import com.xiaomanjun.sleepdownschedule.glass.GlassMorphAllocation
import com.xiaomanjun.sleepdownschedule.glass.rememberGlassSurfaceDescriptor
import com.xiaomanjun.sleepdownschedule.glass.sleepDownGlassSurface

/** A masked background consumer above the liquid shell, below all text and controls.
 * Both consumers sample the same underlay; neither records or samples the other.
 */
@Composable
internal fun AssistantFrostedSurface(backdrop: Backdrop?, config: ScheduleConfigEntity,
    shape: Shape, light: Boolean, modifier: Modifier,
    morphAllocation: GlassMorphAllocation?, shapeProvider: (() -> Shape)?) {
    val base = if (light) Color.White else Color.Black
    val tint = if (light) .78f else .72f
    val quality = LocalGlassQuality.current
    val radius = 18.dp * quality
    val material = remember(radius) { GlassTokens.simpleBlur(radius).copy(
        highlightAlpha = 0f, shadowAlpha = 0f, innerShadowAlpha = 0f) }
    val descriptor = rememberGlassSurfaceDescriptor("AssistantFrost", GlassBackdropDomain.ChromeCombined,
        GlassMaterialRole.SimpleBlur)
    if (Build.VERSION.SDK_INT >= 33 && backdrop != null) {
        val effects: BackdropEffectScope.() -> Unit = remember(radius, base, tint) { {
            blur(radius.toPx())
            runtimeShaderEffect("AssistantFrostMask", AssistantFrostMask, "content") {
                setFloatUniform("contentOrigin", padding, padding)
                setFloatUniform("contentHeight", size.height.coerceAtLeast(1f))
                setColorUniform("tint", base)
                setFloatUniform("tintAlpha", tint)
            }
        } }
        Box(modifier.sleepDownGlassSurface(backdrop, descriptor, material,
            shapeProvider ?: { shape }, GlassEffectFrame(blur = radius), effectsOverride = effects,
            renderBounds = { morphAllocation?.localBounds() }, allocationPaddingPx = morphAllocation?.paddingPx))
    } else if (Build.VERSION.SDK_INT >= 31 && backdrop != null) {
        // API 31/32 has RenderEffect blur but no RuntimeShader. Isolate DstIn so the mask
        // cannot erase the liquid layer or the page. This temporary layer contains no UI.
        GlassSurface(backdrop, config, modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val bounds = morphAllocation?.localBounds() ?: Rect(Offset.Zero, size)
                drawRect(Brush.verticalGradient(0f to Color.White, .55f to Color.White,
                    (2f / 3f) to Color.Transparent, 1f to Color.Transparent,
                    startY = bounds.top, endY = bounds.bottom), blendMode = BlendMode.DstIn)
            }, shape = shape, shapeProvider = shapeProvider, morphAllocation = morphAllocation,
            tokens = material.copy(surfaceAlpha = tint), baseSurfaceColorOverride = base) {}
    } else {
        // Unsupported devices use a readable gradient; do not pretend they have refraction.
        Box(modifier.drawWithCache {
            val bounds = morphAllocation?.localBounds() ?: Rect(Offset.Zero, size)
            val tintBrush = Brush.verticalGradient(0f to base.copy(alpha = .94f),
                .55f to base.copy(alpha = .94f), (2f / 3f) to Color.Transparent, 1f to Color.Transparent,
                startY = bounds.top, endY = bounds.bottom)
            onDrawBehind { drawRect(tintBrush, bounds.topLeft, bounds.size) }
        })
    }
}

// Gaussian blur is provided by AndroidLiquidGlass's RenderEffect path. This shader only
// feathers that frosted sample into the independently refracted shell at the 2/3 boundary.
private const val AssistantFrostMask = """
uniform shader content;
uniform float2 contentOrigin;
uniform float contentHeight;
layout(color) uniform half4 tint;
uniform float tintAlpha;
half4 main(float2 coord) {
    float y = clamp((coord.y - contentOrigin.y) / contentHeight, 0.0, 1.0);
    float mask = 1.0 - smoothstep(0.55, 0.6666667, y);
    half4 sample = content.eval(coord);
    return (tint * tintAlpha + sample * (1.0 - tintAlpha)) * mask;
}
"""
