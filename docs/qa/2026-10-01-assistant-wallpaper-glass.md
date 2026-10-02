# AI 浮窗壁纸配色与分层玻璃

## 本轮范围与原因

用户截图中输入胶囊按应用浅色主题选白底，麦克风却按课程壁纸规则选深色材质；图标又读取浅色主题的深色前景，导致右侧按钮接近不可见。本轮仅收口助手浮窗、输入与语音控件，不调整设置页的独立白色弹层规则。

## 修改

- `AssistantGlassStyle.kt` 提供统一配色。使用已缓存的壁纸缩略图及现有裁切坐标、亮度计算采样顶部区域；深色选黑底白字，亮色选白底深色字。明暗切换保留阈值死区，避免亮度预览在边界反复闪烁。无壁纸时保留用户主动选择的明暗主题；初始配置仍为浅色。
- `DayAgentUi.kt`、`VoiceUi.kt` 和 `ReferenceVoiceCrown.kt` 共用该配色。输入胶囊和麦克风共用 2dp 模糊、12dp / 24dp 折射参数及黑白基础材质；麦克风增加明确描边，开启时蓝底白图标，保留 48dp 触控、长按配置和选中语义。浮窗回复及状态图标也不再误读设置主题色。
- `TopAssistantSurface.kt` 对助手启用独立的 `frostedConversation` 路线；提醒和教务岛默认仍使用原路线。
- `AssistantFrostedSurface.kt` 将同一页面 underlay 的 18dp 背景模糊作为上层磨砂，在高度 55% 到 2/3 之间平滑淡出。下层为轻模糊的液态折射材质，保留边缘高光、内阴影、颗粒与音量驱动高光。文字和交互层后绘制，不参与背景模糊；两层都采样页面，不相互采样。
- API 33+ 使用 RenderEffect 模糊和轻量 AGSL 遮罩；API 31/32 使用 RenderEffect 模糊和隔离的 DstIn 遮罩；更低版本使用可读黑白渐变，不声称具有硬件折射。模糊半径随现有质量档缩放，不增加录音或后台绘制生命周期。

液态控件沿用仓库现有 Kyant 实现和许可证，参数参考 [AndroidLiquidGlass LiquidButton](https://github.com/Kyant0/AndroidLiquidGlass/blob/kmp/app/src/commonMain/kotlin/com/kyant/backdrop/catalog/components/LiquidButton.kt)。没有引入第二套玻璃库；所有消费者通过 `glass/` 门面。

## 验证记录

样式阶段两次 `assembleGithubRelease` 成功，最终合入百炼协议修复后执行 `compileGithubReleaseKotlin :app:testGithubDebugUnitTest --tests '*feature.agent.voice.*' assembleGithubRelease generateSchedulePlusUpdateManifest exportBrandedGithubReleaseApk`，**7 分 12 秒成功**。保留 R8、资源压缩、lintVital 与签名，日志 `tmp/aliyun-voice-glass-release.log`。样式没有新增机械单测；本轮 19 项测试针对新增语音协议/断句风险，不作为外观验收。

`aapt2` 核对 `com.scheduleplus.student`、1.0.1 / 36、应用名“时序清单”、minSdk26。`apksigner verify` 成功，单签名 v2，证书 SHA-256 与原证书相同（`a6510d8496d8b3e4397bd7f30a03488db9408d38b5b4864c67375d4038924886`）。APK **6,404,160 字节**，SHA-256 **`cc75e2a550a11901409caa327bfd1993a8e2e647794839df27c0a21fe919473b`**；交付目录更新清单及 `.sha256.txt` 已按新 APK 校正。

`adb devices -l` 本轮返回空列表，未安装或启动 APK，未进行 vivo S50、iQOO Z9 或 API26 实机界面验收。没有实测帧率，不声称像素级复刻或已测得 60fps。

## 实机复验

1. 深色图片壁纸打开顶端助手：输入框与未开启麦克风都为黑色基础玻璃，文字和图标清晰；亮色图片壁纸对应白底深色前景。调整壁纸亮度及裁切，检查配色同步和阈值稳定。
2. 分别查看初始胶囊、语音声纹浮窗、带回复的浮窗与下拉展开界面。上方约 2/3 磨砂，底部约 1/3 保留壁纸细节和液态边缘，没有硬分界；回复、输入和按钮不被模糊。
3. 麦克风单击、长按、权限拒绝、聆听、查询、朗读和打断仍沿原链路；检查开启蓝色选中状态及无障碍描述。检查顺时针彩色边缘与真实音量声纹仍工作。
4. 检查视频壁纸、窄屏、大字体、横屏、键盘、关闭和重开。视频配色使用缓存预览与亮度参数，不逐视频帧做 GPU 读回，因此动态画面明暗变化不承诺逐帧改色。
5. API 31/32 核对遮罩不会擦除下层页面；API26 检查降级文字、按钮与安全区。用 Perfetto / 系统帧统计记录两层玻璃的实际 GPU 与帧耗时。

## 交付与兼容

保留 `com.scheduleplus.student`、1.0.1 / versionCode 36、数据库与原“时序清单”证书；不清除数据、不推送或发布。正式 APK 路径仍为 `app/build/outputs/delivery/时序清单-v1.0.1-正式版.apk`。

本轮 diff 基线为前一轮完整源码（SHA-256 `c6720bdacee75aab570ea3eb4801e4a6efa645789fa0931af64b654cf988bd4a`），单独保存为 `时序清单-v1.0.1-源码基线-壁纸玻璃修改前.zip`。本轮 patch 为 `时序清单-助手壁纸与玻璃.patch`，包含本轮追加的 [百炼协议与音色修复](2026-10-01-aliyun-realtime-compatibility.md)；不会把此前大量未提交修改归为本轮新增。完整源码候选 ZIP 与其校验文件同步重生成，排除凭据、私有后端、用户数据与构建缓存。
