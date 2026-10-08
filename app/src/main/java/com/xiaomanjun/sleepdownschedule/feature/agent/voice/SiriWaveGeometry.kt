package com.xiaomanjun.sleepdownschedule.feature.agent.voice

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin

/** Native adaptation of SiriWave iOS9Curve's attenuated, mirrored multi-curve algorithm.
 * https://github.com/kopiro/siriwave (MIT, Copyright 2020 Flavio Maria De Stefano).
 * Microphone/playback RMS is the amplitude input; silence produces a flat line, not fake speech.
 * Uses fixed lanes and reusable paths instead of the JS implementation's random spawn timers.
 */
internal fun siriWaveHeight(x: Float, time: Float, lane: Int, amplitude: Float): Float {
    val shifted = x * 3f - (lane - 1) * .65f
    val envelope = (4f / (4f + shifted * shifted)).pow(4)
    return abs(sin(shifted * 2f - time * (1.8f + lane * .3f))) * envelope * amplitude.coerceIn(0f, 1f)
}

/** Final callbacks without stable item IDs can be duplicated across vendor ASR transports. */
internal class VoiceFinalDeduplicator(private val clock: () -> Long) {
    private var text = ""
    private var at = Long.MIN_VALUE
    fun accept(value: String): Boolean {
        val normalized = value.trim().replace(Regex("[\\s，。！？,.!?]+"), "")
        if (normalized.isEmpty()) return false
        val now = clock()
        if (normalized == text && now - at in 0 until 5_000) return false
        text = normalized; at = now
        return true
    }
    fun clear() { text = ""; at = Long.MIN_VALUE }
}
