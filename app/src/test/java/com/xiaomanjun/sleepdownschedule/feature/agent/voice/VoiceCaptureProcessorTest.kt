package com.xiaomanjun.sleepdownschedule.feature.agent.voice

import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt
import org.junit.Assert.*
import org.junit.Test

class VoiceCaptureProcessorTest {
    @Test fun removesDcAndAttenuatesRumbleAtBothCaptureRates() {
        for (rate in listOf(16_000, 24_000)) {
            val dc = pcm(DoubleArray(rate) { .2 })
            VoiceCaptureProcessor(rate, true).process(dc)
            assertTrue(rms(dc, rate / 2) < .0001)
            val rumble = tone(rate, 20.0, .1)
            VoiceCaptureProcessor(rate, true).process(rumble)
            assertTrue(rms(rumble, rate / 2) < .02)
        }
    }

    @Test fun speechBandRemainsAudibleWithoutBoostingOrClipping() {
        for (rate in listOf(16_000, 24_000)) {
            val speech = tone(rate, 1000.0, .6)
            VoiceCaptureProcessor(rate, false).process(speech)
            assertTrue(rms(speech, rate / 2) in 0.39..0.43)
            assertTrue(samples(speech).all { it in -32000..32000 })
        }
    }

    @Test fun quietSteadyNoiseIsSoftenedButNeverHardMuted() {
        val quiet = tone(16_000, 1000.0, .001)
        val platform = quiet.copyOf()
        VoiceCaptureProcessor(16_000, false).process(quiet)
        VoiceCaptureProcessor(16_000, true).process(platform)
        val reduced = rms(quiet, 8000)
        assertTrue(reduced > 0)
        assertTrue(reduced < rms(platform, 8000) * .4)
        assertTrue(rms(platform, 8000) > .0006)
    }

    @Test fun speechOnsetOpensQuicklyAfterQuietBackground() {
        val processor = VoiceCaptureProcessor(16_000, false)
        processor.process(tone(16_000, 1000.0, .001))
        val speech = tone(16_000, 1000.0, .15)
        processor.process(speech)
        // By 30ms, the 10ms detector plus 5ms attack preserves the next speech syllable.
        assertTrue(rms(speech.copyOfRange(960, 3200)) > .095)
    }

    @Test fun arbitraryReadBoundariesDoNotChangeFilterOrEnvelope() {
        for (rate in listOf(16_000, 24_000)) {
            val input = pcm(DoubleArray(rate) { i ->
                val amplitude = if (i < rate / 3 || i > rate * 2 / 3) .001 else .15
                amplitude * sin(2 * PI * 700 * i / rate) + .02
            })
            val whole = input.copyOf()
            VoiceCaptureProcessor(rate, false).process(whole)
            val chunks = input.copyOf()
            val streaming = VoiceCaptureProcessor(rate, false)
            var offset = 0
            while (offset < chunks.size) {
                val end = minOf(offset + 274, chunks.size)
                val frame = chunks.copyOfRange(offset, end)
                streaming.process(frame)
                frame.copyInto(chunks, offset)
                offset = end
            }
            assertArrayEquals(whole, chunks)
        }
    }

    private fun tone(rate: Int, frequency: Double, amplitude: Double) =
        pcm(DoubleArray(rate) { amplitude * sin(2 * PI * frequency * it / rate) })

    private fun pcm(values: DoubleArray) = ByteArray(values.size * 2).also { out ->
        values.forEachIndexed { i, value ->
            val sample = (value * 32767).toInt().coerceIn(-32768, 32767)
            out[i * 2] = sample.toByte()
            out[i * 2 + 1] = (sample shr 8).toByte()
        }
    }

    private fun samples(pcm: ByteArray) = (pcm.indices step 2).map { i ->
        ((pcm[i].toInt() and 255) or (pcm[i + 1].toInt() shl 8)).toShort().toInt()
    }

    private fun rms(pcm: ByteArray, skipSamples: Int = 0): Double =
        samples(pcm).drop(skipSamples).map { it / 32768.0 }.let { values ->
            sqrt(values.sumOf { it * it } / values.size)
        }
}
