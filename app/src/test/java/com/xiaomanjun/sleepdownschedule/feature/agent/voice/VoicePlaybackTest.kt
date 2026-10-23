package com.xiaomanjun.sleepdownschedule.feature.agent.voice

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class VoicePlaybackTest {
    @Test fun fragmentedSamplesAndBackpressurePreservePcmOrder() = runBlocking {
        val output = java.io.ByteArrayOutputStream()
        val (playback, scope) = playback {
            object : VoicePlaybackSink {
                var backpressure = true
                override fun start() = Unit
                override fun write(bytes: ByteArray, offset: Int, length: Int): Int {
                    if (backpressure) { backpressure = false; return 0 }
                    val count = minOf(2, length)
                    output.write(bytes, offset, count)
                    return count
                }
                override val playbackHeadPositionSamples get() = Long.MAX_VALUE
                override fun close() = Unit
            }
        }
        try {
            playback.play(byteArrayOf(1), {})
            playback.play(byteArrayOf(2, 3, 4), {})
            playback.play(byteArrayOf(5), {})
            playback.play(byteArrayOf(6), {})
            playback.finishPlayback()
            org.junit.Assert.assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5, 6), output.toByteArray())
            assertEquals(6L, playback.playedBytes)
        } finally { playback.interrupt(); scope.cancel() }
    }

    @Test fun stalledDrainReportsFailureAndClosesOwner() = runBlocking {
        val failed = CompletableDeferred<Unit>()
        val closed = CompletableDeferred<Unit>()
        val (playback, scope) = playback {
            object : VoicePlaybackSink {
                override fun start() = Unit
                override fun write(bytes: ByteArray, offset: Int, length: Int) = length
                override val playbackHeadPositionSamples get() = 0L
                override fun close() { closed.complete(Unit) }
            }
        }
        try {
            playback.play(byteArrayOf(1, 2), { failed.complete(Unit) })
            playback.finishPlayback()
            withTimeout(5_000) { failed.await(); closed.await() }
        } finally { playback.interrupt(); scope.cancel() }
    }

    @Test fun nextOwnerWaitsUntilCancelledNativeOwnerCloses() = runBlocking {
        val writeEntered = CountDownLatch(1)
        val releaseWrite = CountDownLatch(1)
        val closeEntered = CountDownLatch(1)
        val releaseClose = CountDownLatch(1)
        val firstClosed = CompletableDeferred<Unit>()
        val created = AtomicInteger()
        val (playback, scope) = playback {
            val id = created.incrementAndGet()
            object : VoicePlaybackSink {
                override fun start() = Unit
                override fun write(bytes: ByteArray, offset: Int, length: Int): Int {
                    if (id == 1) {
                        writeEntered.countDown()
                        check(releaseWrite.await(5, TimeUnit.SECONDS))
                    }
                    return length
                }
                override val playbackHeadPositionSamples get() = Long.MAX_VALUE
                override fun close() {
                    if (id == 1) {
                        closeEntered.countDown()
                        check(releaseClose.await(5, TimeUnit.SECONDS))
                        firstClosed.complete(Unit)
                    }
                }
            }
        }

        try {
            playback.play(byteArrayOf(1, 2), {}, 0)
            assertTrue(writeEntered.await(5, TimeUnit.SECONDS))
            playback.interrupt()
            val newToken = playback.playbackToken
            playback.play(byteArrayOf(3, 4), {}, newToken)
            delay(30)
            assertEquals("new sink waits for old write owner", 1, created.get())

            releaseWrite.countDown()
            assertTrue(closeEntered.await(5, TimeUnit.SECONDS))
            delay(30)
            assertEquals("new sink waits for old close to return", 1, created.get())
            releaseClose.countDown()
            withTimeout(5_000) { firstClosed.await() }
            playback.finishPlayback(newToken)
            assertEquals(2, created.get())
        } finally {
            releaseWrite.countDown()
            releaseClose.countDown()
            scope.cancel()
        }
    }

    @Test fun repeatedInterruptAndPlayRejectsEveryStaleToken() = runBlocking {
        val writes = AtomicInteger()
        val (playback, scope) = playback {
            object : VoicePlaybackSink {
                override fun start() = Unit
                override fun write(bytes: ByteArray, offset: Int, length: Int): Int {
                    writes.incrementAndGet()
                    return length
                }
                override val playbackHeadPositionSamples get() = Long.MAX_VALUE
                override fun close() = Unit
            }
        }

        try {
            repeat(50) {
                val oldToken = playback.playbackToken
                playback.interrupt()
                playback.play(byteArrayOf(1, 2), {}, oldToken)
                val currentToken = playback.playbackToken
                playback.play(byteArrayOf(3, 4), {}, currentToken)
                playback.finishPlayback(currentToken)
            }

            assertEquals(50, writes.get())
        } finally {
            scope.cancel()
        }
    }

    @Test fun failedWorkerClosesQueueAndReportsOnce() = runBlocking {
        val failureCount = AtomicInteger()
        val writeGate = CountDownLatch(1)
        val (playback, scope) = playback(queueCapacity = 1) {
            object : VoicePlaybackSink {
                override fun start() = Unit
                override fun write(bytes: ByteArray, offset: Int, length: Int): Int {
                    check(writeGate.await(5, TimeUnit.SECONDS))
                    return -1
                }
                override val playbackHeadPositionSamples get() = 0L
                override fun close() = Unit
            }
        }

        try {
            playback.play(byteArrayOf(1, 2), { failureCount.incrementAndGet() }, 0)
            playback.play(byteArrayOf(3, 4), { failureCount.incrementAndGet() }, 0)
            val blockedSender = async {
                playback.play(byteArrayOf(5, 6), { failureCount.incrementAndGet() }, 0)
            }
            delay(20)
            assertFalse("sender should be waiting on the full queue", blockedSender.isCompleted)
            writeGate.countDown()
            withTimeout(5_000) { blockedSender.await() }
            withTimeout(5_000) {
                while (failureCount.get() == 0) delay(1)
            }
            assertEquals(1, failureCount.get())
        } finally {
            writeGate.countDown()
            playback.interrupt()
            scope.cancel()
        }
    }

    @Test fun cancellationDoesNotReportPlaybackFailure() = runBlocking {
        val failureCount = AtomicInteger()
        val writeEntered = CompletableDeferred<Unit>()
        val (playback, scope) = playback {
            object : VoicePlaybackSink {
                override fun start() = Unit
                override fun write(bytes: ByteArray, offset: Int, length: Int): Int {
                    writeEntered.complete(Unit)
                    Thread.sleep(5)
                    return length
                }
                override val playbackHeadPositionSamples get() = Long.MAX_VALUE
                override fun close() = Unit
            }
        }

        try {
            playback.play(byteArrayOf(1, 2), { failureCount.incrementAndGet() }, 0)
            withTimeout(5_000) { writeEntered.await() }
            playback.interrupt()
            delay(20)
            assertEquals(0, failureCount.get())
            assertEquals(1, playback.playbackToken)
        } finally {
            playback.interrupt()
            scope.cancel()
        }
    }

    private fun playback(
        queueCapacity: Int = 64,
        create: () -> VoicePlaybackSink,
    ): Pair<VoicePlayback, CoroutineScope> {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        return VoicePlayback(
            scope = scope,
            createSink = create,
            workerDispatcher = Dispatchers.IO,
            callbackDispatcher = Dispatchers.Unconfined,
            queueCapacity = queueCapacity,
            drainTimeoutMillis = 100,
        ) to scope
    }
}
