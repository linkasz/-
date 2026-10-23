package com.xiaomanjun.sleepdownschedule.feature.agent.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotSame
import org.junit.Test

class VoiceBargeInBufferTest {
    @Test fun requiresSustainedLevelAndReturnsBoundedPreRoll() {
        val detector = VoiceBargeInBuffer(threshold = .07f, requiredVoicedFrames = 3, maxFrames = 4)
        val source = (0..4).map { byteArrayOf(it.toByte()) }

        assertNull(detector.feed(source[0], .2f))
        assertNull(detector.feed(source[1], .01f))
        assertNull(detector.feed(source[2], .2f))
        assertNull(detector.feed(source[3], .2f))
        val captured = detector.feed(source[4], .2f)

        assertEquals(listOf(1, 2, 3, 4), captured?.map { it.pcm[0].toInt() })
        assertEquals(listOf(.01f, .2f, .2f, .2f), captured?.map { it.level })
        assertNotSame(source[1], captured?.first()?.pcm)
    }

    @Test fun resetDropsPreRollAndConsecutiveSpeechCount() {
        val detector = VoiceBargeInBuffer(requiredVoicedFrames = 2)
        detector.feed(byteArrayOf(1), .2f)
        detector.reset()

        assertNull(detector.feed(byteArrayOf(2), .2f))
        val captured = detector.feed(byteArrayOf(3), .2f)
        assertEquals(listOf(2, 3), captured?.map { it.pcm[0].toInt() })
    }
}
