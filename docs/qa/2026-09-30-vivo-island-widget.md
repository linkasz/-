# vivo 原子岛与桌面组件添加验收记录

## 实现

- 课程实时提醒通过独立适配层写入 vivo 本地通知协议，使用同一通知 ID 执行创建、更新、结束；重复更新至少间隔 10 秒。
- 仅 GitHub 渠道、正式包名、vivo/iQOO 设备、获批课程 scene 与对应签名 SHA-256 同时匹配时启用。默认审批配置为空，因此现阶段仍走普通课程通知。待办提醒保持原路径。
- 小组件添加请求改用 provider 元数据中的预览。请求被桌面接收后显示“等待桌面确认”；只有成功回调或新增组件 ID 才显示“已添加”。无系统确认时可直接前往桌面手动添加。
- 原子岛和组件请求的临时本地状态已排除在 Android 系统备份之外。

## 本地结果

- `:app:compileGithubReleaseKotlin`：通过。
- `:app:assembleGithubDebug`：通过；资源排除修改后的增量打包也通过。APK 为 `com.scheduleplus.student.debug`、`1.0.1 (36)`。
- 用户确认更换签名证书后，`:app:exportBrandedGithubReleaseApk` 通过。正式 APK 为 `com.scheduleplus.student`、`1.0.1 (36)`，证书主题为 `CN=时序清单, OU=Android, O=时序清单, C=CN`，证书 SHA-256 为 `a6510d8496d8b3e4397bd7f30a03488db9408d38b5b4864c67375d4038924886`。交付文件名为 `时序清单-v1.0.1-正式版.apk`。
- ADB：未检测到设备，因此 vivo S50、iQOO Z9 上的系统确认框、组件实例与原子岛实际显示均未验收。
- 原子岛实际显示还需要本应用的课程 scene 获批，并使用匹配获批包名及签名的安装包。

## 设备验收步骤

1. 在 vivo S50、iQOO Z9 分别记录启动器版本、Android 版本和请求返回值。点击“添加到桌面”，核对系统确认界面、成功回调及组件 ID；若没有确认界面，用“前往桌面手动添加”并返回应用确认状态。
2. 获批课程 scene 后，在匹配签名的正式包检查上课提醒创建、更新、下课结束、取消提醒、通知权限撤回和重启恢复；同一课程不得出现两条普通通知。

参考：[Android 小组件固定接口](https://developer.android.com/reference/android/appwidget/AppWidgetManager#requestPinAppWidget(android.content.ComponentName,android.os.Bundle,android.app.PendingIntent))、[vivo 原子岛推送指南](https://help.aliyun.com/zh/document_detail/3030718.html)。
