package com.xiaomanjun.sleepdownschedule.feature.agent.voice

import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.Source
import okio.Timeout
import okio.buffer
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceHttpTest {
    @Test fun interruptAfterHeadersUnblocksBodyReadAndClosesOnReader() = runBlocking {
        val call = BlockingBodyCall()
        val reader = launch(Dispatchers.IO) {
            call.useVoiceResponse { it.body!!.source().readUtf8Line() }
        }
        try {
            assertTrue("body read must have started", call.readStarted.await(5, TimeUnit.SECONDS))
            withTimeout(5_000) { reader.cancelAndJoin() }
            assertTrue("cancellation must reach the call after headers", call.isCanceled())
            assertTrue("response must close after the reader exits", call.bodyClosed.get())
        } finally { call.cancel(); reader.cancelAndJoin() }
    }

    private class BlockingBodyCall : Call {
        val readStarted = CountDownLatch(1)
        val bodyClosed = AtomicBoolean()
        private val cancelled = AtomicBoolean()
        private val unblock = CountDownLatch(1)
        private val request = Request.Builder().url("https://example.invalid/voice-test").build()
        private val bodySource = object : Source {
            override fun read(sink: Buffer, byteCount: Long): Long {
                readStarted.countDown()
                check(unblock.await(5, TimeUnit.SECONDS)) { "cancel did not unblock body read" }
                throw IOException("cancelled")
            }
            override fun timeout() = Timeout.NONE
            override fun close() { bodyClosed.set(true) }
        }.buffer()
        override fun request() = request
        override fun timeout() = Timeout.NONE
        override fun isExecuted() = true
        override fun isCanceled() = cancelled.get()
        override fun clone(): Call = BlockingBodyCall()
        override fun cancel() { cancelled.set(true); unblock.countDown() }
        override fun execute(): Response = error("use asynchronous call")
        override fun enqueue(responseCallback: Callback) {
            val body = object : ResponseBody() {
                override fun contentType(): MediaType? = null
                override fun contentLength() = -1L
                override fun source(): BufferedSource = bodySource
            }
            responseCallback.onResponse(this, Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body(body).build())
        }
    }
}
