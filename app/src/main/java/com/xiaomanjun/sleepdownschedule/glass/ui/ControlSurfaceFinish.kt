package com.xiaomanjun.sleepdownschedule.glass.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke

/** A quiet bevel on the existing surface; no extra blur, offscreen layer or input handler. */
internal fun Modifier.controlSurfaceFinish(shape: Shape, light: Boolean, press: () -> Float): Modifier = drawWithCache {
    val rim = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
    val shine = Brush.linearGradient(
        0f to Color.White.copy(alpha = if (light) .42f else .48f),
        .38f to Color.White.copy(alpha = .08f),
        .62f to Color.Transparent,
        1f to (if (light) Color.Black.copy(alpha = .10f) else Color.White.copy(alpha = .10f)),
        start = Offset.Zero, end = Offset(size.width * .35f, size.height)
    )
    val stroke = Stroke(.7f * density)
    onDrawWithContent {
        drawContent()
        drawPath(rim, shine, alpha = .65f + .25f * press().coerceIn(0f, 1f), style = stroke)
    }
}
