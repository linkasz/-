package com.xiaomanjun.sleepdownschedule.feature.todo

import android.content.Context
import android.net.Uri
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import org.json.JSONArray
import org.json.JSONObject

data class TodoAiConfig(
    val apiUrl: String = TodoAiExtractor.DEFAULT_URL,
    val apiKey: String = "",
    val textModel: String = "glm-4-flash",
    val visionModel: String = "glm-4v-flash"
)

data class TodoExtraction(
    val title: String,
    val description: String,
    val dueAt: Long?,
    val allDay: Boolean,
    val priority: Int,
    val repeatRule: String,
    val subtasks: List<String>
)

class TodoAiExtractor @Inject constructor(@ApplicationContext private val context: Context) {
    fun readConfig(): TodoAiConfig {
        val p = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        return TodoAiConfig(
            apiUrl = p.getString(KEY_URL, DEFAULT_URL).orEmpty(),
            apiKey = p.getString(KEY_KEY, "").orEmpty(),
            textModel = p.getString(KEY_TEXT_MODEL, "glm-4-flash").orEmpty(),
            visionModel = p.getString(KEY_VISION_MODEL, "glm-4v-flash").orEmpty()
        )
    }

    fun saveConfig(config: TodoAiConfig) {
        require(config.apiUrl.startsWith("https://") || config.apiUrl.startsWith("http://")) {
            "API 地址必须以 http:// 或 https:// 开头"
        }
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit()
            .putString(KEY_URL, config.apiUrl.trim())
            .putString(KEY_KEY, config.apiKey)
            .putString(KEY_TEXT_MODEL, config.textModel.trim().ifBlank { "glm-4-flash" })
            .putString(KEY_VISION_MODEL, config.visionModel.trim().ifBlank { "glm-4v-flash" })
            .apply()
    }

    suspend fun extract(text: String, images: List<Uri>): TodoExtraction =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val config = readConfig()
            require(config.apiKey.isNotBlank()) { "请先在 AI 设置中填写 API Key" }
            require(text.isNotBlank() || images.isNotEmpty()) { "先输入文字或选择图片" }
            require(text.length <= MAX_TEXT_CHARS) { "文本不能超过 ${MAX_TEXT_CHARS} 个字符" }
            val model = if (images.isEmpty()) config.textModel else config.visionModel
            val content: Any = if (images.isEmpty()) {
                "请从下列内容提取一个待办事项，严格返回 JSON 对象，不要 Markdown。字段：title(字符串), description(字符串), due_at(ISO 8601 本地日期或日期时间；不确定为 null), priority(LOW/NORMAL/HIGH/URGENT), repeat_rule(NONE/DAILY/WEEKLY/MONTHLY), subtasks(字符串数组)。当前日期：${LocalDate.now()}。\n内容：\n$text"
            } else {
                var totalImageBytes = 0
                JSONArray().apply {
                    put(JSONObject().put("type", "text").put("text", "从图片与补充文字提取待办，严格返回 JSON 对象，不要 Markdown。字段：title, description, due_at(ISO 本地日期或日期时间/null), priority(LOW/NORMAL/HIGH/URGENT), repeat_rule(NONE/DAILY/WEEKLY/MONTHLY), subtasks(字符串数组)。当前日期：${LocalDate.now()}。补充文字：$text"))
                    images.forEach { uri ->
                        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                        val bytes = readImageBytes(uri)
                        totalImageBytes += bytes.size
                        require(totalImageBytes <= MAX_TOTAL_IMAGE_BYTES) { "图片总大小不能超过 16 MB" }
                        val dataUrl = "data:$mime;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
                        put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", dataUrl)))
                    }
                }
            }
            val user = JSONObject().put("role", "user").put("content", content)
            val request = JSONObject()
                .put("model", model)
                .put("temperature", 0.1)
                .put("messages", JSONArray().put(JSONObject().put("role", "system").put("content", "你是待办事项提取器。只返回符合要求的 JSON。" )).put(user))
            val connection = URL(config.apiUrl).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 20_000
                connection.readTimeout = 60_000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setRequestProperty("Authorization", "Bearer ${config.apiKey}")
                connection.outputStream.use { it.write(request.toString().toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                require(code in 200..299) { "AI 服务返回 HTTP $code：${body.take(300)}" }
                parseResponse(body)
            } finally {
                connection.disconnect()
            }
        }

    private fun parseResponse(body: String): TodoExtraction {
        val rawContent = JSONObject(body).getJSONArray("choices")
            .getJSONObject(0).getJSONObject("message").get("content")
        val text = if (rawContent is String) rawContent else buildString {
            val chunks = rawContent as? JSONArray ?: error("AI 返回内容格式错误")
            for (index in 0 until chunks.length()) {
                val part = chunks.optJSONObject(index) ?: continue
                append(part.optString("text"))
            }
        }
        val jsonText = text.substring(text.indexOf('{').takeIf { it >= 0 } ?: error("AI 没有返回 JSON"), text.lastIndexOf('}') + 1)
        val obj = JSONObject(jsonText)
        val priority = when (obj.optString("priority").uppercase()) {
            "LOW" -> 0
            "HIGH" -> 2
            "URGENT" -> 3
            else -> 1
        }
        val repeat = obj.optString("repeat_rule", "NONE").uppercase()
            .takeIf { it in setOf("NONE", "DAILY", "WEEKLY", "MONTHLY") } ?: "NONE"
        val dueText = obj.optString("due_at").takeIf { it.isNotBlank() && it != "null" }
        val dueAt = dueText?.let(::parseLocalTime)
        val allDay = dueText?.length == 10
        val subtasksJson = obj.optJSONArray("subtasks") ?: JSONArray()
        return TodoExtraction(
            title = obj.optString("title").trim().ifBlank { "新待办" },
            description = obj.optString("description").trim(),
            dueAt = dueAt,
            allDay = allDay,
            priority = priority,
            repeatRule = repeat,
            subtasks = (0 until subtasksJson.length()).mapNotNull { subtasksJson.optString(it).trim().takeIf(String::isNotEmpty) }
        )
    }

    private fun parseLocalTime(value: String): Long = runCatching {
        LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }.getOrElse {
        LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun readImageBytes(uri: Uri): ByteArray {
        val input = context.contentResolver.openInputStream(uri) ?: error("无法读取图片")
        return input.use { stream ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            var total = 0
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                total += count
                require(total <= MAX_IMAGE_BYTES) { "单张图片不能超过 8 MB" }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
    }

    companion object {
        const val DEFAULT_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions"
        private const val PREFERENCES = "schedule_plus_ai"
        private const val KEY_URL = "api_url"
        private const val KEY_KEY = "api_key"
        private const val KEY_TEXT_MODEL = "text_model"
        private const val KEY_VISION_MODEL = "vision_model"
        private const val MAX_TEXT_CHARS = 40_000
        private const val MAX_IMAGE_BYTES = 8 * 1024 * 1024
        private const val MAX_TOTAL_IMAGE_BYTES = 16 * 1024 * 1024
    }
}
