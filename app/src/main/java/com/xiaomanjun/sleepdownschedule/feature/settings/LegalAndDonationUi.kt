package com.xiaomanjun.sleepdownschedule.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.xiaomanjun.sleepdownschedule.AppState
import com.xiaomanjun.sleepdownschedule.app.ui.DockScrollPadding

private data class PrivacyPolicySection(val title: String, val body: String)

private val PrivacyPolicySections = listOf(
    PrivacyPolicySection(
        "一、应用与处理原则",
        "时序清单用于管理课程、待办与日常安排。我们尽量在本机处理数据，只在你主动使用相应功能时访问所需的系统服务或你配置的第三方服务。"
    ),
    PrivacyPolicySection(
        "二、本机保存的数据",
        "课程、待办事项、子任务、分组、提醒设置、壁纸取景和应用偏好保存在应用私有存储及本地 Room 数据库中。API Key 保存在应用私有偏好设置，不写入 .shixu 备份。卸载应用或清除数据可能删除本机内容；重要数据请定期导出备份。"
    ),
    PrivacyPolicySection(
        "三、AI 任务提取",
        "AI 任务提取默认关闭。你填写 API URL、API Key 并主动提交文本或选择图片后，所选内容会发送到该 API 对应的模型服务商。服务商如何留存和处理内容受其隐私政策与服务条款约束。不要提交你无权分享或不希望发送给第三方的内容。"
    ),
    PrivacyPolicySection(
        "四、系统日历",
        "开启系统日历同步并授予日历权限后，应用会读取可写日历列表，并将有截止时间的未完成待办写入你设备上的日历。应用不会为此读取其他日历事件内容。你可以在 Android 设置中随时撤回权限。"
    ),
    PrivacyPolicySection(
        "五、其他权限",
        "通知、闹钟和开机启动权限用于按你设置的时间提醒课程或待办；文件和照片选择器只用于读取你主动选择的文件或图片。基础课程表和待办管理不要求注册账号。"
    ),
    PrivacyPolicySection(
        "六、备份与数据控制",
        "你可以在应用内创建或恢复 .shixu 备份。备份文件由你选择保存位置并自行保管；通过系统分享或模型 API 发送的内容由相应接收方处理。你可以编辑或删除本机保存的数据，也可以在系统设置中撤回可选权限。"
    )
)

@Composable
fun PrivacyPolicySettingsScreen(state: AppState, backdrop: Backdrop?) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = DockScrollPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SettingsGroup(backdrop = backdrop, config = state.config, modifier = Modifier.fillMaxWidth()) {
                SettingsInfoRow(
                    "时序清单隐私说明",
                    "课程与待办默认留在本机。仅当你主动使用 AI 提取、系统分享或日历同步时，相关数据才会按该功能说明交给你选择的服务。"
                )
            }
        }
        PrivacyPolicySections.forEach { section ->
            item(key = "privacy-${section.title}") {
                SettingsGroup(backdrop = backdrop, config = state.config, modifier = Modifier.fillMaxWidth()) {
                    SettingsInfoRow(section.title, section.body)
                }
            }
        }
    }
}
