# 语音连续打断稳定性与录音降噪候选

## 场景与证据

用户确认：使用阿里云百炼 `qwen3-asr-flash-realtime`（识别后交给文字 AI），AI 正在朗读时开口打断，多次交互后应用闪退。本轮未取得该手机的崩溃堆栈，也未操作 USB 手机。因此以下是对当前源码中已确认的并发、取消和异常传播风险的修复，不能据此宣布手机闪退已闭环。

前轮玻璃按钮与工具栏统一、语音设置 Picker 闪退修复已分别提交；本轮只修改语音采集、播放、朗读请求取消及相关规则。包名、1.0.2 / 37、Room 45、原签名、确认后创建和自动朗读开关保持现有配置。

## 实际修改

- `VoicePlayback` 串行持有原生播放资源：旧写入和释放结束后才能创建新输出。打断只使旧代失效并取消队列与任务，原生创建、非阻塞写入、暂停、清空和释放都由工作协程处理。旧代不能播放新内容、更新当前播放字节或通知当前会话失败。队列故障使用取消释放已挂起的发送者；正常结束仍排空缓冲。PCM16 跨 HTTP 分块的半个采样保留到下块，播放排空超时作为失败处理。
- `VoiceHttp.useVoiceResponse` 将请求取消覆盖到收到响应头之后的阻塞读取。朗读任务取消时中止 Call，由读取者退出并关闭 Response，不在主线程与读取者并发关闭响应体。百炼朗读、硅基流动朗读和百炼音色试听接入该所有权路径。
- 实时录音通过有上限的帧通道交给 Main 处理会话状态和断句，避免录音工作线程直接执行打断、停止与原生播放清理。通道积压时明确停止并提示，不无限创建每帧任务。录音失败在切回 Main 后再次核对代次；音频焦点回调核对请求身份。
- 朗读完成、系统 TTS 恢复识别、数据库回合放弃等边界捕获普通异常；取消仍按取消传播。实时输入子任务通过 `coroutineScope` 归属会话，异常回到已有会话失败处理。
- 开口打断使用有界短预录和连续三帧阈值，打断后重放短预录，减少开始说话时丢字；只有确认平台回声消除启用后才允许这条自动路线。具体支持范围与待验见下文。

## 降噪范围

继续使用 `VOICE_COMMUNICATION` 采集，并尝试启用系统 `AcousticEchoCanceler` 与 `NoiseSuppressor`，检查实际启用状态；厂商效果不可用或抛异常不会终止录音，效果在所属录音任务退出后释放。

每次录音独立使用 PCM16 处理状态：80 Hz 一阶高通降低直流与低频轰鸣；平台降噪不可用时，再以连续 10 ms 能量窗估计安静底噪，使用 0.25 至 1 的柔和衰减、5 ms 开启和 120 ms 回落。无放大、无硬静音门，分块边界不重置滤波或包络。它只针对低频和轻微稳定底噪，不等同于语音分离、神经网络降噪或软件回声消除。

实现依据：[Android NoiseSuppressor](https://developer.android.com/reference/android/media/audiofx/NoiseSuppressor)、[AcousticEchoCanceler](https://developer.android.com/reference/android/media/audiofx/AcousticEchoCanceler)、[AudioTrack](https://developer.android.com/reference/android/media/AudioTrack)、[音频前处理约定](https://source.android.com/docs/core/audio/implement-pre-processing)。百炼现有协议保留，未照搬其他 SDK 的专用字段；参考[百炼 Android 多模态说明](https://help.aliyun.com/zh/model-studio/multimodal-sdk-android/)。

## 验证与待验

定向测试覆盖旧播放释放与新代隔离、连续 50 轮旧 token 拒绝、满队列故障解阻塞、取消不误报、PCM16 分块与反压、排空超时、响应头之后取消阻塞读取、短预录阈值与重置，以及合成信号下的低频衰减、语音频段保留、安静底噪处理、语音开启和分块一致性。合成信号结果不证明真实房间或厂商音频效果。

首轮 50 项中有两项失败：满队列发送者在 `close()` 后仍挂起，以及取消读取抛出的 IOException 没有恢复为协程取消。对应实现已修正，交付以最终统一构建结果为准。

补齐用户确认的百炼 ASR 开口打断路线后，重新执行 `:app:testGithubDebugUnitTest --tests 'com.xiaomanjun.sleepdownschedule.feature.agent.voice.*'`：11 个套件、50 项测试通过，0 失败、0 错误、0 跳过。统计来自本次 XML 结果；其中连续 50 轮是隔离播放资源的 JVM 测试，不是手机或真实百炼会话的连续对话验收。

未使用真实 API Key 调用百炼，未读取或导出原始语音、用户对话、人格核心或私有后端。未安装、启动或操作手机。本机 API 26 / 36 模拟器此前在 Android 启动前失败，详情见 [2026-10-18 报告](2026-10-18-ai-interruption-stability.md)；未将历史尝试或已编译的原生测试当作本轮设备测试。

自动开口打断覆盖百炼纯 ASR 与 Omni 路线，且要求 AEC 实际启用。纯 ASR 将短预录和后续录音继续交给服务端 VAD，不手动注入 SpeechStarted、不清空输入缓冲、不手动 commit 或创建回答；识别后仍交给已配置的文字 AI。Omni 保留原有手动断句。没有实际启用 AEC 的设备继续使用点击打断，其他平台没有扩展自动打断能力。

待验：百炼当前模型连续开口打断与点击打断、录音与扬声器回声在不同音量下的表现、无 AEC 设备的点击打断、静音/风扇/交通噪声/低声说话、蓝牙与有线耳机、音频焦点切换、断网和快速重开、系统语音及后台切换。原生 `VoiceAudioInterruptTest` 的 30 轮硬件测试未执行。

## 交付核对

最终统一构建于 2026-10-24 完成（本轮从 10 月 23 日开始）：语音定向测试与 `:app:assembleGithubRelease` 一起成功，耗时 5 分 5 秒。保留正式 R8、资源压缩、lintVital 与签名路径，未使用跳过压缩参数。构建前冻结的 12 个源码及测试文件在结束后全部匹配。`git diff --check` 通过。

本地交付包：`tmp/voice-stability-noise-20261024/时序清单-v1.0.2-37-语音打断与降噪候选-20261024.apk`，6,612,061 字节。APK 实际身份为 `com.scheduleplus.student`、`1.0.2 / 37`，非 debuggable；`apksigner verify` 通过，证书 SHA-256 为 `a6510d8496d8b3e4397bd7f30a03488db9408d38b5b4864c67375d4038924886`。仅比较摘要确认 APK 中的人格资源与本地资源一致，未输出其内容。

APK SHA-256：`e09c7dc0c47a5acfc13a3c7461e1fd4f70e416475582a27782b9b1d8f88228ac`。

本地验证日志及摘要：`tmp/20261023-voice-asr-final-build.log`、`tmp/20261023-voice-asr-test-results.json`、`tmp/20261023-voice-asr-source-freeze.json`、`tmp/20261024-voice-apk-summary.json`。这些临时材料与 APK 不提交到公开仓库。

GitHub 同步目标为既有分支 `codex/ai-model-discovery-20261018`，范围仅限本轮语音源码、测试和本报告；实际远端更新结果以推送回执及本轮交付回复为准。

本地候选保留本地真实人格资源，不作为已脱敏的公开 APK 上传。提交仅包含本轮语音源码、测试和本报告；工作树中原有私有资源、版本配置、声明文档及其他改动保持原处。无清库、卸载、reset、历史重写、标签、合并或 Release，也不修改宣传视频工程。
