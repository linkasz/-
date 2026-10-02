# 时序清单

> **基于 SleepDown课程表修改；原作者：xiaomanjun233；原项目：[SleepDown-Schedule](https://github.com/xiaomanjun233/SleepDown-Schedule)；非官方修改版。**

时序清单是一款本地优先的 Android 课程表与待办应用，提供教务导入、液态玻璃界面、任务管理、月历与效率洞察。基础数据管理无需注册账号。

## 功能

- 多课表、单双周课程、手动编辑、教务导入、今日课程提醒和桌面小组件。
- 待办增删改、分组、置顶、优先级、截止时间、重复任务、子任务、课程关联和今日待办桌面小组件。
- 从文字或图片提取任务；支持 Android 分享文本和图片。
- 月历汇总课程与待办、选日详情、Android 系统日历写入与自动同步选项。
- 完成率、工作量分布和生产力趋势概览。
- `.shixu` 备份与恢复；兼容导入旧备份，保留 BackupFormatV1 数据结构。
- 智能助手支持按日期查询本机课程与待办、确认创建待办、对话历史及可配置的语音转写和朗读。
- Compose、Room、MVVM、Hilt、Miuix 与 `io.github.kyant0:backdrop` 液态玻璃效果。

最低系统版本为 Android 8.0（API 26）。Android applicationId 为 `com.scheduleplus.student`。

## 构建

使用 Android Studio 打开本仓库，安装项目所需 Android SDK，并选择可用的 Gradle JDK。命令行可运行：

```powershell
./gradlew.bat :app:assembleGithubDebug
```

仓库包含 Apache 2.0 许可的 Miuix v0.9.3 源码，并应用 `patches/` 中的三份界面适配补丁；干净检出即可构建，无需配置仓库外的 Miuix 路径。需要切换为另一份已应用同版补丁的源码时，可传入 `-Psleepdown.miuixSourcePath=<源码目录>`。Release 签名密钥不应提交到源码仓库。

## 许可证与来源

本项目保留上游 `LICENSE.md` 和 `THIRD_PARTY_NOTICES.md`。SleepDown 采用署名、非商业、源码可见许可；该许可不是 OSI 定义的开源许可证。请阅读 [LICENSE.md](LICENSE.md)，并为任何公开分发同时提供与所发布版本对应的完整源代码。

发布修改版时，Release 页面必须显著包含以下声明，并同时附上对应源码与更新清单：

> 基于 SleepDown课程表修改；原作者：xiaomanjun233；原项目：[https://github.com/xiaomanjun233/SleepDown-Schedule](https://github.com/xiaomanjun233/SleepDown-Schedule)；非官方修改版。修改版完整源代码：[linkasz/-](https://github.com/linkasz/-)。

可使用 [Release 页面模板](.github/release-template.md)。

感谢 SleepDown-Schedule 原作者 xiaomanjun233 的项目与贡献：[SleepDown-Schedule](https://github.com/xiaomanjun233/SleepDown-Schedule)。
# 1.0.2 公开构建说明

本仓库的来源人格使用空核心占位，保留可见风格、工具与用户确认链路；与保留真实核心的本地完整版存在人格表现差异。详见 [公开人格策略](docs/release/PUBLIC_PERSONA_POLICY.md)。
