package com.xiaomanjun.sleepdownschedule.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.kyant.backdrop.Backdrop
import com.xiaomanjun.sleepdownschedule.BuildConfig
import com.xiaomanjun.sleepdownschedule.R
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LiquidAlertAction
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LiquidAlertActionStyle
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LiquidAlertDialog
import com.xiaomanjun.sleepdownschedule.feature.coloros.ColorOSCourseBridge
import com.xiaomanjun.sleepdownschedule.feature.coloros.ColorOSCourseContract
import com.xiaomanjun.sleepdownschedule.feature.coloros.ColorOSCourseDiagnostics
import com.xiaomanjun.sleepdownschedule.feature.coloros.ColorOSCourseExperiment
import com.xiaomanjun.sleepdownschedule.model.AppState
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun ColorOSCourseSettingsSection(
    state: AppState,
    backdrop: Backdrop?
) {
    if (!BuildConfig.SLEEPDOWN_EXPERIMENTAL_FEATURES) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var diagnostics by remember { mutableStateOf<ColorOSCourseDiagnostics?>(null) }
    var showDiagnostics by remember { mutableStateOf(false) }

    fun reload(showWhenReady: Boolean = false) {
        scope.launch {
            diagnostics = ColorOSCourseExperiment.diagnose(context)
            if (showWhenReady) showDiagnostics = true
        }
    }

    LaunchedEffect(Unit) {
        diagnostics = ColorOSCourseExperiment.diagnose(context)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        reload()
    }

    val current = diagnostics
    val isHonor = current?.device?.isHonor ?: ColorOSCourseExperiment.deviceStatus().isHonor
    GlassPreferenceSection("实验功能") {
        SettingsGroup(
            backdrop = backdrop,
            config = state.config,
            modifier = Modifier.fillMaxWidth()
        ) {
            SettingsInfoRow(
                title = "使用前准备",
                body = if (isHonor) {
                    "YOYO 建议适用于荣耀 MagicOS。第一次使用请先安装课程组件。"
                } else {
                    "课程流体云适用于 OPPO、一加和 realme 的 ColorOS 系统。第一次使用请先安装课程组件。"
                }
            )
            SettingsDivider()
            SettingsInfoRow(
                title = if (isHonor) "YOYO 建议配置" else "流体云配置",
                body = if (isHonor) {
                    "1. 安装课程组件，在系统的 YOYO 建议和通知设置中允许课程提醒。\n" +
                        "2. 在应用启动管理中为“WakeUp课程表”打开自启动、关联启动和后台运行。\n" +
                        "3. 回到本页重新同步，再用底部的测试按钮检查效果。测试课程约 21～22 分钟后开始，持续 5 分钟。\n" +
                        "4. 若仍未显示，检查系统是否限制组件运行；部分荣耀机型还会校验组件签名或应用特征。"
                } else {
                    "1. 安装课程组件，打开系统的流体云总开关。\n" +
                        "2. 在应用启动管理中为“WakeUp课程表”打开自启动、关联启动和后台运行。\n" +
                        "3. 回到本页重新同步，再用底部的测试按钮检查效果。测试课程约 21～22 分钟后开始，持续 5 分钟。\n" +
                        "4. 若仍未显示，检查系统通知权限和组件后台限制。"
                }
            )
            SettingsDivider()
            SettingsInfoRow(
                title = "后台说明",
                body = (if (ColorOSCourseExperiment.allowsParallelLiveUpdate(context)) {
                    if (isHonor) "当前模式会同时使用课程组件与 SleepDown 实时活动。"
                    else "课前提醒由流体云显示，实时活动不再重复发送课前提醒；课中、课间及次日课程等其余提醒继续由实时活动显示。"
                } else "") +
                    "课程组件会保存已同步的课程快照，重启解锁或组件更新后通知系统重新读取。" +
                    "请在系统设置中允许“WakeUp课程表”自启动、关联启动和后台运行；自启动不代表系统不会冻结后台应用。" +
                    "若强行停止 SleepDown 或课程组件，请重新打开应用并同步。"
            )
            if (current?.proxyIsSleepDown == true) {
                SettingsDivider()
                SettingsActionRow(
                    title = "组件启动权限",
                    subtitle = "为“WakeUp课程表”打开自启动和关联启动。",
                    buttonText = "设置",
                    iconRes = R.drawable.ic_settings,
                    backdrop = backdrop,
                    onClick = { openCourseComponentSettings(context, startup = true) }
                )
                SettingsDivider()
                SettingsActionRow(
                    title = "组件后台运行",
                    subtitle = "打开“WakeUp课程表”的应用信息，检查电池与后台限制。",
                    buttonText = "设置",
                    iconRes = R.drawable.ic_settings,
                    backdrop = backdrop,
                    onClick = { openCourseComponentSettings(context, startup = false) }
                )
            }
            SettingsDivider()
            SettingsValueRow("设备支持", current?.device?.let { if (it.isColorOSFamily) "支持" else "不支持" } ?: "检测中…")
            SettingsDivider()
            SettingsValueRow("课程组件", current?.let {
                when {
                    it.proxyIsSleepDown && it.proxyVersionSupported -> "已安装 · ${it.proxyVersionName}"
                    it.proxyIsSleepDown -> "需要更新 · ${it.proxyVersionName}"
                    it.proxyInstalled -> "与已安装的 WakeUp 课程表冲突"
                    else -> "未安装"
                }
            } ?: "检测中…")
            SettingsDivider()
            SettingsValueRow("课程读取", current?.let {
                if (it.exportValid) "正常 · 今天 ${it.todayCourseCount} 门，明天 ${it.tomorrowCourseCount} 门" else "异常"
            } ?: "检测中…")
            SettingsDivider()
            SettingsValueRow("系统读取", current?.let {
                if (it.lastSystemQueryAt > 0) {
                    "最近一次 ${it.lastSystemQueryAt.toDisplayTime()}"
                } else {
                    "等待系统读取"
                }
            } ?: "检测中…")
            SettingsDivider()
            SettingsActionRow(
                title = "重新同步",
                subtitle = "让系统重新读取当前课程。",
                buttonText = "同步",
                iconRes = R.drawable.ic_refresh,
                backdrop = backdrop,
                onClick = {
                    ColorOSCourseExperiment.synchronizeFromUserAction(context)
                    reload()
                }
            )
            SettingsDivider()
            SettingsActionRow(
                title = "问题诊断",
                subtitle = "查看设备、组件和课程读取状态。",
                buttonText = "查看",
                iconRes = R.drawable.ic_settings,
                backdrop = backdrop,
                onClick = { reload(showWhenReady = true) }
            )
        }
    }

    if (showDiagnostics && current != null) {
        LiquidAlertDialog(
            title = if (isHonor) "YOYO 建议诊断" else "课程流体云诊断",
            message = current.asText(),
            actions = listOf(
                LiquidAlertAction("完成", LiquidAlertActionStyle.Primary) {
                    showDiagnostics = false
                }
            ),
            backdrop = backdrop,
            config = state.config,
            onDismissRequest = { showDiagnostics = false }
        )
    }

}

private fun openCourseComponentSettings(context: Context, startup: Boolean) {
    if (startup) {
        // This entry is exposed by current ColorOS. Other systems use app details.
        val opened = runCatching {
            context.startActivity(
                Intent("com.oplus.battery.permission.startup.StartupAppListActivity")
                    .setPackage("com.oplus.battery")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.isSuccess
        if (opened) return
    }
    context.startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${ColorOSCourseContract.PROXY_PACKAGE}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

private fun Long?.toDisplayTime(): String {
    val timestamp = this?.takeIf { it > 0 } ?: return "无"
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MM-dd HH:mm:ss"))
}
