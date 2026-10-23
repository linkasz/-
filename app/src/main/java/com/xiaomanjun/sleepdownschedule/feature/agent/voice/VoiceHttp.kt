package com.xiaomanjun.sleepdownschedule.feature.agent.voice

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Response

/** Cancellation must also abort a blocking body read after the response headers arrived. */
internal suspend fun <T> Call.useVoiceResponse(block: suspend (Response) -> T): T = coroutineScope {
    val call = this@useVoiceResponse
    val abort = launch(Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) {
        try { awaitCancellation() } finally { call.cancel() }
    }
    try {
        awaitVoice().use { block(it) }
    } catch (error: Exception) {
        currentCoroutineContext().ensureActive()
        throw error
    } finally { abort.cancel() }
}
