package com.xiaomanjun.sleepdownschedule.feature.agent.voice

import android.util.Base64
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

internal fun aliyunHttpBase(settings: VoiceSettings): String =
    "https://${settings.workspace}.${settings.region}.maas.aliyuncs.com/api/v1"

/** HTTP TTS cannot rewrite the answer: every request receives only a fragment of the UI message. */
internal object AliyunTtsClient {
    private val client = OkHttpClient.Builder().readTimeout(60, TimeUnit.SECONDS).build()
    suspend fun stream(settings: VoiceSettings, text: String, onCall: (Call) -> Unit,
        onResponse: (Response) -> Unit, onPcm: suspend (ByteArray) -> Unit) = withContext(Dispatchers.IO) {
        require(text.isNotBlank()) { "没有可朗读的内容" }
        // Split by Unicode code point to respect API limits without breaking a character.
        val points = text.codePoints().toArray()
        for (part in points.toList().chunked(240)) {
            currentCoroutineContext().ensureActive()
            val fragment = String(part.toIntArray(), 0, part.size)
            val body = buildJsonObject {
                put("model", settings.ttsModel)
                putJsonObject("input") { put("text", fragment); put("voice", settings.ttsVoice); put("language_type", "Auto") }
            }.toString().toRequestBody("application/json".toMediaType())
            val call = client.newCall(Request.Builder().url(aliyunHttpBase(settings) + "/services/aigc/multimodal-generation/generation")
                .header("Authorization", "Bearer ${settings.apiKey}").header("X-DashScope-SSE", "enable").post(body).build())
            onCall(call)
            call.awaitVoice().use { response ->
                onResponse(response)
                check(response.isSuccessful) { "百炼朗读请求失败（${response.code}），请核对地域和音色权限" }
                val source = requireNotNull(response.body).source()
                var received = false
                while (!source.exhausted()) {
                    currentCoroutineContext().ensureActive()
                    val line = source.readUtf8Line() ?: break
                    if (!line.startsWith("data:")) continue
                    val data = line.removePrefix("data:").trim()
                    if (data.isBlank() || data == "[DONE]") continue
                    val json = Json.parseToJsonElement(data).jsonObject
                    check((json["code"] as? JsonPrimitive)?.content.orEmpty().isBlank()) { "百炼朗读返回错误，请核对音色与模型" }
                    val audio = (json["output"] as? JsonObject)?.get("audio") as? JsonObject
                    val encoded = (audio?.get("data") as? JsonPrimitive)?.content.orEmpty()
                    if (encoded.isNotEmpty()) { received = true; onPcm(Base64.decode(encoded, Base64.DEFAULT)) }
                }
                check(received) { "百炼未返回朗读音频，请重新试听或检查权限" }
            }
        }
    }
}
