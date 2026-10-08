package com.xiaomanjun.sleepdownschedule.feature.update

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

internal enum class SchedulePlusUpdateChannel { STABLE, BETA }

internal data class SchedulePlusUpdate(
    val applicationId: String,
    val versionName: String,
    val versionCode: Long,
    val apkUrl: String,
    val sha256: String,
    val releaseUrl: String,
    val releaseNotes: String,
    val isPrerelease: Boolean
)

internal data class SchedulePlusUpdateCheck(
    val update: SchedulePlusUpdate?,
    val message: String
)

internal class SchedulePlusUpdateException(message: String, cause: Throwable? = null) : IOException(message, cause)

/** Reads only public Releases from the SchedulePlus repository configured below. */
internal class SchedulePlusUpdateRepository(
    private val cacheDirectory: File,
    private val connectionFactory: (URL) -> HttpURLConnection = ::openConnection
) {
    fun check(channel: SchedulePlusUpdateChannel, applicationId: String, currentVersionCode: Long): SchedulePlusUpdateCheck {
        val release = when (channel) {
            SchedulePlusUpdateChannel.STABLE -> {
                val stable = parseRelease(JSONObject(readJson("$API_ROOT/releases/latest", RELEASE_JSON_LIMIT)))
                if (stable.isDraft || stable.isPrerelease) {
                    throw SchedulePlusUpdateException("自有更新源返回的不是正式版 Release")
                }
                stable
            }
            SchedulePlusUpdateChannel.BETA -> {
                val releases = parseReleaseList(readJson("$API_ROOT/releases?per_page=100", RELEASE_LIST_LIMIT))
                releases.asSequence()
                    .filter { !it.isDraft && it.isPrerelease }
                    .maxByOrNull { it.publishedAt }
                    ?: return SchedulePlusUpdateCheck(null, "目前還沒有可用的 Beta 版本")
            }
        }
        val update = readUpdateManifest(release, applicationId)
        if (update.versionCode <= currentVersionCode) {
            return SchedulePlusUpdateCheck(null, "目前已是最新版本")
        }
        return SchedulePlusUpdateCheck(update, "发现新版本 ${update.versionName}")
    }

    fun download(update: SchedulePlusUpdate): File {
        require(isOwnedReleaseAssetUrl(update.apkUrl, APK_ASSET_NAME)) { "更新 APK 地址不属于时序清单发布仓库" }
        val directory = File(cacheDirectory, "scheduleplus-updates").apply {
            if (!exists() && !mkdirs()) throw SchedulePlusUpdateException("无法创建更新缓存目录")
        }
        val output = File(directory, "schedule-plus-${update.versionCode}.apk")
        if (output.isFile && sha256(output) == update.sha256) return output
        output.delete()
        val temporary = File(directory, "${output.name}.download")
        temporary.delete()

        val connection = open(update.apkUrl)
        try {
            val status = connection.responseCode
            if (status !in 200..299) throw httpFailure(status)
            val advertisedLength = connection.contentLengthLong
            if (advertisedLength > MAX_APK_BYTES) throw SchedulePlusUpdateException("更新文件超过大小限制")
            val digest = MessageDigest.getInstance("SHA-256")
            var total = 0L
            connection.inputStream.buffered().use { input ->
                temporary.outputStream().buffered().use { outputStream ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > MAX_APK_BYTES) throw SchedulePlusUpdateException("更新文件超过大小限制")
                        digest.update(buffer, 0, count)
                        outputStream.write(buffer, 0, count)
                    }
                }
            }
            val downloadedHash = digest.digest().toHexString()
            if (downloadedHash != update.sha256) {
                throw SchedulePlusUpdateException("更新文件 SHA-256 校验失败，请稍后重试")
            }
            if (!temporary.renameTo(output)) throw SchedulePlusUpdateException("无法保存已校验的更新文件")
            return output
        } catch (error: Exception) {
            temporary.delete()
            if (error is SchedulePlusUpdateException) throw error
            throw SchedulePlusUpdateException("下载更新失败：${error.message ?: "网络错误"}", error)
        } finally {
            connection.disconnect()
        }
    }

    private fun readUpdateManifest(release: GitHubRelease, expectedApplicationId: String): SchedulePlusUpdate {
        val manifestAsset = release.assets.singleOrNull { it.name == MANIFEST_ASSET_NAME }
            ?: throw SchedulePlusUpdateException("此 Release 缺少 $MANIFEST_ASSET_NAME")
        if (!isOwnedReleaseAssetUrl(manifestAsset.url, MANIFEST_ASSET_NAME)) {
            throw SchedulePlusUpdateException("Release 资产链接不属于时序清单发布仓库")
        }
        return parseUpdateManifest(
            readJson(manifestAsset.url, MANIFEST_JSON_LIMIT),
            release,
            expectedApplicationId
        )
    }

    private fun readJson(url: String, maxBytes: Int): String {
        val connection = open(url)
        try {
            val status = connection.responseCode
            if (status == HttpURLConnection.HTTP_NOT_FOUND) {
                throw SchedulePlusUpdateException("自有仓库尚未公开，或当前频道还没有 Release")
            }
            if (status == HttpURLConnection.HTTP_FORBIDDEN) {
                throw SchedulePlusUpdateException("GitHub 暂时限制了更新请求，请稍后再试")
            }
            if (status !in 200..299) throw httpFailure(status)
            val length = connection.contentLengthLong
            if (length > maxBytes) throw SchedulePlusUpdateException("更新服务返回的数据过大")
            connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > maxBytes) throw SchedulePlusUpdateException("更新服务返回的数据过大")
                    output.write(buffer, 0, count)
                }
                return output.toString(Charsets.UTF_8.name())
            }
        } catch (error: Exception) {
            if (error is SchedulePlusUpdateException) throw error
            throw SchedulePlusUpdateException("连接自有 GitHub Release 失败：${error.message ?: "网络错误"}", error)
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String): HttpURLConnection {
        val connection = connectionFactory(URL(url))
        connection.requestMethod = "GET"
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "SchedulePlus-Android")
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        return connection
    }

    private fun httpFailure(status: Int) = SchedulePlusUpdateException("GitHub 更新服务返回 HTTP $status")

    private fun parseReleaseList(text: String): List<GitHubRelease> {
        val releases = JSONArray(text)
        return buildList(releases.length()) {
            for (index in 0 until releases.length()) add(parseRelease(releases.getJSONObject(index)))
        }
    }

    private fun parseRelease(json: JSONObject): GitHubRelease {
        val assetsJson = json.optJSONArray("assets") ?: JSONArray()
        val assets = buildList(assetsJson.length()) {
            for (index in 0 until assetsJson.length()) {
                val asset = assetsJson.getJSONObject(index)
                add(
                    GitHubReleaseAsset(
                        name = asset.getString("name"),
                        url = asset.getString("browser_download_url"),
                        digest = asset.optString("digest").takeIf(String::isNotBlank)
                    )
                )
            }
        }
        return GitHubRelease(
            tagName = json.getString("tag_name"),
            isDraft = json.optBoolean("draft", false),
            isPrerelease = json.optBoolean("prerelease", false),
            htmlUrl = json.getString("html_url"),
            body = json.optString("body").take(MAX_RELEASE_NOTES_CHARS),
            publishedAt = json.optString("published_at"),
            assets = assets
        )
    }

    companion object {
        const val OWNER = "linkasz"
        const val REPOSITORY = "-"
        const val MANIFEST_ASSET_NAME = "scheduleplus-update.json"
        const val APK_ASSET_NAME = "app-github-release.apk"
        const val APPLICATION_ID = "com.scheduleplus.student"
        const val MAX_APK_BYTES = 200L * 1024L * 1024L
        private const val API_ROOT = "https://api.github.com/repos/$OWNER/$REPOSITORY"
        private const val REPOSITORY_PATH = "/$OWNER/$REPOSITORY"
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 30_000
        private const val RELEASE_JSON_LIMIT = 1 * 1024 * 1024
        private const val RELEASE_LIST_LIMIT = 4 * 1024 * 1024
        private const val MANIFEST_JSON_LIMIT = 64 * 1024
        private const val MAX_RELEASE_NOTES_CHARS = 12_000
        private val VERSION_NAME = Regex("\\d+\\.\\d+\\.\\d+(?:_beta\\d+)?")
        private val SHA256 = Regex("[0-9a-f]{64}")

        internal fun isOwnedReleaseAssetUrl(url: String, assetName: String): Boolean = runCatching {
            val uri = URI(url)
            uri.scheme == "https" && uri.host.equals("github.com", ignoreCase = true) &&
                uri.userInfo == null && uri.port in setOf(-1, 443) && uri.query == null && uri.fragment == null &&
                uri.path.startsWith("$REPOSITORY_PATH/releases/download/") && uri.path.endsWith("/$assetName")
        }.getOrDefault(false)

        internal fun isOwnedReleasePageUrl(url: String, tagName: String): Boolean = runCatching {
            val uri = URI(url)
            uri.scheme == "https" && uri.host.equals("github.com", ignoreCase = true) &&
                uri.userInfo == null && uri.port in setOf(-1, 443) && uri.query == null && uri.fragment == null &&
                uri.path == "$REPOSITORY_PATH/releases/tag/$tagName"
        }.getOrDefault(false)

        internal fun parseUpdateManifest(
            raw: String,
            release: GitHubRelease,
            expectedApplicationId: String
        ): SchedulePlusUpdate {
            val manifestJson = JSONObject(raw)
            val manifestAsset = release.assets.singleOrNull { it.name == MANIFEST_ASSET_NAME }
                ?: throw SchedulePlusUpdateException("此 Release 缺少 $MANIFEST_ASSET_NAME")
            val apkAsset = release.assets.singleOrNull { it.name == APK_ASSET_NAME }
                ?: throw SchedulePlusUpdateException("此 Release 缺少 $APK_ASSET_NAME")
            val applicationId = manifestJson.getString("applicationId")
            val versionName = manifestJson.getString("versionName")
            val versionCode = manifestJson.getLong("versionCode")
            val apkUrl = manifestJson.getString("apkUrl")
            val sha256 = manifestJson.getString("sha256").lowercase(Locale.ROOT)
            val releaseUrl = manifestJson.getString("releaseUrl")

            if (!isOwnedReleaseAssetUrl(apkAsset.url, APK_ASSET_NAME)) {
                throw SchedulePlusUpdateException("Release 资产链接不属于时序清单发布仓库")
            }
            if (applicationId != expectedApplicationId) throw SchedulePlusUpdateException("更新包 applicationId 不匹配")
            if (!VERSION_NAME.matches(versionName) || release.tagName != "v$versionName" || versionCode <= 0L) {
                throw SchedulePlusUpdateException("更新清单中的版本信息无效")
            }
            if (release.isPrerelease != versionName.contains("_beta")) {
                throw SchedulePlusUpdateException("Release 类型与版本号不一致")
            }
            if (apkUrl != apkAsset.url || !isOwnedReleaseAssetUrl(apkUrl, APK_ASSET_NAME)) {
                throw SchedulePlusUpdateException("更新 APK 地址与 Release 资产不匹配")
            }
            if (releaseUrl != release.htmlUrl || !isOwnedReleasePageUrl(releaseUrl, release.tagName)) {
                throw SchedulePlusUpdateException("Release 页面地址与发布版本不匹配")
            }
            if (!SHA256.matches(sha256)) throw SchedulePlusUpdateException("更新清单中的 SHA-256 无效")
            apkAsset.digest?.takeIf { it.startsWith("sha256:", ignoreCase = true) }?.let { apiDigest ->
                if (apiDigest.substringAfter(':').lowercase(Locale.ROOT) != sha256) {
                    throw SchedulePlusUpdateException("Release 资产摘要与更新清单不匹配")
                }
            }

            return SchedulePlusUpdate(
                applicationId = applicationId,
                versionName = versionName,
                versionCode = versionCode,
                apkUrl = apkUrl,
                sha256 = sha256,
                releaseUrl = releaseUrl,
                releaseNotes = release.body,
                isPrerelease = release.isPrerelease
            )
        }

        internal fun sha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().buffered().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            return digest.digest().toHexString()
        }

        private fun openConnection(url: URL): HttpURLConnection = url.openConnection() as HttpURLConnection

        private fun ByteArray.toHexString(): String = joinToString("") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }
    }
}

internal data class GitHubRelease(
    val tagName: String,
    val isDraft: Boolean,
    val isPrerelease: Boolean,
    val htmlUrl: String,
    val body: String,
    val publishedAt: String,
    val assets: List<GitHubReleaseAsset>
)

internal data class GitHubReleaseAsset(
    val name: String,
    val url: String,
    val digest: String?
)
