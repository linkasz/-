# 版本基线与发布关系

本文说明当前维护方式；构建配置是当前版本事实来源。基线快照核对于 **2026-10-03**。

## 当前基线

| 项目 | 当前事实 | 核对入口 |
| --- | --- | --- |
| 持续开发分支 | `main`；本轮同步候选为 `codex/release-1.0.2`，尚未合并或发布 | `app/build.gradle.kts` 与发布检查 |
| Android 主应用 | `com.scheduleplus.student`，`versionName=1.0.2`，`versionCode=37`，Room 45 | `app/build.gradle.kts`、`AppDatabase.kt` |
| 发行渠道 | `github` 与 `store` 两种 flavor；厂商实验入口由 `SLEEPDOWN_EXPERIMENTAL_FEATURES` 按渠道控制 | `app/build.gradle.kts`、`app/src/github/`、`app/src/store/` |
| 课程组件 | 独立包 `com.suda.yzune.wakeupschedule`；构建版本 `6.0.18` / `258` | `coloros-wakeup-proxy/build.gradle.kts` |
| 历史实验线 | 旧 Git 提交保留；旧公开 Release 与版本标签按本轮收口清理 | Git 提交历史 |
| 历史集成线 | `develop` 不再是新功能或外部 PR 的默认目标 | 当前维护流程 |

当前版本保留 GitHub 与商店两种 flavor；厂商实验入口由 `SLEEPDOWN_EXPERIMENTAL_FEATURES` 按渠道控制。实际可用性还受设备、系统、授权和组件状态限制。详见[功能地图](FEATURE_MAP.md)和[历史实验线记录](EXP_BRANCH_AND_RELEASES.md)。

## 正式版、Beta 与实验功能

历史 Release 页面和旧版本附件按本轮发布收口要求清理；Git 提交历史保留。当前源码中的面向用户更新说明只保留本次发布记录，QA、迁移与工程报告仍按原路径留档。

| 类型 | 命名和标签 | 代码来源 | 更新日志与发布页 |
| --- | --- | --- | --- |
| 正式版 | `1.0.2` / `v1.0.2` | `main` 上经过发布验证的提交 | 正式版，附 APK、更新清单和完整源码链接 |
| 普通 Beta | `1.0.3_betaN` / 对应 tag | 同一 `main` 基线的发布候选 | 用户主动启用 Beta 后才检查预发布版本 |
| 厂商实验功能 | 无独立版本后缀 | 当前 `main` 的隔离实现 | 随对应正式版或 Beta 交付，在说明中标明实验状态与设备条件 |

升级前核对相同 applicationId、兼容签名及递增的 versionCode；不能仅凭版本名称判断。旧身份 `com.example.courseschedule` 不能直接覆盖安装当前包，需先在旧版导出 `.sleepdown` 再恢复，见[迁移说明](migration/1_2_0_PACKAGE_MIGRATION.md)。

1.0.2 的本地完整版保留私有人格核心；公开源码与公开 APK 使用空核心占位。两者包名、版本和签名一致，人格行为可能不同；只将公开版 APK 与对应更新清单用于 GitHub 分发。

## 一轮发布怎样收口

1. 日常功能和修复从新的 `main` 派生短期分支，验证后进入 `main`；不从历史 `develop` 或 `exp` 派生新普通功能。
2. Beta 只记录本次实际交付的用户可感知变化、标签、构建与验收。预发布不会取代最新正式版。
3. 每轮只在应用与对应 Release 保留当前版本更新说明；QA、迁移和工程报告不作为应用内版本日志展示。
4. 发布前检查 Git 提交与标签、`versionName`/`versionCode`、包名、签名、渠道开关、主 APK 与组件 APK 身份；上传后从公开地址回读大小及 SHA-256。
5. 已发布标签、附件和说明不通过覆盖来“修好旧版本”；修订使用新 Beta、正式补丁版或明确的新标签。

构建、安装与专项验收入口见根目录 [AGENTS.md](../AGENTS.md)。Git 工作方式见[开发工作流](DEVELOPMENT_WORKFLOW.md)。具体历史记录在 `release-notes/` 与带日期的 `docs/` 报告中；历史记录不自动定义当前功能状态。
