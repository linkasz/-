# 壁纸同步与日历当天详情修复记录

日期：2026-09-29

## 修复内容

- 根主界面的待办、日历、洞察次级页面之前复用了设置页的中性背景层，覆盖了课程页壁纸。本次只让待办目的地绘制与课程页相同的壁纸、亮度预览、裁切/取样层和前景色调层；设置页仍使用原中性背景。离开待办返回课程页时保留背景直到转场结束。
- 通用详情脚手架默认显示已保存的全局壁纸，静态图与视频均复用课程壁纸渲染器；设置详情显式选择原设置背景。无壁纸时保持白色。
- 巡查发现独立的多课表管理页此前固定使用黑色背景；现已接入相同壁纸渲染与玻璃采样层。设置内嵌的多课表管理仍留在设置背景。
- 日历当天课程从最新课表状态按日期计算；详情卡片显示课程时间、地点、教师和备注，并与当天待办合并。再次点击已打开日期、点击关闭按钮或点击遮罩都可关闭；无事件日期显示空状态。
- 日历日期、课程和待办详情沿用课程玻璃卡片及当前课程配色。

## 验证

- `:app:testGithubDebugUnitTest --tests com.xiaomanjun.sleepdownschedule.feature.todo.TodoCalendarUnifiedAgendaTest`：通过。
- `compileGithubReleaseKotlin`：通过。
- `assembleGithubDebug`：通过。
- APK：`app/build/outputs/apk/github/debug/app-github-debug.apk`，applicationId 为 `com.scheduleplus.student.debug`，versionName `1.0.1`，versionCode `36`，minSdk `26`，targetSdk `36`。
- SHA-256：`E994CC240144959E33DD453E4954A940B7A53A80DB71C969B1B9798AA71B5D0D`。

## 设备验收状态

ADB 当前检测到 `emulator-5554`（Android API 26 模拟器）。本轮未安装 APK、未设置测试壁纸，也未采集界面截图，因此静态图片/视频壁纸同步、预览实时变化、弹层边缘布局和实体设备视觉效果仍待设备验收。构建通过不代表这些界面项目已验收。
