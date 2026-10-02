# 设置闪退与待办界面问题排查记录

日期：2026-09-28  
设备：Vivo V2352A，Android 16 / API 36，1260 × 2800，字体缩放 1.0  
分支：`codex/calendar-sync-orphan-recovery`

## 已定位问题

| 编号 | 影响 | 根因与处理 | 当前验收状态 |
| --- | --- | --- | --- |
| SET-001 | 点击底部“设置”后应用闪退 | 真机复现栈为 `IllegalArgumentException: Only VectorDrawables and rasterized asset types are supported`，发生在 `SettingsRootScreen` 对 `currentIconResId()` 结果调用 Compose `painterResource()`。该资源是自适应启动器图标 XML，Compose painter 不支持此类型。设置页现改为通过 Android `Drawable` / `ImageView` 显示图标。 | 已在真机复现并确认根因；修复已进入源码。安装被系统拒绝，修复后的设备回归待用户允许 USB 安装后完成。 |
| TODO-001 | 分组、课程、重复规则和任务菜单可能偏离触发控件 | 调用处原先捕获根坐标，而 Popup 定位器没有使用传入锚点。调用处改为 `boundsInWindow()`；公共定位器现在优先使用该锚点，按窗口边界对齐，顶部空间不足时翻转到控件下方。 | 定位计算定向测试 3/3 通过；真机菜单位置待验收。 |
| NAV-001 | 课程与待办曾各自提供页面 Activity / Dock，入口导航不统一 | 待办列表、日历、洞察已嵌入 `MainActivity` 的 Compose 主导航。单 Dock 提供课程、待办、日历、洞察、设置五个目的地；分享过滤器、通知、组件、快捷磁贴、今日助手和课程详情入口统一路由到主 Activity。旧 `TodoActivity` 仅保留无 UI 兼容转发。 | Release Kotlin 编译、4 项定向单测和 Debug APK 组装通过。真机覆盖安装被设备拒绝，实际页面导航尚未验收。 |
| NAV-002 | 从课程编辑器进入待办后，系统返回可能关闭隐藏的课程编辑器；待办草稿在切换到设置或旋转时可能丢失 | 课程编辑器宿主在挂起期间禁用自身返回处理器；待办编辑草稿及课程入口请求加入 Compose 可保存状态，编辑字段变更同步到草稿状态。 | Release Kotlin 编译、4 项定向单测和 Debug APK 组装通过；设备安装被拒，返回与旋转行为待真机验收。 |

## 本轮验证

- `:app:compileGithubReleaseKotlin`：包含草稿恢复和挂起编辑器返回修复的统一导航候选源码通过。
- `:app:testGithubDebugUnitTest --tests MainNavigationTest --tests GlassMiuixPopupPositionTest`：4 项通过（Dock 五目的地顺序 1 项、Popup 坐标定位 3 项）。
- `:app:assembleGithubDebug`：通过，生成 `app/build/outputs/apk/github/debug/app-github-debug.apk`（约 33.3 MB）。
- Vivo V2352A / Android 16 / API 36 已通过 ADB 授权连接；覆盖安装返回 `INSTALL_FAILED_ABORTED: User rejected permissions`。未卸载旧版、未清除应用数据，也未尝试绕过系统安装确认。
- 本轮没有执行 `:app:assembleGithubRelease`，也没有做设备页面操作；因安装未获设备确认，统一 Dock、设置页、待办编辑和分享入口都不能记录为真机通过。

## 全项目验收缺口

本轮检查设置入口、Popup 定位和统一主导航链路。以下项目保留在 [发布验收清单](RELEASE_ACCEPTANCE.md)，本轮没有完成端到端或设备验收，因此不据此认定功能缺失或正常：

- 待办重复规则、父子任务、分组、课程关联及删除/撤销边界。
- AI 文字、单图/多图分享、模型失败恢复与 Shizuku 权限路径。
- 月/周日历、系统日历手动/自动幂等同步、权限撤回后孤儿事件清理。
- 课程/待办时间轴、通知点击定位和提醒去重。
- 洞察、Widget、多窗口尺寸与字体缩放。
- 静态、实况、视频壁纸生命周期；旧数据迁移及 BackupFormatV1 往返。
- Release 更新器的正式版/Beta 来源、摘要校验和 Android 安装确认。

## 后续顺序

1. 用户在设备允许 USB 安装后覆盖安装最终 Debug 包；回归设置首次进入、返回和连续切换五个 Dock 目的地，并检查待办页面状态、菜单锚点与编辑返回课程页。只用现有数据浏览，不创建或删除真实任务。
2. 按 [发布验收清单](RELEASE_ACCEPTANCE.md) 逐块实测未验收流程；API 26 设备和平板需另行记录，不能用 API 36 手机结果替代。
3. 配置既有发布签名后再打包 Release；签名材料保留在仓库外。

本轮未改 Room schema、`BackupFormatV1`、包名或系统组件身份；未推送、发布或清理工作树。
