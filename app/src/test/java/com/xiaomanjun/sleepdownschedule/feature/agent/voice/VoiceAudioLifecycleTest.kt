package com.xiaomanjun.sleepdownschedule.feature.agent.voice

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class VoiceAudioLifecycleTest {
    @Test fun recorderReleaseWaitsUntilCancelledBlockingReadOwnerExits() = runBlocking {
        val readGate = CompletableDeferred<Unit>()
        val reader = launch(start = CoroutineStart.UNDISPATCHED) {
            withContext(NonCancellable) { readGate.await() }
        }
        var releaseCount = 0

        reader.cancel()
        releaseAfterVoiceJob(reader) { releaseCount++ }
        assertEquals(0, releaseCount)

        readGate.complete(Unit)
        reader.join()
        assertEquals(1, releaseCount)
        assertTrue(reader.isCompleted)
    }

    @Test fun releaseWithoutReaderIsImmediate() {
        var released = false

        releaseAfterVoiceJob(null) { released = true }

        assertTrue(released)
    }

    @Test fun cleanupMarkerWaitsForReleaseEvenWhenReaderAlreadyReportsCompleted() {
        val reader = CompletableDeferred<Unit>()
        val releaseStarted = CountDownLatch(1)
        val allowRelease = CountDownLatch(1)
        val cleanup = releaseAfterVoiceJob(reader) {
            releaseStarted.countDown()
            check(allowRelease.await(5, TimeUnit.SECONDS))
        }
        val completer = Thread { reader.complete(Unit) }
        completer.start()
        try {
            assertTrue(releaseStarted.await(5, TimeUnit.SECONDS))
            assertTrue(reader.isCompleted)
            assertFalse("Reopen must wait for the resource release callback", cleanup.isCompleted)
        } finally {
            allowRelease.countDown()
            completer.join(5_000)
        }
        assertFalse(completer.isAlive)
        assertTrue(cleanup.isCompleted)
    }
}
