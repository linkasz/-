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
