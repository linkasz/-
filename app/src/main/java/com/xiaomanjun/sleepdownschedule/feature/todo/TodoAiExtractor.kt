package com.xiaomanjun.sleepdownschedule.feature.todo

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.xiaomanjun.sleepdownschedule.feature.importing.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class TodoAiConfig(
    val apiUrl: String = TodoAiExtractor.DEFAULT_URL,
    val apiKey: String = "",
    val textModel: String = "glm-4-flash",
    val visionModel: String = "glm-4v-flash",
    val credentialError: String? = null,
    val followMainConnection: Boolean = true,
    val availableModels: List<String> = emptyList()
)

internal data class TodoAiRequestConfig(
    val apiUrl: String,
    val apiKey: String,
    val model: String,
    val providerId: String,
    val endpointStyle: AiEndpointStyle,
    val structuredOutputMode: StructuredOutputMode,
    val authType: AiAuthType,
    val responsesPath: String,
    val chatCompletionsPath: String,
    val supportsVision: Boolean
) {
    fun endpoint(): String {
        val path = (if (endpointStyle == AiEndpointStyle.RESPONSES) responsesPath else chatCompletionsPath).trim('/')
        val base = apiUrl.trim().trimEnd('/')
        return if (path.isBlank()) base else "$base/$path"
    }
}

internal fun shouldTodoFollowMainConnection(
    hasSavedFlag: Boolean,
    savedFlag: Boolean,
    apiKey: String,
    apiUrl: String,
    textModel: String,
    visionModel: String
): Boolean = if (hasSavedFlag) savedFlag else {
    apiKey.isBlank() && apiUrl.trim().trimEnd('/') == TodoAiExtractor.DEFAULT_URL.trimEnd('/') &&
        textModel.trim() == "glm-4-flash" && visionModel.trim() == "glm-4v-flash"
}

internal fun resolveTodoAiRequestConfig(
    todo: TodoAiConfig,
    main: AiImportSettings?,
    useVisionModel: Boolean = false
): TodoAiRequestConfig = if (todo.followMainConnection) {
    requireNotNull(main) { "主 AI 连接不可用" }
    val profile = main.profile
    val endpointStyle = if (AiProviderPresets.shouldUseResponses(profile)) {
        AiEndpointStyle.RESPONSES
    } else {
        AiEndpointStyle.CHAT_COMPLETIONS
    }
    val requestPath = if (endpointStyle == AiEndpointStyle.RESPONSES) profile.responsesPath else profile.chatCompletionsPath
    val baseUrl = if (requestPath.isBlank()) {
        profile.baseUrl.trim().trimEnd('/')
    } else {
        normalizeAiBaseUrlForProvider(profile.id, profile.baseUrl).trimEnd('/')
    }
    TodoAiRequestConfig(
        apiUrl = baseUrl,
        apiKey = main.apiKey,
        model = profile.defaultModel,
        providerId = profile.id,
        endpointStyle = endpointStyle,
        structuredOutputMode = profile.structuredOutputMode,
        authType = profile.authType,
        responsesPath = profile.responsesPath,
        chatCompletionsPath = profile.chatCompletionsPath,
        supportsVision = AiProviderPresets.supportsImageInput(profile)
    )
} else {
    TodoAiRequestConfig(
        apiUrl = todo.apiUrl,
        apiKey = todo.apiKey,
        model = if (useVisionModel) todo.visionModel else todo.textModel,
        providerId = "todo_standalone",
        endpointStyle = AiEndpointStyle.CHAT_COMPLETIONS,
        structuredOutputMode = StructuredOutputMode.PROMPT_ONLY,
        authType = AiAuthType.ApiKeyBearer,
        responsesPath = "",
        chatCompletionsPath = "",
        supportsVision = true
    )
}

internal fun buildTodoAiRequestBody(
    config: TodoAiRequestConfig,
    prompt: String,
    imageDataUrls: List<String>
): JsonObject = if (config.endpointStyle == AiEndpointStyle.RESPONSES) {
    buildJsonObject {
        put("model", config.model)
        put("store", false)
        put("input", buildJsonArray {
            add(buildJsonObject {
                put("role", "user")
                put("content", buildJsonArray {
                    add(buildJsonObject {
                        put("type", "input_text")
                        put("text", prompt)
                    })
                    imageDataUrls.forEach { image ->
                        add(buildJsonObject {
                            put("type", "input_image")
                            put("image_url", image)
                        })
                    }
                })
            })
        })
        putStructuredTodoFormat(config.structuredOutputMode, responses = true)
    }
} else {
    buildJsonObject {
        put("model", config.model)
        put("temperature", 0.1)
        put("messages", buildJsonArray {
            add(buildJsonObject {
                put("role", "system")
                put("content", "你是待办事项提取器。只返回符合要求的 JSON。")
            })
            add(buildJsonObject {
                put("role", "user")
                put("content", if (imageDataUrls.isEmpty()) JsonPrimitive(prompt) else buildJsonArray {
                    add(buildJsonObject { put("type", "text"); put("text", prompt) })
                    imageDataUrls.forEach { image ->
                        add(buildJsonObject {
                            put("type", "image_url")
                            put("image_url", buildJsonObject { put("url", image) })
                        })
                    }
                })
            })
        })
        putStructuredTodoFormat(config.structuredOutputMode, responses = false)
    }
}

private fun kotlinx.serialization.json.JsonObjectBuilder.putStructuredTodoFormat(
    mode: StructuredOutputMode,
    responses: Boolean
) {
    if (mode == StructuredOutputMode.PROMPT_ONLY) return
    val schema = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            put("title", buildJsonObject { put("type", "string") })
            put("description", buildJsonObject { put("type", "string") })
            put("due_at", buildJsonObject { put("type", buildJsonArray { add(JsonPrimitive("string")); add(JsonPrimitive("null")) }) })
            put("priority", buildJsonObject { put("type", "string"); put("enum", buildJsonArray { listOf("LOW", "NORMAL", "HIGH", "URGENT").forEach { add(JsonPrimitive(it)) } }) })
            put("repeat_rule", buildJsonObject { put("type", "string"); put("enum", buildJsonArray { listOf("NONE", "DAILY", "WEEKLY", "MONTHLY").forEach { add(JsonPrimitive(it)) } }) })
            put("subtasks", buildJsonObject { put("type", "array"); put("items", buildJsonObject { put("type", "string") }) })
        })
        put("required", buildJsonArray {
            listOf("title", "description", "due_at", "priority", "repeat_rule", "subtasks")
                .forEach { add(JsonPrimitive(it)) }
        })
        put("additionalProperties", false)
    }
    val format = if (mode == StructuredOutputMode.JSON_SCHEMA) {
        buildJsonObject {
            put("type", "json_schema")
            if (responses) {
                put("name", "todo_extraction")
                put("strict", true)
                put("schema", schema)
            } else {
                put("json_schema", buildJsonObject {
                    put("name", "todo_extraction")
                    put("strict", true)
                    put("schema", schema)
                })
            }
        }
    } else {
        buildJsonObject { put("type", "json_object") }
    }
    if (responses) put("text", buildJsonObject { put("format", format) })
    else put("response_format", format)
}

internal fun parseTodoAiResponseText(body: String, endpointStyle: AiEndpointStyle): String {
    val root = Json.parseToJsonElement(body).jsonObject
    val raw = if (endpointStyle == AiEndpointStyle.RESPONSES) {
        root["output_text"]?.jsonPrimitive?.contentOrNull.orEmpty().ifBlank {
            root["output"]?.jsonArray.orEmpty().flatMap { item ->
                (item as? JsonObject)?.get("content")?.let { (it as? JsonArray).orEmpty() }.orEmpty()
            }.mapNotNull { part ->
                (part as? JsonObject)?.get("text")?.jsonPrimitive?.contentOrNull
            }.joinToString("\n")
        }
    } else {
        val message = root["choices"]?.jsonArray?.firstOrNull()?.jsonObject?.get("message")?.jsonObject
            ?: error("AI 返回内容格式错误")
        when (val content = message["content"]) {
            is JsonPrimitive -> content.contentOrNull.orEmpty()
            is JsonArray -> content.mapNotNull { (it as? JsonObject)?.get("text")?.jsonPrimitive?.contentOrNull }.joinToString("\n")
            else -> ""
        }
    }
    val start = raw.indexOf('{').takeIf { it >= 0 } ?: error("AI 没有返回 JSON")
    val end = raw.lastIndexOf('}').takeIf { it >= start } ?: error("AI 没有返回 JSON")
    return raw.substring(start, end + 1)
}

internal fun buildTodoExtractionPrompt(text: String, hasImages: Boolean, date: LocalDate): String = if (hasImages) {
    "从图片与补充文字提取待办，严格返回 JSON 对象，不要 Markdown。字段：title, description, due_at(ISO 本地日期或日期时间/null), priority(LOW/NORMAL/HIGH/URGENT), repeat_rule(NONE/DAILY/WEEKLY/MONTHLY), subtasks(字符串数组)。当前日期：$date。补充文字：$text"
} else {
    "请从下列内容提取一个待办事项，严格返回 JSON 对象，不要 Markdown。字段：title(字符串), description(字符串), due_at(ISO 8601 本地日期或日期时间；不确定为 null), priority(LOW/NORMAL/HIGH/URGENT), repeat_rule(NONE/DAILY/WEEKLY/MONTHLY), subtasks(字符串数组)。当前日期：$date。\n内容：\n$text"
}

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
        val apiUrl = p.getString(KEY_URL, DEFAULT_URL).orEmpty()
        val textModel = p.getString(KEY_TEXT_MODEL, "glm-4-flash").orEmpty()
        val visionModel = p.getString(KEY_VISION_MODEL, "glm-4v-flash").orEmpty()
        // Upgrade the legacy plaintext credential only after the encrypted value is committed.
        val legacy = p.getString(KEY_KEY, "").orEmpty()
        var credentialError: String? = null
        if (!p.contains(KEY_ENCRYPTED_KEY) && legacy.isNotBlank()) {
            try {
                val encrypted = com.xiaomanjun.sleepdownschedule.feature.importing.AiImportSettingsStore.encrypt(context, legacy)
                check(p.edit().putString(KEY_ENCRYPTED_KEY, encrypted).remove(KEY_KEY).commit())
            } catch (_: Exception) {
                credentialError = "待办 AI 凭据加密升级失败，原配置已保留，请重新配置"
            }
        }
        val encryptedKey = p.getString(KEY_ENCRYPTED_KEY, "").orEmpty()
        val apiKey = try {
            com.xiaomanjun.sleepdownschedule.feature.importing.AiImportSettingsStore.decrypt(context, encryptedKey)
        } catch (_: Exception) {
            credentialError = "待办 AI 凭据无法解密，原配置已保留，请重新配置"
            ""
        }
        if (encryptedKey.isNotBlank() && apiKey.isBlank()) {
            credentialError = "待办 AI 凭据无法解密，原配置已保留，请重新配置"
        }
        val followMainConnection = shouldTodoFollowMainConnection(
            hasSavedFlag = p.contains(KEY_FOLLOW_MAIN),
            savedFlag = p.getBoolean(KEY_FOLLOW_MAIN, true),
            apiKey = apiKey.ifBlank { legacy.ifBlank { if (encryptedKey.isNotBlank()) "configured" else "" } },
            apiUrl = apiUrl,
            textModel = textModel,
            visionModel = visionModel
        )
        if (!p.contains(KEY_FOLLOW_MAIN)) p.edit().putBoolean(KEY_FOLLOW_MAIN, followMainConnection).apply()
        val availableModels = runCatching {
            val encoded = p.getString(KEY_AVAILABLE_MODELS, null) ?: return@runCatching emptyList()
            val array = JSONArray(encoded)
            (0 until array.length()).mapNotNull { array.optString(it).trim().takeIf(String::isNotBlank) }.distinctBy(String::lowercase)
        }.getOrDefault(emptyList())
        return TodoAiConfig(
            apiUrl = apiUrl,
            apiKey = apiKey,
            textModel = textModel,
            visionModel = visionModel,
            credentialError = credentialError,
            followMainConnection = followMainConnection,
            availableModels = availableModels
        )
    }

    fun saveConfig(config: TodoAiConfig) {
        val editor = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_FOLLOW_MAIN, config.followMainConnection)
            .putString(KEY_AVAILABLE_MODELS, JSONArray(config.availableModels.map(String::trim).filter(String::isNotBlank).distinctBy(String::lowercase)).toString())
        if (!config.followMainConnection) {
            val apiUrl = runCatching { URL(config.apiUrl.trim()) }.getOrNull()
            require(apiUrl != null && apiUrl.protocol in setOf("https", "http") && apiUrl.host.isNotBlank()) {
                "请填写有效的 http:// 或 https:// API 地址"
            }
            require(config.credentialError == null || config.apiKey.isNotBlank()) { "请重新填写 API Key，原配置将保留到保存成功" }
            val encrypted = com.xiaomanjun.sleepdownschedule.feature.importing.AiImportSettingsStore.encrypt(context, config.apiKey)
            editor.putString(KEY_URL, config.apiUrl.trim())
                .putString(KEY_ENCRYPTED_KEY, encrypted)
                .remove(KEY_KEY)
                .putString(KEY_TEXT_MODEL, config.textModel.trim().ifBlank { "glm-4-flash" })
                .putString(KEY_VISION_MODEL, config.visionModel.trim().ifBlank { "glm-4v-flash" })
        }
        check(editor.commit()) { "保存待办 AI 配置失败，请重试" }
    }

    suspend fun extract(text: String, images: List<Uri>): TodoExtraction =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val todoConfig = readConfig()
            require(text.isNotBlank() || images.isNotEmpty()) { "先输入文字或选择图片" }
            require(text.length <= MAX_TEXT_CHARS) { "文本不能超过 ${MAX_TEXT_CHARS} 个字符" }
            val mainSettings = if (todoConfig.followMainConnection) {
                AiImportSettingsStore.loadForRuntime(context) ?: AiImportSettingsStore.load(context)
            } else {
                null
            }
            val requestConfig = resolveTodoAiRequestConfig(
                todoConfig,
                mainSettings,
                useVisionModel = images.isNotEmpty()
            )
            if (todoConfig.followMainConnection) {
                val activeMain = requireNotNull(mainSettings)
                require(activeMain.profile.id != AiProviderPresets.none.id && activeMain.profile.defaultModel.isNotBlank()) {
                    "请先在 AI 设置中选择连接并配置模型"
                }
                require(activeMain.apiKey.isNotBlank()) { "请先在 AI 设置中填写 API Key" }
            } else {
                check(todoConfig.credentialError == null) { todoConfig.credentialError.orEmpty() }
            }
            require(requestConfig.apiKey.isNotBlank()) { "请先配置 AI API Key" }
            require(requestConfig.model.isNotBlank()) { "请先在 AI 设置中配置模型" }
            if (images.isNotEmpty()) require(requestConfig.supportsVision) { "当前 AI 模型不支持图片输入，请更换支持视觉的模型" }

            var totalImageBytes = 0
            val imageDataUrls = images.map { uri ->
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                val bytes = readImageBytes(uri)
                totalImageBytes += bytes.size
                require(totalImageBytes <= MAX_TOTAL_IMAGE_BYTES) { "图片总大小不能超过 16 MB" }
                "data:$mime;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
            val prompt = buildTodoExtractionPrompt(text, images.isNotEmpty(), LocalDate.now())
            val body = buildTodoAiRequestBody(requestConfig, prompt, imageDataUrls)
            val connection = openAiPostConnection(
                url = requestConfig.endpoint(),
                apiKey = requestConfig.apiKey,
                authType = requestConfig.authType,
                contentType = "application/json; charset=utf-8",
                accept = "application/json",
                connectTimeoutMs = 20_000,
                readTimeoutMs = 60_000
            )
            try {
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                require(code in 200..299) { "AI 服务返回 HTTP $code：${body.take(300)}" }
                parseResponse(parseTodoAiResponseText(body, requestConfig.endpointStyle))
            } finally {
                connection.disconnect()
            }
        }

    private fun parseResponse(body: String): TodoExtraction {
        val obj = JSONObject(body)
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
        private const val KEY_ENCRYPTED_KEY = "api_key_encrypted"
        private const val KEY_TEXT_MODEL = "text_model"
        private const val KEY_VISION_MODEL = "vision_model"
        private const val KEY_FOLLOW_MAIN = "follow_main_connection_v1"
        private const val KEY_AVAILABLE_MODELS = "available_models_v1"
        private const val MAX_TEXT_CHARS = 40_000
        private const val MAX_IMAGE_BYTES = 8 * 1024 * 1024
        private const val MAX_TOTAL_IMAGE_BYTES = 16 * 1024 * 1024
    }
}
