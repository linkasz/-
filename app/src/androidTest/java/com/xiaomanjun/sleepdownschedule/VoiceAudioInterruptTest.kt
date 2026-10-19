package com.xiaomanjun.sleepdownschedule.feature.agent.voice

import android.Manifest
import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.xiaomanjun.sleepdownschedule.MainActivity
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises real recorder/track ownership on device without using a model or network. */
@RunWith(AndroidJUnit4::class)
class VoiceAudioInterruptTest {
    @Test
    fun repeatedStartPlaybackInterruptAndReleaseKeepsNewPlaybackAlive() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val uiAutomation = instrumentation.uiAutomation
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scopeJob = SupervisorJob()
        val scope = CoroutineScope(scopeJob + Dispatchers.Main.immediate)
        val audio = VoiceAudio(context, scope)
        val microphoneFailures = AtomicInteger()
        val capturedFrames = AtomicInteger()
        val playbackFailures = AtomicInteger()
        val silence = ByteArray(24_000 / 5 * 2) // 200ms, mono PCM16 at 24kHz.

        try {
            uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.RECORD_AUDIO)
            ActivityScenario.launch(MainActivity::class.java).use {
                instrumentation.waitForIdleSync()
                runBlocking(Dispatchers.Main) {
                    repeat(30) {
                        audio.start(
                            rate = 16_000,
                            onFrame = { _, _ -> capturedFrames.incrementAndGet() },
                            onLostFocus = {},
                            onFailure = { microphoneFailures.incrementAndGet() }
                        )
                        delay(60)

                        val interruptedToken = audio.playbackToken
                        audio.play(silence, { playbackFailures.incrementAndGet() }, interruptedToken)
                        audio.interrupt()
                        audio.play(silence, { playbackFailures.incrementAndGet() }, interruptedToken)

                        val currentToken = audio.playbackToken
                        assertTrue("interrupt must invalidate the prior playback token", currentToken > interruptedToken)
                        audio.play(silence, { playbackFailures.incrementAndGet() }, currentToken)
                        audio.finishPlayback(currentToken)
                        assertEquals("stale-token PCM must not enter the new playback", silence.size.toLong(), audio.playedBytes)

                        audio.release()
                    }
                }
                assertTrue("the recorder must deliver frames", capturedFrames.get() > 0)
                assertEquals("recorder read failures", 0, microphoneFailures.get())
                assertEquals("playback failures", 0, playbackFailures.get())
            }
        } finally {
            runBlocking(Dispatchers.Main) {
                audio.release()
                scopeJob.cancelAndJoin()
            }
            // Revoking a runtime permission kills the instrumented process on Android.
            // This test runs only in the designated emulator; leave its permission intact.
        }
    }
}
