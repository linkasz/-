# Dock 拖动卡顿与项目验收审计

日期：2026-09-28  
分支：`codex/calendar-sync-orphan-recovery`  
远端：仅 `origin -> https://github.com/linkasz/schedule-plus-android.git`

## Dock 调查与修复

| 项目 | 证据与处理 | 状态 |
| --- | --- | --- |
| 拖动样本触发弹簧任务扇出 | `DampedDragAnimation.updateValue()` 原先每次输入都启动外层和内层协程；动画帧的 `updateVelocity()` 又持续启动新的 velocity 动画。现在拖动期间由单个 `snapshotFlow` 收集器配合 `collectLatest` 消费最新目标，直接读取主弹簧速度，不再逐帧另起 velocity 协程。 | 代码路径已收敛；帧时间收益待设备采样 |
| Dock 横向偏移逐样本启动协程 | 原先每个触摸样本都启动协程执行 `Animatable.snapTo()`。现在拖动时直接更新偏移状态，仅在松手/取消后运行一次回弹弹簧。 | 已修复，未实机验证 |
| 高亮与指示器重复接收拖动 | 同一个指示器同时安装高亮和弹簧拖动识别器。Dock 改为由弹簧拖动入口驱动高亮按下/释放；高亮精确跟随位置直接更新状态，不再逐样本启动动画协程。相同精确跟随优化也适用于周视图的外部拖动高亮。 | 已修复，未实机验证 |
| 松手重复结算 | 原实现会在拖动回调与选中索引观察器中重复启动选择弹簧；点击切换外部选中项时，索引观察器还会将变化回调给导航一次。现在拖动结束负责一次结算和导航，外部标签变化只更新指示器。 | 已修复，未实机验证 |
| 窗口尺寸/方向变化 | 偏移 `derivedStateOf` 原来只按 density 记忆；拖动映射和高亮闭包也捕获首次布局的标签宽度与方向。现在宽度参与记忆键，手势和高亮读取最新宽度/方向，并安全处理零宽度。 | 已修复，未实机验证 |
| 取消/生命周期中断 | 拖动跟踪器在正常结束、手势取消和指针输入协程取消时都会停止；高亮只在尚未完成结算的中断路径做恢复。 | 代码路径已补齐，设备行为待验收 |
| 玻璃/Backdrop 绘制成本 | 现有 Dock 有独立的玻璃容器、移动强调内容和指示器材质层。本轮未删减这些效果；新增的 `SleepDown.LiquidTabs.Drag` Perfetto 区段可与现有 Compose tracing 和 Glass counters 对照。 | 需要设备 Perfetto 证据后再决定是否调整 |

`DockDragMathTest` 覆盖慢速/快速样本累计、RTL 映射、边界夹取、零宽度和落点取整。新增 `DockDragBenchmark` 提供带玻璃壁纸的慢拖和快拖 `FrameTimingMetric` 场景。

## 其他模块巡查

- 设置闪退、Popup 锚点和统一导航已有修复记录，当前源代码保留这些改动；设置/待办视觉截图对应的真机回归、统一导航旋转与状态恢复仍未验收，沿用 [设置与待办审计记录](2026-09-28-settings-todo-bug-audit.md) 的状态。
- 本轮静态查看系统日历同步、日历事件删除重试和待办自动同步入口，没有确认新的数据丢失或重复事件缺陷。权限撤回、自动/手动交错同步和事件清理重试仍须按 [发布验收清单](RELEASE_ACCEPTANCE.md) 实测。
- AI 分享与 Shizuku、待办完整工作流、提醒跳转、备份迁移、桌面组件、实况/视频壁纸以及更新器目前仍属未端到端验收项；没有将这些未验收项登记为已确认 Bug。

## 下一步顺序

1. 在可用真机完成 Dock 点击、慢拖、快拖、边界回拖、取消和五个页面切换；以同设备同刷新率比较基准，检查主线程、RenderThread、Compose tracing 与 Glass counters。
2. 在手机、平板、横竖屏、RTL、字体放大和无 Backdrop 降级下验收统一导航、待办/日历/洞察及手势取消。
3. 按数据风险依次验收待办重复/子任务、日历幂等和权限撤回、通知去重/定位、旧备份迁移与恢复。
4. 验收 AI 分享与 Shizuku、Widget、壁纸生命周期和自有 Release 更新器；签名材料仍放在仓库外，再准备正式候选包。

## 本轮验证限制

- `adb` 位于工作区 SDK；`adb devices -l` 当前没有连接设备，因此没有采集帧时间或实机 Perfetto trace。此前性能交接记录也没有有效的 PLJ110 物理性能基线，本轮未沿用失败数据或启动该暂停路线。
- 已尝试 `:app:compileGithubReleaseKotlin` 和 `DockDragMathTest`。使用 JDK 17、JDK 21 与 JBR 25 均在 Gradle 构建配置前因 Windows Java 无法建立 loopback connection 失败；`--no-daemon --offline` 和沙箱外重试仍得到相同的 `UnixDomainSockets.connect: Invalid argument`。所以本轮源码编译、单测和 Macrobenchmark 编译均未能完成，不能据此宣称通过。
- `git diff --check` 无空白错误；Git 只提示现有工作树文件的 LF/CRLF 转换提醒。
- 本轮未推送、发布、清理工作树或改动 Room/备份协议。

## 2026-09-29 Dock 与图标跟进

- Dock 不再让每个拖动目标更新通过 `collectLatest` 取消并重启 `Animatable`。Dock 使用独立的单帧弹簧驱动，每帧采样最新目标并推进一次临界阻尼状态；松手后继续收敛到最近标签。共享的 `DampedDragAnimation` 未改，`LiquidToggle` 行为不受此路径影响。
- 新增弹簧状态步进的定向测试；`DockDragMathTest` 7/7 通过，`compileGithubReleaseKotlin` 通过。Macrobenchmark 已改为先按住 500ms 再注入慢拖/快拖触摸事件，`:benchmark:assemble` 编译通过；尚未在设备上运行，本机 `adb devices -l` 没有已连接设备，因此该修复还没有实机帧时收益结论。
- 图标已统一为白色底板和蓝紫线条课程网格/勾选标记；自适应前景使用矢量资源，旧系统五档密度位图及六个启动器别名同步更新，1024×1024 PNG 源图保留在 `app/src/main/icon-artwork/`。
- `assembleGithubRelease` 已完成 Kotlin、R8、资源优化与 lint，封包阶段因缺少仓库外 Release 签名配置失败；按用户选择没有生成 Debug 替代包。未推送或发布，版本号保持 1.2.8 / 35。

## 后续开发顺序

1. 有设备后先完成 Dock 慢拖/快拖帧时对比，并回归五项统一导航、设置页与待办布局；之后覆盖 API 26 和平板布局。
2. 按发布验收清单收口待办重复/父子任务、日历同步幂等与权限撤回、通知定位、旧数据迁移和 BackupFormatV1 往返。
3. 再验收文本/图片分享与 Shizuku、桌面组件、静态/实况/视频壁纸生命周期及自有 Release 更新器；逐项记录复现条件和实际结果，只修确认的问题。
