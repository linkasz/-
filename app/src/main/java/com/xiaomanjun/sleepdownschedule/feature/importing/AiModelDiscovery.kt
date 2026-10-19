package com.xiaomanjun.sleepdownschedule.feature.importing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URI
import javax.net.ssl.SSLException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal data class DiscoveredAiModel(val id: String, val name: String = id)

private const val ModelListTimeoutMs = 30_000L
private const val MaxModelListBytes = 4 * 1024 * 1024

internal fun aiModelListEndpoint(profile: AiProviderProfile): String {
    require(profile.id != AiProviderPresets.none.id && !AiProviderPresets.isManagedFreeId(profile.id)) {
        "请先选择自己的 AI 服务商"
    }
    val base = normalizeAiBaseUrlForProvider(profile.id, profile.baseUrl)
    val uri = runCatching { URI(base) }.getOrNull()
    require(uri != null && uri.scheme in listOf("http", "https") && !uri.host.isNullOrBlank() &&
        uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null) {
        "请填写有效的接口地址，不要在地址中包含密钥、查询参数或片段"
    }
    return if (base.endsWith("/models", ignoreCase = true)) base else "$base/models"
}

internal fun parseAiModelList(body: String): List<DiscoveredAiModel> {
    val root = runCatching { Json.parseToJsonElement(body) }.getOrNull()
        ?: error("平台返回的模型列表不是有效 JSON，请检查接口地址或手动添加模型")
    val entries = when (root) {
        is JsonArray -> root
        is JsonObject -> root["data"] as? JsonArray
        else -> null
    } ?: error("平台未返回兼容的模型列表，请手动添加模型")
    val models = entries.mapNotNull { entry ->
        fun JsonObject.string(key: String): String? = (get(key) as? JsonPrimitive)
            ?.takeIf { it.isString }?.content?.trim()?.takeIf { it.isNotBlank() }
        when (entry) {
            is JsonObject -> entry.string("id")?.let { DiscoveredAiModel(it, entry.string("name") ?: it) }
            is JsonPrimitive -> entry.takeIf { it.isString }?.content?.trim()
                ?.takeIf { it.isNotBlank() }?.let { DiscoveredAiModel(it) }
            else -> null
        }
    }.filter { it.id.none(Char::isISOControl) }
        .distinctBy { it.id }
        .sortedBy { it.id.lowercase() }
    check(entries.isEmpty() || models.isNotEmpty()) { "平台返回的模型条目缺少有效 ID，请手动添加模型" }
    // Never silently present a partial list as the account's complete catalog.
    check((root as? JsonObject)?.get("has_more") != JsonPrimitive(true)) {
        "平台模型列表需要分页，当前接口暂不支持完整获取，请手动添加模型"
    }
    return models
}

internal fun mergeAiModelIds(existing: List<String>, added: List<String>): List<String> =
    (existing + added).map(String::trim).filter(String::isNotBlank).distinctBy(String::lowercase)

/** Discovery uses only the connection draft; it never persists settings or calls a model. */
internal suspend fun discoverAiModels(settings: AiImportSettings): List<DiscoveredAiModel> {
    val endpoint = aiModelListEndpoint(settings.profile)
    val key = settings.apiKey.trim()
    require(key.isNotBlank() && key.none(Char::isISOControl)) { "请先填写有效的 API Key" }
    return withTimeoutOrNull(ModelListTimeoutMs) {
        withContext(Dispatchers.IO) {
            val connection = (URI(endpoint).toURL().openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 15_000
                // Do not forward credentials to a redirect target controlled by the response.
                instanceFollowRedirects = false
                useCaches = false
                setRequestProperty("Accept", "application/json")
                setAiAuthHeader(key, settings.profile.authType)
            }
            suspendCancellableCoroutine { continuation ->
                continuation.invokeOnCancellation { runCatching { connection.disconnect() } }
                try {
                    val status = connection.responseCode
                    check(status in 200..299) {
                        when (status) {
                            401, 403 -> "无法获取模型列表（HTTP $status），请检查 API Key 和模型访问权限"
                            404, 405, 501 -> "平台未提供兼容的模型列表接口（HTTP $status），可继续手动添加模型"
                            429 -> "请求过于频繁或额度不足（HTTP 429），请稍后重试"
                            in 300..399 -> "模型列表接口发生重定向，请核对并填写最终接口地址"
                            else -> "获取模型列表失败（HTTP $status），请稍后重试或手动添加"
                        }
                    }
                    val bytes = connection.inputStream.use { stream ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = stream.read(buffer)
                            if (count < 0) break
                            check(output.size() + count <= MaxModelListBytes) { "模型列表过大，请使用平台控制台确认模型 ID 后手动添加" }
                            output.write(buffer, 0, count)
                        }
                        output.toByteArray()
                    }
                    val models = parseAiModelList(bytes.toString(Charsets.UTF_8))
                    continuation.resume(models)
                } catch (error: Exception) {
                    // Remote bodies, URLs and exception messages can echo credentials.
                    val safeError = when (error) {
                        is SocketTimeoutException -> IllegalStateException("获取模型列表超时，请检查网络后重试")
                        is SSLException -> IllegalStateException("无法建立安全连接，请检查接口地址与证书")
                        is IOException -> IllegalStateException("无法连接模型列表接口，请检查网络与接口地址")
                        else -> error
                    }
                    if (continuation.isActive) continuation.resumeWithException(safeError)
                } finally {
                    runCatching { connection.disconnect() }
                }
            }
        }
    } ?: error("获取模型列表超时，请稍后重试")
}
