package com.xiaomanjun.sleepdownschedule.feature.agent.voice

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@Serializable
internal data class TtsVoice(val id: String, val name: String, val language: List<String>, val gender: String,
    val style: String, val sampleUrl: String? = null, val compatibleModels: List<String>, val source: String)

internal object AliyunTtsVoices {
    const val SOURCE = "https://help.aliyun.com/zh/model-studio/qwen-tts-voice-list"
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder().readTimeout(30, TimeUnit.SECONDS).build()
    fun load(context: Context, model: String): List<TtsVoice> {
        val cached = context.getSharedPreferences("scheduleplus_voice", 0).getString("tts_catalog", null)
        val voices = cached?.let { runCatching { json.decodeFromString<List<TtsVoice>>(it) }.getOrNull() }
            ?: context.assets.open("voices/aliyun-tts.json").bufferedReader().use { json.decodeFromString(it.readText()) }
        return voices.filter { model in it.compatibleModels }
    }

    /** Built-in voices are published in an official table, not a fabricated list API. */
    suspend fun refresh(context: Context, model: String): List<TtsVoice> = withContext(Dispatchers.IO) {
        val body = client.newCall(Request.Builder().url(SOURCE).build()).awaitVoice().use {
            check(it.isSuccessful) { "官方音色文档读取失败（${it.code}）" }; it.body?.string().orEmpty()
        }
        val voices = parseOfficialTable(body)
        require(voices.any { model in it.compatibleModels }) { "官方页面格式已变化，继续使用本地音色目录" }
        check(context.getSharedPreferences("scheduleplus_voice", 0).edit().putString("tts_catalog", json.encodeToString(voices)).commit()) { "音色目录缓存失败" }
        voices.filter { model in it.compatibleModels }
    }

    internal fun parseOfficialTable(raw: String): List<TtsVoice> {
        val html = raw.replace("\\\"", "\"").replace("\\n", "\n").replace("\\/", "/")
        val section = html.split("<h2").firstOrNull { "非实时" in it && "<table" in it }
            ?: error("没有找到官方非实时音色目录")
        val table = section.substringBefore("</table>")
        fun plain(text: String) = text.replace(Regex("<[^>]+>"), "").replace("&nbsp;", " ").replace("&amp;", "&").trim()
        return Regex("<tr[^>]*>([\\s\\S]*?)</tr>").findAll(table).mapNotNull { row ->
            val cells = Regex("<td[^>]*>([\\s\\S]*?)</td>").findAll(row.groupValues[1]).map { it.groupValues[1] }.toList()
            if (cells.size != 4) return@mapNotNull null
            val models = Regex("\\bqwen[\\w.-]+").findAll(plain(cells[3])).map { it.value }.distinct().toList()
            if ("qwen3-tts-flash" !in models) return@mapNotNull null
            val id = plain(cells[0]); val description = plain(cells[1])
            val name = Regex("音色名[：:]([\\s\\S]*?)描述").find(description)?.groupValues?.get(1)?.trim() ?: id
            val url = Regex("<audio[^>]+src=\"([^\"]+)\"").find(cells[1])?.groupValues?.get(1)
                ?.takeIf { it.startsWith("https://help-static-aliyun-doc.aliyuncs.com/") }
            TtsVoice(id, name, plain(cells[2]).split('、'), if ("女性" in description) "女" else if ("男性" in description) "男" else "未知",
                "官方系统音色", url, models, SOURCE)
        }.distinctBy { it.id }.toList().also { require(it.size >= 10) { "音色目录不完整" } }
    }

    /** Account custom voices are a separate paginated API, filtered by target_model. */
    suspend fun accountVoices(settings: VoiceSettings): List<TtsVoice> = withContext(Dispatchers.IO) {
        settings.validated()
        val rows = mutableListOf<TtsVoice>()
        var page = 0
        while (true) {
            require(page < 100) { "账号音色过多，请缩小查询范围" }
            val payload = buildJsonObject { put("model", "qwen-voice-enrollment")
                putJsonObject("input") { put("action", "list"); put("page_index", page); put("page_size", 100) } }
            val root = client.newCall(Request.Builder().url(aliyunHttpBase(settings) + "/services/audio/tts/customization")
                .header("Authorization", "Bearer ${settings.apiKey}").post(payload.toString().toRequestBody("application/json".toMediaType())).build())
                .awaitVoice().use { response ->
                    check(response.isSuccessful) { "账号音色查询失败（${response.code}），请检查业务空间和权限" }
                    json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject
                }
            val output = root["output"] as? JsonObject ?: error("账号音色接口没有返回目录")
            val list = output["voice_list"] as? JsonArray ?: error("账号音色目录格式不正确")
            list.forEach { item ->
                val voice = item.jsonObject
                val target = voice["target_model"]?.jsonPrimitive?.content.orEmpty()
                if (target == settings.ttsModel) {
                    val id = voice["voice"]?.jsonPrimitive?.content.orEmpty()
                    if (id.isNotBlank()) rows += TtsVoice(id, "自定义 · $id", listOf(voice["language"]?.jsonPrimitive?.content ?: "未知"),
                        "未知", "账号自定义", null, listOf(target), "account")
                }
            }
            val total = output["total_count"]?.jsonPrimitive?.intOrNull ?: error("账号音色分页信息缺失")
            if ((page + 1) * 100 >= total) break
            check(list.isNotEmpty()) { "账号音色分页提前结束，请重试" }
            page++
        }
        rows.distinctBy { it.id }
    }
}
