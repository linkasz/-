package com.xiaomanjun.sleepdownschedule.feature.agent.voice

import org.junit.Assert.*
import org.junit.Test

class ManualVoiceTurnTest {
    private val frameBytes = 16000 / 25 * 2 // 40ms at 16kHz, matching AudioRecord.

    @Test fun realSpeechCommitsExactlyOnceAfterSixHundredMillisecondsOfSilence() {
        val detector = ManualVoiceTurn(16000)
        assertTrue(detector.feed(.2f, frameBytes).started)
        repeat(4) { assertFalse(detector.feed(.2f, frameBytes).started) }
        repeat(14) { assertFalse(detector.feed(0f, frameBytes).commit) }
        assertTrue(detector.feed(0f, frameBytes).commit)
        repeat(25) { assertFalse(detector.feed(0f, frameBytes).commit) }
    }
    @Test fun silenceAndBriefNoiseNeverSubmitAnEmptyUtterance() {
        val detector = ManualVoiceTurn(16000)
        repeat(49) { assertFalse(detector.feed(0f, frameBytes).clear) }
        assertTrue(detector.feed(0f, frameBytes).clear)
        detector.feed(.3f, frameBytes)
        repeat(14) { assertFalse(detector.feed(0f, frameBytes).commit) }
        val end = detector.feed(0f, frameBytes)
        assertTrue(end.clear); assertFalse(end.commit)
    }
    @Test fun durationUsesActualPcmSizeAndSupportsContinuousTurns() {
        val detector = ManualVoiceTurn(16000)
        repeat(2) {
            assertTrue(detector.feed(.2f, 16000 * 2 / 5).started) // 200ms, not a fixed frame count.
            assertTrue(detector.feed(0f, 16000 * 2 * 600 / 1000).commit)
        }
    }
    @Test fun twentySecondLimitAndResetBoundServerBuffers() {
        val detector = ManualVoiceTurn(16000)
        repeat(499) { assertFalse(detector.feed(.2f, frameBytes).commit) }
        assertTrue(detector.feed(.2f, frameBytes).commit)
        detector.feed(.2f, frameBytes); detector.reset()
        repeat(15) { assertFalse(detector.feed(0f, frameBytes).commit) }
        assertTrue(detector.feed(.2f, frameBytes).started)
    }
}
