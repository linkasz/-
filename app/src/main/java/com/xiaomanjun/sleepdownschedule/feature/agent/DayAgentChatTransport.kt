package com.xiaomanjun.sleepdownschedule.feature.agent

import com.xiaomanjun.sleepdownschedule.feature.importing.*

import com.xiaomanjun.sleepdownschedule.*

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive

private val AgentChatJson = Json { ignoreUnknownKeys = true; isLenient = true }

internal fun AiImportSettings.usesOfficialOpenAiEndpoint(): Boolean =
    profile.id == AiProviderPresets.openAI.id &&
        isOfficialOpenAIBaseUrl(normalizeAiBaseUrlForProvider(profile.id, profile.baseUrl))

internal fun AiImportSettings.usesDeepSeekChatEndpoint(): Boolean =
    profile.id == AiProviderPresets.deepSeek.id ||
        runCatching {
            URL(normalizeAiBaseUrlForProvider(profile.id, profile.baseUrl)).host
                .equals("api.deepseek.com", ignoreCase = true)
        }.getOrDefault(false)

/**
 * OpenAI-compatible Chat Completions wire transport.
 *
 * Agent orchestration and local tool execution stay in [DayAgentService]; this class owns request
 * serialization, authentication, HTTP, and streaming response normalization.
 */
internal class DayAgentChatTransport {
    @OptIn(InternalCoroutinesApi::class)
    fun post(settings: AiImportSettings, body: String, job: Job? = null): String {
        val request = openConnection(settings, body, job)
        val connection = request.connection
        return try {
            job?.ensureActive()
            connection.readResponse(settings.profile.id).also { job?.ensureActive() }
        } catch (error: Throwable) {
            job?.ensureActive()
            throw error
        } finally {
            request.cancellation?.dispose()
            runCatching { connection.disconnect() }
        }
    }

    @OptIn(InternalCoroutinesApi::class)
    fun stream(
        settings: AiImportSettings,
        body: String,
        onDelta: (String) -> Unit,
        onUsage: (AgentTokenUsage) -> Unit,
        job: Job? = null
    ): String {
        val request = openConnection(settings, body, job)
        val connection = request.connection
        return try {
            job?.ensureActive()
            val code = connection.responseCode
            if (code !in 200..299) {
                val error = connection.errorStream
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    .orEmpty()
                    .take(300)
                throw IllegalStateException(formatAiRequestError(code, error, settings.profile.id))
            }
            if (!connection.contentType.orEmpty().contains("text/event-stream", ignoreCase = true)) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                job?.ensureActive()
                onUsage(parseAgentTokenUsage(response))
                val content = parseFullChatContent(response)
                job?.ensureActive()
                onDelta(content)
                content
            } else {
                val result = StringBuilder()
                var hasFinalContent = false
                connection.forEachSseDataLine { data ->
                    job?.ensureActive()
                    val event = parseSseJsonObject(data) ?: return@forEachSseDataLine
                    val usage = agentTokenUsage(event)
                    if (!usage.isEmpty) {
                        job?.ensureActive()
                        onUsage(usage)
                    }
                    val content = runCatching {
                        val choice = event["choices"]
                            ?.jsonArray
                            ?.firstOrNull()
                            ?.jsonObject
                            ?: return@runCatching ""
                        val streamed = choice["delta"]?.jsonObject
                        agentTextFromJson(streamed?.get("content"))
                            .ifBlank { agentTextFromJson(choice["text"]) }
                    }.getOrNull().orEmpty()
                    if (content.isNotEmpty()) {
                        job?.ensureActive()
                        hasFinalContent = true
                        result.append(content)
                        onDelta(content)
                    }
                }
                job?.ensureActive()
                if (!hasFinalContent) throw MissingAgentBodyException()
                result.toString()
            }
        } catch (error: Throwable) {
            job?.ensureActive()
            throw error
        } finally {
            request.cancellation?.dispose()
            runCatching { connection.disconnect() }
        }
    }

    fun body(
        settings: AiImportSettings,
        messages: List<Pair<String, String>>,
        stream: Boolean
    ): String = buildJsonObject {
        put("model", settings.profile.defaultModel)
        put("stream", stream)
        put("temperature", 0.55)
        put("messages", buildJsonArray {
            messages.forEach { (role, content) ->
                add(buildJsonObject {
                    put("role", role)
                    put("content", content)
                })
            }
        })
        if (!stream && settings.profile.structuredOutputMode == StructuredOutputMode.JSON_OBJECT) {
            put("response_format", buildJsonObject { put("type", "json_object") })
        }
    }.toString()

    fun agentBody(
        settings: AiImportSettings,
        messages: List<JsonObject>,
        stream: Boolean,
        includeTools: Boolean,
        includeMemoryTool: Boolean = false,
        forceMiMoWebSearch: Boolean = false,
        excludedTools: Set<AgentToolName> = emptySet()
    ): String = buildJsonObject {
        put("model", settings.profile.defaultModel)
        put("stream", stream)
        put("temperature", 0.35)
        if (settings.usesOfficialOpenAiEndpoint()) put("store", false)
        put("messages", buildJsonArray { messages.forEach(::add) })
        if (includeTools) {
            put(
                "tools",
                agentToolDefinitions(
                    includeMiMoWebSearch = supportsMiMoOfficialWebSearch(
                        providerId = settings.profile.id,
                        baseUrl = normalizeAiBaseUrlForProvider(
                            settings.profile.id,
                            settings.profile.baseUrl
                        ),
                        model = settings.profile.defaultModel
                    ),
                    forceMiMoWebSearch = forceMiMoWebSearch,
                    includeMemoryTool = includeMemoryTool,
                    // Several compatible providers reject this Chat Completions extension.
                    strictFunctions = settings.usesOfficialOpenAiEndpoint(),
                    excludedTools = excludedTools
                )
            )
            put("tool_choice", "auto")
        } else if (settings.usesDeepSeekChatEndpoint()) {
            // DeepSeek otherwise occasionally serializes an imagined function as DSML text even
            // though this final-answer request intentionally exposes no native tools.
            put("tool_choice", "none")
        }
    }.toString()

    @OptIn(InternalCoroutinesApi::class)
    private fun openConnection(
        settings: AiImportSettings,
        body: String,
        job: Job?
    ): ChatCancellableConnection {
        job?.ensureActive()
        val path = settings.profile.chatCompletionsPath.trim('/')
        val base = if (path.isEmpty()) {
            // 未显式配置路径：直接使用下发的完整地址，不再 normalize 剥掉 /chat/completions 等后缀
            settings.profile.baseUrl.trim().trimEnd('/')
        } else {
            normalizeAiBaseUrlForProvider(settings.profile.id, settings.profile.baseUrl).trimEnd('/')
        }
        val connection = openAiPostConnection(
            url = if (path.isEmpty()) base else "$base/$path",
            apiKey = settings.apiKey,
            authType = settings.profile.authType,
            contentType = "application/json; charset=utf-8",
            accept = null
        )
        val cancellation = job?.let { connection.disconnectAgentRequestOnCancellation(it) }
        try {
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            return ChatCancellableConnection(connection, cancellation)
        } catch (error: Throwable) {
            cancellation?.dispose()
            runCatching { connection.disconnect() }
            job?.ensureActive()
            throw error
        }
    }
}

private data class ChatCancellableConnection(
    val connection: HttpURLConnection,
    val cancellation: DisposableHandle?
)

internal fun parseFullChatContent(response: String): String {
    val root = AgentChatJson.parseToJsonElement(response).jsonObject
    val choice = root["choices"]?.jsonArray?.firstOrNull()?.jsonObject
        ?: throw MissingAgentBodyException()
    val message = choice["message"]?.jsonObject
    return agentTextFromJson(message?.get("content"))
        .ifBlank { agentTextFromJson(choice["text"]) }
        .takeIf(String::isNotBlank)
        ?: throw MissingAgentBodyException()
}

internal class MissingAgentBodyException : IllegalStateException("AI 没有返回最终正文")

private fun HttpURLConnection.readResponse(providerId: String): String {
    val code = responseCode
    val stream = if (code in 200..299) inputStream else errorStream
    val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
    if (code !in 200..299) {
        throw IllegalStateException(formatAiRequestError(code, text, providerId))
    }
    return text
}
