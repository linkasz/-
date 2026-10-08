# 删除操作按钮统一背景自适应配色

## 原因与改动

前版 `TodoDeleteActions` 使用 AI 助理的 `rememberAssistantGlassPalette`，采样壁纸顶部区域，并采用与待办页不同的亮暗判断，导致页面其他按钮为浅色时这三个按钮仍出现深色。

本次仅修改 `feature/todo/TodoActivity.kt` 的这个组件：

- 使用与待办页其他按钮一致的 `glassUsesLightStyle(config)` 和 `sleepDownGlassForegroundColor(config)`。
- 不覆盖 GlassSurface 默认底色，由公共组件根据页面配置提供白色或深色玻璃底。文字与边框共同使用相同前景色。
- 删除可用时文字使用页面删除图标相同的 `0xFFE54D4D`，取消专属红色底，三个按钮共用底色。
- 保留模糊、折射、高光、阴影、按压、50dp 同行布局和窄屏 Tooltip。禁用文字仍为 0.80 透明度，选择与删除业务逻辑不变。

## 构建与交付

正式 `compileGithubReleaseKotlin` 与 `assembleGithubRelease` 通过，耗时 3m 45s。R8、资源压缩和 lintVital 保留，原签名验证通过。包名 `com.scheduleplus.student`、版本 1.0.1 / 36 不变。

APK SHA-256：`0038fd5a546ef3914c70be701b9d7e9698bdf3fe88e1943178b039f27971a4e2`。

设备 V2352A / Android 16 完成系统安装确认后覆盖安装成功。手机 `base.apk` SHA-256 与交付包一致。

实机 4 个场景通过：冷启动待办、展开删除操作、取消、再次展开。当前明亮壁纸下三枚按钮与页面其他按钮一致采用浅色玻璃底、深色文字，截图可见玻璃背景与清晰边界。取消后重新展开的位置一致，三个范围仍为 `[105,861,437,1036]`、`[465,861,796,1036]`、`[824,861,1155,1036]`，同行且不重叠。当前进程没有 FATAL EXCEPTION。

当前列表为空，全选和删除所选正确禁用，没有执行用户任务删除。结果文件为 `tmp/todo-adaptive-actions/device-results.json`，日志为 `tmp/todo-adaptive-actions/device-check.log`，手机停留在按钮展开界面供用户查看。

本轮没有重新运行单元测试；交付中历史 28 项定向测试来自前轮，不作为此次新增验收。不同壁纸、字体比例、低版本降级与真实删除未验收。Wiki 搜索未找到相关笔记，未清除数据、推送或发布。增量 diff 基于改动前工作树快照，不包含任务外已有改动。
