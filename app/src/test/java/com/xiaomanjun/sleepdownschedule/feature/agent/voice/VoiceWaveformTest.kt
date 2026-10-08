package com.xiaomanjun.sleepdownschedule.feature.agent.voice

import org.junit.Assert.*
import org.junit.Test

class VoiceWaveformTest {
    @Test fun silenceIsFlatAndMeasuredAudioDrivesAmplitude() {
        assertEquals(0f, siriWaveHeight(.2f, .8f, 1, 0f), 0f)
        val low = siriWaveHeight(.2f, .8f, 1, .2f)
        assertTrue(low > 0f)
        assertEquals(low * 4f, siriWaveHeight(.2f, .8f, 1, .8f), .001f)
        assertTrue(siriWaveHeight(8f, .8f, 1, 1f) < low)
    }
    @Test fun vendorFinalCallbacksMergeWithinFiveSecondsAndAllowNewIntent() {
        var now = 100L
        val gate = VoiceFinalDeduplicator { now }
        assertTrue(gate.accept("今天天气？"))
        assertFalse(gate.accept("今天 天气"))
        assertTrue(gate.accept("明天天气"))
        assertFalse(gate.accept("明天天气"))
        now += 5_000
        assertTrue(gate.accept("明天天气"))
        gate.clear(); assertTrue(gate.accept("明天天气"))
    }
    @Test fun queryStateCanBeInterrupted() {
        assertEquals(CompactVoiceMicAction.INTERRUPT, compactVoiceMicAction(VoicePhase.QUERYING))
    }
}
