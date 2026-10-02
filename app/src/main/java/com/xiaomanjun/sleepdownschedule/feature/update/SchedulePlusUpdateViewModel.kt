package com.xiaomanjun.sleepdownschedule.feature.update

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiaomanjun.sleepdownschedule.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class SchedulePlusUpdateUiState(
    val channel: SchedulePlusUpdateChannel = SchedulePlusUpdateChannel.STABLE,
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val message: String = "手动检查时序清单的 GitHub Release",
    val update: SchedulePlusUpdate? = null,
    val downloadedApkPath: String? = null
)

internal class SchedulePlusUpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SchedulePlusUpdateRepository(application.cacheDir)
    private val preferences = application.getSharedPreferences(PREFERENCES, Application.MODE_PRIVATE)
    private val mutableState = MutableStateFlow(
        SchedulePlusUpdateUiState(
            channel = if (preferences.getBoolean(KEY_BETA, false)) {
                SchedulePlusUpdateChannel.BETA
            } else {
                SchedulePlusUpdateChannel.STABLE
            }
        )
    )
    val state = mutableState.asStateFlow()

    fun setBetaChannel(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_BETA, enabled).apply()
        mutableState.value = SchedulePlusUpdateUiState(
            channel = if (enabled) SchedulePlusUpdateChannel.BETA else SchedulePlusUpdateChannel.STABLE,
            message = if (enabled) "已切换到 Beta 频道；点击检查更新" else "已切换到正式版频道；点击检查更新"
        )
    }

    fun checkForUpdates() {
        if (mutableState.value.isChecking || mutableState.value.isDownloading) return
        val channel = mutableState.value.channel
        mutableState.value = mutableState.value.copy(
            isChecking = true,
            message = "正在检查更新…",
            update = null,
            downloadedApkPath = null
        )
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.check(channel, BuildConfig.APPLICATION_ID, BuildConfig.VERSION_CODE.toLong())
                }
                mutableState.value = mutableState.value.copy(
                    isChecking = false,
                    message = result.message,
                    update = result.update
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(
                    isChecking = false,
                    message = error.message ?: "检查更新失败，请稍后重试",
                    update = null
                )
            }
        }
    }

    fun downloadUpdate() {
        val update = mutableState.value.update ?: return
        if (mutableState.value.isDownloading) return
        mutableState.value = mutableState.value.copy(isDownloading = true, message = "正在下载并校验更新…")
        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) { repository.download(update) }
                mutableState.value = mutableState.value.copy(
                    isDownloading = false,
                    message = "更新已下载并通过 SHA-256 校验",
                    downloadedApkPath = file.absolutePath
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(
                    isDownloading = false,
                    message = error.message ?: "下载更新失败，请稍后重试",
                    downloadedApkPath = null
                )
            }
        }
    }

    companion object {
        private const val PREFERENCES = "schedule_plus_updates"
        private const val KEY_BETA = "beta_channel"
    }
}
