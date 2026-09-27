# 课表+ 自有 GitHub Release 发布

本文用于 `linkasz/schedule-plus-android`。Git 远端和应用内更新源都只使用该仓库；原 SleepDown 链接只出现在许可署名与来源说明中。

GitHub 仓库描述建议：`基于 SleepDown-Schedule 修改的课程与待办管理 Android 应用，支持 AI 任务提取、系统日历同步和动态壁纸；非官方修改版。`

## 稳定版

1. 更新 `app/build.gradle.kts` 的版本配置。当前自有应用基线为 `1.2.7 / versionCode 34`；后续版本号沿用 1.2.6 序列且 versionCode 单调增加。
2. 按仓库 `AGENTS.md` 配置仓库外的 Release 签名，执行：

   ```powershell
   .\gradlew.bat :app:generateSchedulePlusUpdateManifest --console=plain --no-parallel --max-workers=2
   ```

3. 将生成的 `app/build/outputs/apk/github/release/app-github-release.apk` 与 `app/build/outputs/release-manifest/scheduleplus-update.json` 上传到 GitHub Release，tag 使用 `v<versionName>`。
4. 从该 Release 下载两个文件，重新计算 APK SHA-256，并确认与 JSON 一致；检查 APK applicationId、versionCode、签名、公开源码 tag 和 Release 说明。
5. 使用 `.github/release-template.md`，保留非官方修改版署名、原项目许可来源、修改说明和完整源码链接。发布为正式版时不得将其标成 prerelease。

## Beta

使用相同流程，构建时传入例如 `-Psleepdown.versionName=1.2.7_beta1 -Psleepdown.versionCode=35`；清单任务默认会生成匹配的 `v1.2.7_beta1` tag。GitHub Release 标记为 prerelease。Beta 和正式版都附 APK 与更新 JSON；应用默认只查询稳定版，用户打开 Beta 开关后才查询预发布版本。

## 更新清单

`scheduleplus-update.json` 使用 UTF-8 JSON，字段如下：

```json
{
  "applicationId": "com.scheduleplus.student",
  "versionName": "1.2.7",
  "versionCode": 34,
  "apkUrl": "https://github.com/linkasz/schedule-plus-android/releases/download/v1.2.7/app-github-release.apk",
  "sha256": "由 APK 计算出的 64 位小写 SHA-256",
  "releaseUrl": "https://github.com/linkasz/schedule-plus-android/releases/tag/v1.2.7"
}
```

更新器只接受与当前 Release API 元数据匹配的自有仓库资产地址，并在安装前验证清单、包名、版本和 APK 摘要。Android 安装器负责最终签名兼容性校验并要求用户确认。

## 发布边界

- 不把 `SleepDown-Server/`、密钥、签名文件、用户数据库或缓存加入源码仓库或 Release。
- 保留 `LICENSE.md`、`THIRD_PARTY_NOTICES.md`、About 署名和 Release 声明。SleepDown 的自定义许可要求非商业使用与对应版本源码可见；不得把项目描述为 OSI 批准的开源项目。
- 不配置指向 SleepDown 原作者仓库的 Git 远端、更新 API 或 APK 下载地址。
