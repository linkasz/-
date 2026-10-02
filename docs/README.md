# 开发文档入口

跨窗口接手应用开发先看 [时序清单开发交接（2026-10-02）](PROJECT_HANDOFF_2026-10-02.md)：包含最新需求、工作树基线、代码入口、验收边界与待办清单。宣传视频由原窗口继续负责。

语音与音色菜单、矩形人格卡片、保留课程下拉浮窗并新增独立 AI 页面，以及图标白边处理见 [本轮修复与实机记录](qa/2026-10-01-menus-assistant-page.md)。

最新人格业务层隔离、上下文工具、百炼专用朗读与待办日历修复见 [本轮交付记录](qa/2026-10-01-persona-tools-calendar.md)。记录区分源码/构建验证与尚未进行的真实服务、设备验收。

最新百炼 ASR / Omni 协议与音色兼容修复见 [本轮记录](qa/2026-10-01-aliyun-realtime-compatibility.md)。账号真实调用与实机语音须单独验收。

最新 AI 浮窗配色与分层玻璃见 [本轮记录](qa/2026-10-01-assistant-wallpaper-glass.md)。实机外观与帧率须单独验收。

最新浅色弹层、语音、天气与双包并存修复见 [本轮记录](qa/2026-10-01-weather-voice-light-coexist.md)。此前目标核对与剩余验收见 [2026-10-01-goal-completion-audit.md](qa/2026-10-01-goal-completion-audit.md)。当前自有仓库为 [linkasz/-](https://github.com/linkasz/-)，具体实现与验证以源码及对应轮次报告为准。

先阅读根目录 [AGENTS.md](../AGENTS.md)，再按问题查阅下列文档。无需在每轮工作前遍历所有文档。

| 任务 | 参考文档 |
| --- | --- |
| 当前版本、Beta / 正式版与历史实验线 | [RELEASE_MODEL.md](RELEASE_MODEL.md)、[EXP_BRANCH_AND_RELEASES.md](EXP_BRANCH_AND_RELEASES.md) |
| 最初十个 Beta 的功能与时间证据 | [EARLY_BETA_HISTORY.md](EARLY_BETA_HISTORY.md) |
| 每轮开发、PR、收尾与工作树整理 | [DEVELOPMENT_WORKFLOW.md](DEVELOPMENT_WORKFLOW.md) |
| 按用户任务查功能与实现入口 | [FEATURE_MAP.md](FEATURE_MAP.md) |
| 目录、依赖与结构调整 | [PROJECT_STRUCTURE.md](architecture/PROJECT_STRUCTURE.md) |
| 页面、弹窗、选择器与公共控件 | [SLEEPDOWN_DESIGN_SYSTEM.md](architecture/SLEEPDOWN_DESIGN_SYSTEM.md) |
| 玻璃采样、渲染和生命周期 | [LIQUID_GLASS_FRAMEWORK.md](performance/LIQUID_GLASS_FRAMEWORK.md) |
| 跨 Activity 转场 | [TRANSITION_FRAMEWORK.md](TRANSITION_FRAMEWORK.md) |
| AI 文件导入 | [AI_FILE_IMPORT.md](ai/AI_FILE_IMPORT.md) |
| AI助理 | [DAY_AGENT_RUNTIME.md](architecture/DAY_AGENT_RUNTIME.md) |
| 备份与升级 | [BACKUP_FORMAT_V1.md](migration/BACKUP_FORMAT_V1.md)、[1_2_0_PACKAGE_MIGRATION.md](migration/1_2_0_PACKAGE_MIGRATION.md) |
| 节次设置 | [PERIOD_SCHEMES_GUIDE.md](PERIOD_SCHEMES_GUIDE.md) |
| 厂商实验功能的历史与隔离边界 | [EXP_BRANCH_AND_RELEASES.md](EXP_BRANCH_AND_RELEASES.md) |

`release-notes/` 保存每个版本的面向用户说明；Beta 是逐次记录，正式版是该轮有效内容的归并。带日期的 `docs/` 报告保存调查与验收经过，`docs/archive/` 保存暂停实验。它们均不自动改变当前功能基线。当前工作空间整理记录见 [2026-09-25-workspace-audit.md](2026-09-25-workspace-audit.md)。旧实验参数、测试数量、临时禁令和“本轮不做”的范围只适用于当时任务。引用时核对日期与代码；新的明确需求可以更新相关基线。

专项规范中的“不改动成熟链路”用于控制任务外回归和纯结构迁移，不阻止针对该链路的功能修复、设计调整或有证据的优化。发现过时事实时就地修正文档，避免继续追加相互冲突的补丁式约束。
