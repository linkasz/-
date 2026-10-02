# 时序清单自有 GitHub Release 发布

本文用于 `linkasz/-`（远端 `https://github.com/linkasz/-.git`）。Git 远端和应用内更新源都使用该仓库；上游链接用于准确的许可署名与组件来源说明。

GitHub 仓库描述建议：`基于 SleepDown-Schedule 修改的课程与待办管理 Android 应用，支持 AI 任务提取、系统日历同步和动态壁纸；非官方修改版。`

## 稳定版

1. 更新 `app/build.gradle.kts` 的版本配置。当前候选为 `1.0.2 / versionCode 37`，应用身份为 `com.scheduleplus.student`；后续 versionCode 必须单调增加。
2. 按仓库 `AGENTS.md` 配置仓库外的 Release 签名，执行：

   ```powershell
   .\gradlew.bat :app:generateSchedulePlusUpdateManifest --console=plain --no-parallel --max-workers=2
   ```

3. 将生成的 `app/build/outputs/apk/github/release/app-github-release.apk` 与 `app/build/outputs/release-manifest/scheduleplus-update.json` 上传到 GitHub Release，tag 使用 `v<versionName>`。
4. 从该 Release 下载两个文件，重新计算 APK SHA-256，并确认与 JSON 一致；检查 APK applicationId、versionCode、签名、公开源码 tag 和 Release 说明。
5. 使用 `.github/release-template.md`，保留非官方修改版署名、原项目许可来源、修改说明和完整源码链接。发布为正式版时不得将其标成 prerelease。

## Beta

使用相同流程，构建时传入例如 `-Psleepdown.versionName=1.0.3_beta1 -Psleepdown.versionCode=38`；清单任务默认会生成匹配的 `v1.0.3_beta1` tag。GitHub Release 标记为 prerelease。Beta 和正式版都附 APK 与更新 JSON；应用默认只查询稳定版，用户打开 Beta 开关后才查询预发布版本。

## 更新清单

`scheduleplus-update.json` 使用 UTF-8 JSON，字段如下：

```json
{
  "applicationId": "com.scheduleplus.student",
  "versionName": "1.0.2",
  "versionCode": 37,
  "apkUrl": "https://github.com/linkasz/-/releases/download/v1.0.2/app-github-release.apk",
  "sha256": "由 APK 计算出的 64 位小写 SHA-256",
  "releaseUrl": "https://github.com/linkasz/-/releases/tag/v1.0.2"
}
```

更新器只接受与当前 Release API 元数据匹配的自有仓库资产地址，并在安装前验证清单、包名、版本和 APK 摘要。Android 安装器负责最终签名兼容性校验并要求用户确认。

本轮分别交付 `时序清单-v1.0.2-本地完整版.apk` 与 `时序清单-v1.0.2-公开版.apk`。公开源码和公开 APK 的来源人格 `corePrompt` 及内置核心常量为空；本地真实核心保留原位，不进入公开快照。两包的人格行为可能不同。当前更新协议仍固定读取 Release 中的 `app-github-release.apk`；只上传已核验的公开版，配套清单必须由公开版 APK 生成。

生成本地 APK/清单不等于已发布。当前 GitHub 插件可操作源码、分支和 PR，未提供创建 Release 或上传附件工具；正式发布还需可用的附件上传能力。公开版 About 与发布说明的署名处理应按实际书面豁免范围确定，不把本地豁免自动扩大为公开分发豁免。

## 发布边界

- 不把 `SleepDown-Server/`、密钥、签名文件、用户数据库或缓存加入源码仓库或 Release。
- 保留 `LICENSE.md`、`THIRD_PARTY_NOTICES.md`、About 署名和 Release 声明。SleepDown 的自定义许可要求非商业使用与对应版本源码可见；不得把项目描述为 OSI 批准的开源项目。
- 不配置指向 SleepDown 原作者仓库的 Git 远端、更新 API 或 APK 下载地址。
