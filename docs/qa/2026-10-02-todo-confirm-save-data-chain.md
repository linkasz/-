# 待办确认保存与 AI 创建回执

## 范围与基线

用户在应用交接后选择优先检查“待办与 AI 确认保存的数据链路”。本轮先修复手动待办保存、AI 提取子任务的保存和 AI 待办创建后的失败边界；课程、课表创建及回收站完整实机验收继续保留在交接待验清单。

已阅读根 AGENTS、2026-10-02 交接文档、最新待办按钮 QA、菜单/浮窗 QA 和助理创建确认 QA，并核对相应源码。个人 Wiki 搜索“时序清单 SleepDown SchedulePlus”没有结果。

开始时分支为 `codex/calendar-sync-orphan-recovery`，HEAD 为 `07ee11f`，工作树 349 条记录（203 条已跟踪改动、146 条未跟踪路径）。保留该工作树和分支，没有切换到 main 或从远端重建。六个本轮修改的源码文件均在修改前复制到 `tmp/todo-confirm-save-20261002/baseline/`；增量 patch 对比这些副本，不将已有改动冒充本轮成果。

## 已定位的问题与修复

- 手动编辑页确认按钮和 ViewModel 没有保存中的执行保护，连续确认可多次提交 ID 为 0 的草稿。现在 ViewModel 在启动保存协程前同步领取保存状态，UI 同时禁用确认；已关闭编辑页的迟到确认不再提交。
- 父任务保存后的回调逐个启动子任务保存，各自独立提交，子任务失败时父任务已存在。现在 Repository 校验父子草稿，DAO 在同一 Room 事务内保存父任务及所有提取子任务。任何子任务写入失败都会回滚这一批；事务中再次核对已有父任务是否存在、是否归档，旧草稿不能借确认恢复归档数据。
- 手动保存回调原本在通知、组件等后续步骤之后，后续异常可能阻止界面收到保存成功。现在本地事务提交后先返回成功，提醒/组件/日历独立处理失败；已确认的保存使用应用生命周期，页面退出不取消它。外部 Provider 延迟不占用下一份草稿的保存状态。
- 将后续日历清理改为在既有同步锁内读取当前任务；若较晚执行清理时任务已重新获得日期，不按旧编辑快照删除事件。真实并发 Provider 行为仍需实机验证。
- AI 待办的日历对象初始化、自动同步偏好读取等原本位于整段创建的错误边界内，即使事务已提交，也可能返回可重试的创建失败。现在事务失败与提交后的失败分开；提醒、组件、日历各自尝试，普通后续异常仍返回已验证的创建成功及后续提示，从而保持成功回执的防重复执行保护。协程取消继续传播。

主要源码为 `feature/todo/TodoActivity.kt`、`TodoViewModel.kt`、`TodoRepository.kt`、`TodoDao.kt`、`TodoCalendarSync.kt` 和 `feature/agent/AgentTodoAction.kt`。

本轮没有改包名、版本、Room 实体/版本/迁移、备份格式、人格、语音入口、玻璃材质或视频工程。

## 验证

本轮实际执行 `:app:compileGithubReleaseKotlin`、带下列四个类过滤器的 `:app:testGithubDebugUnitTest` 和 `:app:compileGithubDebugAndroidTestKotlin`，最终 `BUILD SUCCESSFUL in 8m 3s`。单 worker、4GB Gradle 堆。测试 XML 核对结果为 **4 套、32 项、0 失败、0 错误、0 跳过**，没有将历史测试数量计入本轮。

| 测试类 | 本轮通过项数 |
| --- | ---: |
| AgentCreationReceiptLedgerTest | 4 |
| AgentDateTodoTest | 12 |
| AgentReplyBoundaryTest | 12 |
| AgentTodoCreationFollowUpTest | 4 |

新增 JVM 测试 `AgentTodoCreationFollowUpTest` 共 4 项：日历失败后成功回执仍阻止再次创建、提醒失败不跳过组件/日历、日历需确认与保存成功同时报告、取消语义保留。定向命令同时运行现有 `AgentCreationReceiptLedgerTest`、`AgentReplyBoundaryTest`、`AgentDateTodoTest`。

新增 Android 内存数据库测试 `TodoConfirmedSaveTest` 共 5 项：父子完整保存、子任务外键失败回滚整批、已有父任务编辑回滚、无效子任务不产生部分草稿、归档后的旧快照不能恢复任务。测试源码编译通过，**未在 Android 上执行**。另外检查 Release 的 Room 生成代码，`upsertWithSubtasks` 确实由 `performInTransactionSuspending(__db)` 包裹。此检查不替代 Android 运行时的回滚测试。

增量 diff 空白检查通过。本轮没有正式打包或安装，未操作 USB 手机。

环境恢复只设置命令环境变量，没有重写项目配置：使用工作区已有 JDK 21/Gradle 缓存与 SDK；Windows Java Selector 的 Unix socket 连接先报 `Invalid argument: connect`，指定工作区临时 socket 目录并让 Java 子进程继承后恢复。Miuix 子构建还需设置 `ANDROID_HOME`。构建日志在 `tmp/todo-confirm-save-20261002/validation.log`。

## APK 与待验项

现有正式 APK `app/build/outputs/delivery/时序清单-v1.0.1-正式版.apk` 重新核对 SHA-256 为 `0038fd5a546ef3914c70be701b9d7e9698bdf3fe88e1943178b039f27971a4e2`，与交接记录一致。该 APK 没有包含本轮源码修复；没有生成新 APK、重新验签或安装。

仍待执行：五项 Android 内存库测试，真实界面的快速确认/失败重试/页面退出、AI 草稿编辑取消与确认、通知及日历权限异常、实际 Provider 并发时序、进程终止与持久回执交接、课程/课表创建、真实归档/恢复/30 天边界。没有使用真实用户任务进行删除或恢复，也未使用云端密钥。

未提交、推送、发布、清库、卸载或改写 Git 历史；私有后端、核心提示词及原始用户资料未导出。
