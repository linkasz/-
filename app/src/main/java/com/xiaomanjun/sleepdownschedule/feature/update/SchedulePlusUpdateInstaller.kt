package com.xiaomanjun.sleepdownschedule.feature.update

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

internal object SchedulePlusUpdateInstaller {
    fun install(context: Context, update: SchedulePlusUpdate, apk: File): InstallRequestResult {
        require(apk.isFile) { "没有找到已下载的更新文件" }
        val archive = context.packageManager.getPackageArchiveInfo(apk.absolutePath, 0)
            ?: error("更新文件不是有效的 Android 安装包")
        require(archive.packageName == context.packageName) { "更新包 applicationId 不匹配" }
        require(archive.versionName == update.versionName) { "APK 版本名称与更新清单不匹配" }
        val archiveVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            archive.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            archive.versionCode.toLong()
        }
        require(archiveVersionCode == update.versionCode) { "APK 版本与更新清单不匹配" }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return InstallRequestResult.PERMISSION_REQUIRED
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MIME_TYPE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            clipData = ClipData.newRawUri("SchedulePlus update", uri)
        }
        context.startActivity(installIntent)
        return InstallRequestResult.INSTALLER_OPENED
    }

    private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
}

internal enum class InstallRequestResult { PERMISSION_REQUIRED, INSTALLER_OPENED }
