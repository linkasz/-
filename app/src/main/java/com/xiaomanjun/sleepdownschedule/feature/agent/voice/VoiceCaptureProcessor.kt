package com.xiaomanjun.sleepdownschedule.feature.agent.voice

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sqrt

/** Per-recording PCM16 state. This attenuates rumble/quiet background, not acoustic echo. */
internal class VoiceCaptureProcessor(sampleRate: Int, private val platformNoiseSuppression: Boolean) {
    init { require(sampleRate in 8_000..48_000) }

    private val highPass = exp(-2.0 * PI * 80.0 / sampleRate)
    private val gainAttack = exp(-1.0 / (.005 * sampleRate))
    private val gainRelease = exp(-1.0 / (.120 * sampleRate))
    private val windowSamples = sampleRate / 100
    private var previousInput = 0.0
    private var previousOutput = 0.0
    private var energy = 0.0
    private var samples = 0
    private var noiseFloor = .0015
    private var targetGain = 1.0
    private var gain = 1.0

    /** Mutates the caller-owned frame; windows persist across AudioRecord read boundaries. */
    fun process(frame: ByteArray) {
        require(frame.size % 2 == 0) { "PCM16 frame must contain complete samples" }
        for (index in frame.indices step 2) {
            val input = ((frame[index].toInt() and 255) or (frame[index + 1].toInt() shl 8)).toShort() / 32768.0
            val filtered = highPass * (previousOutput + input - previousInput)
            previousInput = input
            previousOutput = filtered
            energy += filtered * filtered
            if (++samples == windowSamples) {
                val rms = sqrt(energy / samples)
                // Only learn during quiet windows; sustained speech cannot become the noise floor.
                if (rms < noiseFloor * 1.6) {
                    val adaptation = if (rms < noiseFloor) .15 else .02
                    noiseFloor += adaptation * (rms - noiseFloor)
                    noiseFloor = noiseFloor.coerceIn(.0003, .004)
                }
                if (!platformNoiseSuppression) {
                    val speech = ((rms / noiseFloor - 1.5) / 2.0).coerceIn(0.0, 1.0)
                    targetGain = .25 + .75 * speech * speech * (3.0 - 2.0 * speech)
                }
                energy = 0.0
                samples = 0
            }
            // Fast opening preserves speech onset; slow closing avoids a hard gate between syllables.
            val smoothing = if (targetGain > gain) gainAttack else gainRelease
            gain = targetGain + smoothing * (gain - targetGain)
            val output = (filtered * gain * 32768.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            frame[index] = output.toByte()
            frame[index + 1] = (output shr 8).toByte()
        }
    }
}
