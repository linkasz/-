package com.xiaomanjun.sleepdownschedule.feature.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulePlusUpdateManifestTest {
    @Test
    fun acceptsManifestForTheReleaseApkAndKeepsReleaseNotes() {
        val update = SchedulePlusUpdateRepository.parseUpdateManifest(
            manifest(),
            stableRelease(),
            SchedulePlusUpdateRepository.APPLICATION_ID
        )

        assertEquals("1.0.1", update.versionName)
        assertEquals(36L, update.versionCode)
        assertEquals(NOTE, update.releaseNotes)
        assertFalse(update.isPrerelease)
    }

    @Test
    fun rejectsPackageUrlAndVersionMismatch() {
        assertThrows(SchedulePlusUpdateException::class.java) {
            SchedulePlusUpdateRepository.parseUpdateManifest(
                manifest(applicationId = "com.xiaomanjun.sleepdownschedule"),
                stableRelease(),
                SchedulePlusUpdateRepository.APPLICATION_ID
            )
        }
        assertThrows(SchedulePlusUpdateException::class.java) {
            SchedulePlusUpdateRepository.parseUpdateManifest(
                manifest(versionName = "1.0.2"),
                stableRelease(),
                SchedulePlusUpdateRepository.APPLICATION_ID
            )
        }
    }

    @Test
    fun rejectsLookalikeHostsAndAssetsWithDifferentSha256() {
        val attackerUrl = "https://github.com.evil.example/${SchedulePlusUpdateRepository.OWNER}/" +
            "${SchedulePlusUpdateRepository.REPOSITORY}/releases/download/v1.0.1/" +
            SchedulePlusUpdateRepository.APK_ASSET_NAME
        assertFalse(
            SchedulePlusUpdateRepository.isOwnedReleaseAssetUrl(
                attackerUrl,
                SchedulePlusUpdateRepository.APK_ASSET_NAME
            )
        )

        val wrongDigestRelease = stableRelease(
            apkDigest = "sha256:${"b".repeat(64)}"
        )
        assertThrows(SchedulePlusUpdateException::class.java) {
            SchedulePlusUpdateRepository.parseUpdateManifest(
                manifest(),
                wrongDigestRelease,
                SchedulePlusUpdateRepository.APPLICATION_ID
            )
        }
    }

    @Test
    fun betaManifestMustMatchPrereleaseTagAndStatus() {
        val beta = release(
            tag = "v1.0.1_beta1",
            prerelease = true,
            apkDigest = "sha256:$HASH"
        )
        val parsed = SchedulePlusUpdateRepository.parseUpdateManifest(
            manifest(versionName = "1.0.1_beta1", versionCode = 37),
            beta,
            SchedulePlusUpdateRepository.APPLICATION_ID
        )
        assertTrue(parsed.isPrerelease)
        assertEquals(37L, parsed.versionCode)

        assertThrows(SchedulePlusUpdateException::class.java) {
            SchedulePlusUpdateRepository.parseUpdateManifest(
                manifest(versionName = "1.0.1_beta1", versionCode = 37),
                stableRelease(),
                SchedulePlusUpdateRepository.APPLICATION_ID
            )
        }
    }

    private fun manifest(
        applicationId: String = SchedulePlusUpdateRepository.APPLICATION_ID,
        versionName: String = "1.0.1",
        versionCode: Int = 36
    ): String {
        val tag = "v$versionName"
        val root = "https://github.com/${SchedulePlusUpdateRepository.OWNER}/" +
            "${SchedulePlusUpdateRepository.REPOSITORY}/releases"
        return """
            {
              "applicationId":"$applicationId",
              "versionName":"$versionName",
              "versionCode":$versionCode,
              "apkUrl":"$root/download/$tag/${SchedulePlusUpdateRepository.APK_ASSET_NAME}",
              "sha256":"$HASH",
              "releaseUrl":"$root/tag/$tag"
            }
        """.trimIndent()
    }

    private fun stableRelease(apkDigest: String = "sha256:$HASH") = release(
        tag = "v1.0.1",
        prerelease = false,
        apkDigest = apkDigest
    )

    private fun release(tag: String, prerelease: Boolean, apkDigest: String): GitHubRelease {
        val root = "https://github.com/${SchedulePlusUpdateRepository.OWNER}/" +
            "${SchedulePlusUpdateRepository.REPOSITORY}/releases"
        return GitHubRelease(
            tagName = tag,
            isDraft = false,
            isPrerelease = prerelease,
            htmlUrl = "$root/tag/$tag",
            body = NOTE,
            publishedAt = "2026-09-27T10:00:00Z",
            assets = listOf(
                GitHubReleaseAsset(
                    SchedulePlusUpdateRepository.MANIFEST_ASSET_NAME,
                    "$root/download/$tag/${SchedulePlusUpdateRepository.MANIFEST_ASSET_NAME}",
                    null
                ),
                GitHubReleaseAsset(
                    SchedulePlusUpdateRepository.APK_ASSET_NAME,
                    "$root/download/$tag/${SchedulePlusUpdateRepository.APK_ASSET_NAME}",
                    apkDigest
                )
            )
        )
    }

    private companion object {
        const val HASH = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        const val NOTE = "新增课表+自有更新源。"
    }
}
