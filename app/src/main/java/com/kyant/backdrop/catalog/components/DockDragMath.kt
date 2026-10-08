// Based on Kyant0/AndroidLiquidGlass catalog components, Apache-2.0.
// Modified for SleepDown-Schedule.
package com.kyant.backdrop.catalog.components

import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal data class DockSpringState(
    val value: Float,
    val velocity: Float
)

/** Advances a critically damped spring toward the newest target for one display frame. */
internal fun dockSpringStep(
    value: Float,
    velocity: Float,
    target: Float,
    deltaSeconds: Float,
    stiffness: Float = 1000f
): DockSpringState {
    if (!deltaSeconds.isFinite() || deltaSeconds <= 0f || !stiffness.isFinite() || stiffness <= 0f) {
        return DockSpringState(value, velocity)
    }

    // Capping a long frame prevents a pause or debugger stop from turning into a visible jump.
    val delta = deltaSeconds.coerceAtMost(0.032f)
    val angularFrequency = sqrt(stiffness)
    val displacement = value - target
    val velocityTerm = velocity + angularFrequency * displacement
    val decay = exp(-angularFrequency * delta)
    val nextDisplacement = (displacement + velocityTerm * delta) * decay
    val nextVelocity = (velocity - angularFrequency * velocityTerm * delta) * decay
    return DockSpringState(target + nextDisplacement, nextVelocity)
}

internal fun dockDragTarget(
    currentTarget: Float,
    dragAmountPx: Float,
    tabWidthPx: Float,
    isLtr: Boolean,
    tabsCount: Int
): Float {
    require(tabsCount > 0)
    if (tabWidthPx <= 0f || !tabWidthPx.isFinite()) {
        return currentTarget.coerceIn(0f, (tabsCount - 1).toFloat())
    }
    val direction = if (isLtr) 1f else -1f
    return (currentTarget + dragAmountPx / tabWidthPx * direction)
        .coerceIn(0f, (tabsCount - 1).toFloat())
}

internal fun dockSettledIndex(targetValue: Float, tabsCount: Int): Int {
    require(tabsCount > 0)
    return targetValue.roundToInt().coerceIn(0, tabsCount - 1)
}
