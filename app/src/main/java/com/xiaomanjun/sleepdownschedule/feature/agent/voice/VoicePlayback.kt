package com.xiaomanjun.sleepdownschedule.feature.agent.voice

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

internal interface VoicePlaybackSink {
    fun start()
    fun write(bytes: ByteArray, offset: Int, length: Int): Int
    val playbackHeadPositionSamples: Long
    fun close()
}

/** Serializes native playback ownership across interruptions and reopen attempts. */
internal class VoicePlayback(
    private val scope: CoroutineScope,
    private val createSink: () -> VoicePlaybackSink,
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val callbackDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val queueCapacity: Int = 64,
    private val zeroWriteDelayMillis: Long = 2,
    private val drainTimeoutMillis: Long = 60_000,
) {
    private class Generation(val token: Int, val queue: Channel<ByteArray>) {
        var job: Job? = null
    }

    private val generation = AtomicInteger(0)
    private data class Played(val token: Int, val bytes: Long)
    private val played = AtomicReference(Played(0, 0))
    private val current = AtomicReference<Generation?>(null)
    private val ownership = Mutex()

    val playbackToken: Int get() = generation.get()
    val playedBytes: Long get() = played.get().takeIf { it.token == generation.get() }?.bytes ?: 0

    suspend fun play(pcm: ByteArray, onFailure: () -> Unit, expectedToken: Int = playbackToken) {
        val active = withContext(callbackDispatcher) {
            if (expectedToken != generation.get()) return@withContext null
            val existing = current.get()
            if (existing?.token == expectedToken && existing.job?.isActive == true) {
                existing
            } else {
                val next = Generation(expectedToken, Channel(queueCapacity))
                val worker = scope.launch(workerDispatcher, start = CoroutineStart.LAZY) {
                    runGeneration(next, onFailure)
                }
                next.job = worker
                current.set(next)
                worker.start()
                next
            }
        } ?: return

        try {
            active.queue.send(pcm)
        } catch (_: ClosedSendChannelException) {
            // The owning worker already reports its failure, or interruption invalidated it.
        } catch (cancelled: CancellationException) {
            // Cancelling the owner's queue releases blocked senders without cancelling their caller.
            currentCoroutineContext().ensureActive()
        }
    }

    suspend fun finishPlayback(expectedToken: Int = playbackToken) {
        val job = withContext(callbackDispatcher) {
            if (expectedToken != generation.get()) return@withContext null
            current.get()?.takeIf { it.token == expectedToken }?.also { it.queue.close() }?.job
        }
        job?.join()
    }

    fun interrupt() {
        generation.incrementAndGet()
        current.getAndSet(null)?.let { owner ->
            owner.queue.cancel()
            owner.job?.cancel()
        }
    }

    private suspend fun runGeneration(owner: Generation, onFailure: () -> Unit) {
        var bytesWritten = 0L
        try {
            ownership.withLock {
                if (owner.token != generation.get()) return@withLock
                var sink: VoicePlaybackSink? = null
                try {
                    val output = createSink()
                    sink = output
                    output.start()
                    var trailingByte: Byte? = null
                    for (chunk in owner.queue) {
                        currentCoroutineContext().ensureActive()
                        if (owner.token != generation.get()) break
                        // HTTP reads need not end on a PCM16 sample boundary.
                        val bytes = trailingByte?.let { byteArrayOf(it) + chunk } ?: chunk
                        val completeBytes = bytes.size - bytes.size % 2
                        trailingByte = if (completeBytes < bytes.size) bytes.last() else null
                        var offset = 0
                        while (offset < completeBytes && owner.token == generation.get()) {
                            currentCoroutineContext().ensureActive()
                            val written = output.write(bytes, offset, completeBytes - offset)
                            when {
                                written > 0 -> {
                                    check(written <= completeBytes - offset && written % 2 == 0) { "Invalid PCM16 write count" }
                                    offset += written
                                    bytesWritten += written
                                    if (owner.token == generation.get()) played.set(Played(owner.token, bytesWritten))
                                }
                                written == 0 -> delay(zeroWriteDelayMillis)
                                else -> error("Audio output failed: $written")
                            }
                        }
                    }
                    if (owner.token == generation.get()) {
                        check(trailingByte == null) { "Incomplete PCM16 sample" }
                        drain(output, bytesWritten)
                    }
                } finally {
                    runCatching { sink?.close() }
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            owner.queue.cancel()
            withContext(callbackDispatcher) {
                if (owner.token == generation.get() && current.get() === owner) {
                    try {
                        onFailure()
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        // A client callback cannot strand the serialized native owner.
                    }
                }
            }
        } finally {
            owner.queue.cancel()
            current.compareAndSet(owner, null)
        }
    }

    private suspend fun drain(sink: VoicePlaybackSink, bytesWritten: Long) {
        val targetSamples = bytesWritten / 2
        val completed = withTimeoutOrNull(drainTimeoutMillis) {
            while (sink.playbackHeadPositionSamples < targetSamples) delay(zeroWriteDelayMillis)
            true
        }
        check(completed == true) { "Audio output drain timed out" }
    }
}
