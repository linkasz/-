package com.xiaomanjun.sleepdownschedule.feature.agent.voice

import com.xiaomanjun.sleepdownschedule.feature.importing.AiImportSettings
import com.xiaomanjun.sleepdownschedule.feature.importing.AiProviderPresets
import com.xiaomanjun.sleepdownschedule.feature.importing.AiProviderProfile
import com.xiaomanjun.sleepdownschedule.feature.importing.DiscoveredAiModel
import com.xiaomanjun.sleepdownschedule.feature.importing.discoverAiModels
import com.xiaomanjun.sleepdownschedule.feature.importing.setAiAuthHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.concurrent.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import javax.net.ssl.SSLException

/** Provider profile used by the model picker and the existing connection check. */
internal fun voiceModelDiscoveryProfile(settings: VoiceSettings): AiProviderProfile = when (settings.platform) {
    VoicePlatform.OPENAI -> AiProviderPresets.openAI.copy(baseUrl = "https://api.openai.com/v1")
    VoicePlatform.GLM -> AiProviderPresets.zhipu.copy(baseUrl = "https://open.bigmodel.cn/api/paas/v4")
    VoicePlatform.SILICON_FLOW -> AiProviderPresets.siliconFlow.copy(baseUrl = "https://api.siliconflow.cn/v1")
    VoicePlatform.ALIYUN -> AiProviderPresets.dashScope.copy(
        baseUrl = if (settings.region == "ap-southeast-1") "https://dashscope-intl.aliyuncs.com/compatible-mode/v1"
        else "https://${settings.workspace}.cn-beijing.maas.aliyuncs.com/compatible-mode/v1"
    )
    VoicePlatform.SYSTEM -> AiProviderPresets.none
}

/** Fetches the platform catalog only; it does not prove that the key can invoke a listed model. */
internal suspend fun discoverVoiceModels(settings: VoiceSettings): List<DiscoveredAiModel> {
    require(settings.platform != VoicePlatform.SYSTEM) { "系统语音没有在线模型目录，可继续使用系统默认语音" }
    require(settings.apiKey.isNotBlank() && settings.apiKey.none(Char::isISOControl)) { "请先填写有效的 API Key" }
    val profile = voiceModelDiscoveryProfile(settings)
    val catalog = if (settings.platform == VoicePlatform.ALIYUN) {
        discoverAliyunVoiceModels(settings)
    } else {
        discoverAiModels(AiImportSettings(profile, settings.apiKey))
    }
    return filterVoiceModelCatalog(settings.platform, catalog)
}

internal fun filterVoiceModelCatalog(platform: VoicePlatform, catalog: List<DiscoveredAiModel>): List<DiscoveredAiModel> {
    val accepted = when (platform) {
        VoicePlatform.SILICON_FLOW -> {
            val ids = catalog.map { it.id.lowercase() }.toSet()
            SiliconVoicePreset.entries.filter { it.asr.lowercase() in ids && it.tts.lowercase() in ids }
                .map { DiscoveredAiModel(it.id, it.label) }
        }
        else -> catalog.filter { isSupportedVoiceModel(platform, it.id) }
    }
    return accepted.distinctBy { it.id.lowercase() }.sortedBy { it.id.lowercase() }
}

private suspend fun discoverAliyunVoiceModels(settings: VoiceSettings): List<DiscoveredAiModel> {
    val endpoint = when (settings.region) {
        "ap-southeast-1" -> "https://dashscope-intl.aliyuncs.com/api/v1/models"
        "cn-beijing" -> {
            require(settings.workspace.matches(Regex("[A-Za-z0-9-]+"))) { "请填写百炼业务空间 ID" }
            "https://${settings.workspace}.cn-beijing.maas.aliyuncs.com/api/v1/models"
        }
        else -> error("当前地域暂不支持在线获取模型列表，可手动添加模型")
    }
    return withTimeout(30_000) {
        withContext(Dispatchers.IO) {
            val collected = linkedMapOf<String, DiscoveredAiModel>()
            var page = 1
            var total: Int? = null
            do {
                val uri = URI(endpoint)
                require(uri.scheme == "https" && uri.host in setOf("dashscope-intl.aliyuncs.com") ||
                    uri.scheme == "https" && uri.host.matches(Regex("[A-Za-z0-9-]+\\.cn-beijing\\.maas\\.aliyuncs\\.com"))) {
                    "百炼模型目录地址不受支持"
                }
                val separator = if ('?' in endpoint) '&' else '?'
                val response = readDirectoryPage("$endpoint${separator}page_no=$page&page_size=20", settings.apiKey)
                val directoryPage = parseAliyunModelPage(response)
                require(directoryPage.pageNumber == null || directoryPage.pageNumber == page) { "百炼模型目录页码不匹配，请手动添加模型" }
                require(directoryPage.models.isNotEmpty() || directoryPage.total == 0) { "百炼模型目录提前结束，请手动添加模型" }
                directoryPage.models.forEach { collected.putIfAbsent(it.id, it) }
                total = directoryPage.total
                page++
                check(page <= 101) { "百炼模型目录分页异常，请手动添加模型" }
            } while ((page - 1) * 20 < (total ?: 0))
            collected.values.toList()
        }
    }
}

internal data class AliyunVoiceModelPage(val total: Int, val models: List<DiscoveredAiModel>, val pageNumber: Int? = null)

internal fun parseAliyunModelPage(body: String): AliyunVoiceModelPage {
    val root = runCatching { Json.parseToJsonElement(body) as? JsonObject }.getOrNull()
        ?: error("百炼未返回兼容的模型目录，请手动添加模型")
    val output = root["output"] as? JsonObject ?: error("百炼未返回兼容的模型目录，请手动添加模型")
    val rows = output["models"] as? JsonArray ?: error("百炼未返回兼容的模型目录，请手动添加模型")
    val total = (output["total"] as? JsonPrimitive)?.content?.toIntOrNull()
        ?: error("百炼模型目录缺少分页总数，请手动添加模型")
    require(total >= 0) { "百炼模型目录分页信息无效，请手动添加模型" }
    val models = rows.mapNotNull { row ->
        val obj = row as? JsonObject ?: return@mapNotNull null
        val id = (obj["model"] as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim().orEmpty()
        val name = (obj["name"] as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim()
        id.takeIf { it.isNotBlank() && it.none(Char::isISOControl) }
            ?.let { DiscoveredAiModel(it, name?.takeIf(String::isNotBlank) ?: it) }
    }.distinctBy { it.id.lowercase() }
    require(rows.isEmpty() || models.isNotEmpty()) { "百炼模型条目缺少有效 ID，请手动添加模型" }
    val pageNumber = (output["page_no"] as? JsonPrimitive)?.content?.toIntOrNull()
    val pageSize = (output["page_size"] as? JsonPrimitive)?.content?.toIntOrNull()
    require(pageSize == null || pageSize == 20) { "百炼模型目录分页大小不匹配，请手动添加模型" }
    return AliyunVoiceModelPage(total, models, pageNumber)
}

private suspend fun readDirectoryPage(endpoint: String, key: String): String = withContext(Dispatchers.IO) {
    val uri = URI(endpoint)
    require(uri.scheme == "https" && uri.rawUserInfo == null && uri.rawFragment == null && uri.rawQuery?.contains("key", true) != true) {
        "模型目录地址无效"
    }
    suspendCancellableCoroutine { continuation ->
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 15_000
            instanceFollowRedirects = false
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setAiAuthHeader(key, com.xiaomanjun.sleepdownschedule.feature.importing.AiAuthType.ApiKeyBearer)
        }
        continuation.invokeOnCancellation { runCatching { connection.disconnect() } }
        try {
            val status = connection.responseCode
            check(status in 200..299) {
                when (status) {
                    401, 403 -> "无法获取百炼模型目录（HTTP $status），请检查 API Key 与业务空间权限"
                    404, 405, 501 -> "百炼未提供可用的模型目录接口，可继续手动添加模型"
                    in 300..399 -> "百炼模型目录发生重定向，请检查地域和业务空间"
                    else -> "获取百炼模型目录失败（HTTP $status），请稍后重试或手动添加模型"
                }
            }
            val output = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    check(output.size() + count <= 4 * 1024 * 1024) { "百炼模型目录响应过大，可手动添加模型" }
                    output.write(buffer, 0, count)
                }
            }
            if (continuation.isActive) continuation.resume(output.toString(Charsets.UTF_8.name()))
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            val safeError = when (error) {
                is SSLException -> IllegalStateException("无法建立百炼安全连接，请检查地域与证书")
                is IOException -> IllegalStateException("无法连接百炼模型目录，请检查网络与业务空间")
                else -> error
            }
            if (continuation.isActive) continuation.resumeWithException(safeError)
        } finally {
            runCatching { connection.disconnect() }
        }
    }
}
