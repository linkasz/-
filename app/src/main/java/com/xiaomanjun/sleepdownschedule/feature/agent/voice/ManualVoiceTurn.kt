package com.xiaomanjun.sleepdownschedule.feature.agent.voice

/** PCM-duration based end-of-turn detection for Aliyun Omni manual mode.
 * Audio is streamed continuously; commit only a voiced utterance. Idle buffers are
 * cleared every two seconds. No timer-generated waveform and no unbounded silence.
 */
internal class ManualVoiceTurn(private val sampleRate: Int) {
    data class Action(val started: Boolean = false, val commit: Boolean = false, val clear: Boolean = false)
    private var bufferedSamples = 0
    private var voicedSamples = 0
    private var quietSamples = 0
    private var speaking = false

    @Synchronized fun reset() {
        bufferedSamples = 0; voicedSamples = 0; quietSamples = 0; speaking = false
    }

    @Synchronized fun feed(level: Float, pcmBytes: Int): Action {
        val samples = pcmBytes / 2
        bufferedSamples += samples
        val started = level > .07f && !speaking
        if (level > .07f) {
            speaking = true; voicedSamples += samples; quietSamples = 0
        } else if (speaking) quietSamples += samples
        val ended = speaking && (quietSamples >= sampleRate * 600 / 1000 || bufferedSamples >= sampleRate * 20)
        if (ended) {
            val commit = voicedSamples >= sampleRate / 5
            reset()
            return Action(started, commit, clear = !commit)
        }
        if (!speaking && bufferedSamples >= sampleRate * 2) {
            reset(); return Action(clear = true)
        }
        return Action(started = started)
    }
}

/** Bounded input pre-roll for interrupting playback without forwarding speaker echo. */
internal class VoiceBargeInBuffer(
    private val threshold: Float = .07f,
    private val requiredVoicedFrames: Int = 3,
    private val maxFrames: Int = 5
) {
    data class CapturedFrame(val pcm: ByteArray, val level: Float)
    private val frames = ArrayDeque<CapturedFrame>()
    private var voicedFrames = 0

    @Synchronized fun reset() {
        frames.clear()
        voicedFrames = 0
    }

    /** Returns pre-roll once sustained speech crosses threshold; retained memory is frame-bounded. */
    @Synchronized fun feed(frame: ByteArray, level: Float): List<CapturedFrame>? {
        frames.addLast(CapturedFrame(frame.copyOf(), level))
        while (frames.size > maxFrames) frames.removeFirst()
        voicedFrames = if (level > threshold) voicedFrames + 1 else 0
        if (voicedFrames < requiredVoicedFrames) return null
        return frames.toList().also { reset() }
    }
}
