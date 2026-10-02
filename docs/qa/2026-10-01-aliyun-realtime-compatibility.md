# 百炼实时转写、Omni 与音色兼容修复

## 证据与范围

用户确认三个 `qwen3-asr-flash-realtime` 稳定/快照模型均不能用，而 `qwen3.5-omni-flash-realtime` 能用；没有提供实际服务错误码和当前地域。本轮没有连接设备、没有读取或调用用户密钥，不将源码缺陷等同于已确认全部设备故障的唯一原因。个人 Wiki 本轮搜索未找到项目相关笔记。

源码中 `submitFinal`、`interrupt`、`stop` 原来都无条件发送 `response.cancel`。ASR 客户端协议没有响应生成/取消事件，且 Omni 在没有活动响应时取消也会返回错误；收到错误后客户端用统一的“语音协议或模型配置不被服务支持”停止会话。这是可确认的客户端协议缺陷，能解释转写与对话模型出现不同表现，但仍需实际服务返回验证。

## 修改

- `RealtimeVoiceProtocol.kt`：ASR 保持 16kHz PCM、文字输出及服务端断句，不允许生成或取消模型回复；所有取消入口统一经过能力与活动响应检查，打断 ASR 不再发送不属于其协议的缓冲清理事件。
- Omni 改为官方输入/输出格式结构（单声道 16 bit PCM，输入 16kHz、输出 24kHz）；输入转录模型从旧 `gummy-realtime-v1` 改为文档指定值。取消依赖未公开的 `create_response:false`；使用 Manual 模式和真实音量断句提交，提交不触发 Omni 回答。文字 AI 仍是唯一答复来源。
- `ManualVoiceTurn.kt`：按实际 PCM 时长判定，有声至少 200ms、停顿 600ms 后提交，静音每 2 秒清理，最长片段 20 秒。持续上传支持流式转录，不把固定帧数当作不变时长；没有空音频提交或无限静音累积。阈值沿用现有音量标尺，实机弱声及嘈杂环境需验收。
- Omni 朗读已校验的文字答复使用文档支持的 `session.update.instructions` 与裸 `response.create`，不发送其当前文档未列出的用户文字项及 OpenAI 专有参数。跟踪响应创建/结束，迟到的已打断响应取消后不播放。停止、离页、后台仍沿用现有释放链路。
- `AliyunVoiceCatalog.kt`：3.5 Flash/Plus 各提供 21 款官方音色，3.8 Flash 提供 27 款，展示中文名及真实 ID；没有将不同模型音色混为一组。ASR 只展示系统朗读方式，不让用户选择无法生成的云端音色。
- `VoiceSettings.kt`、`VoiceUi.kt`：切模型校正不兼容音色；加载旧偏好时保留模型、密钥、地域、业务空间与朗读选择，仅校正音色。旧 Chelsie/Cherry 配置在新 Omni 系列回归 Tina，ASR 回归系统默认。密钥继续本机加密、不写备份。
- `VoiceFailureMessage.kt`、`VoiceSession.kt`：连接检查和实际会话共用错误分类，展示模型、地域和受限长度错误码；区分鉴权、权限、额度、模型、音色及连接中断。服务原始描述仅用于分类，不展示或记录可能包含密钥和转录的原始消息。握手超时明确报错，避免保持“连接中”。

仍保留用户选择的北京/新加坡配置；没有推断用户所在地域、冒充账号授权或自动替换成另一个模型。

## 官方核对依据

- [实时转写客户端事件](https://help.aliyun.com/zh/model-studio/qwen-asr-realtime-client-events)：ASR 支持的输入、提交和结束事件。
- [实时转写指南](https://help.aliyun.com/zh/model-studio/real-time-speech-recognition-user-guide)：用户所列三款 ASR 的地域与版本支持范围。
- [Omni 客户端事件](https://help.aliyun.com/zh/model-studio/client-events)：格式、转录配置、Manual 提交、响应生成/取消。
- [Omni 音色列表](https://help.aliyun.com/zh/model-studio/omni-voice-list)：按模型筛选音色 ID。目录只提取功能参数与短名称，未复制长描述。

## 验证

语音定向测试 **19/19 通过**，0 失败、0 错误、0 跳过：`RealtimeVoiceProtocolTest` 12 项、`ManualVoiceTurnTest` 4 项、`VoiceWaveformTest` 3 项。覆盖三个 ASR 在北京/新加坡的帧结构、禁止回复与取消、Omni 格式/Manual/朗读帧、模型音色迁移、错误脱敏、实际 PCM 时长断句/静音/噪音/连续轮次/上限，以及现有真实振幅波形。报告 `app/build/test-results/testGithubDebugUnitTest/`。

`compileGithubReleaseKotlin`、上述定向测试、完整 `assembleGithubRelease` 及交付任务 **7 分 12 秒成功**，保留 R8、资源压缩、lintVital 与原签名；实际 APK、身份、签名及校验值见 [助手玻璃交付记录](2026-10-01-assistant-wallpaper-glass.md)。日志 `tmp/aliyun-voice-glass-release.log`。未进行账号真实调用及手机语音/外观验收，不宣称所有账号模型均可用。

### 手机复验

1. 在语音设置保留现有 Key，核对地域与业务空间。依次选择三个 ASR，保存并连接检查；进入聆听后说“今天有什么安排”，确认实时文字出现、文字 AI 回复一次，开启朗读时使用手机中文 TTS；关闭朗读时只有文字。
2. ASR 检查首次转写、第二轮、点击打断、关闭、重开；不能因为回复取消或缓冲清理事件退出。系统未安装中文 TTS 时明确提示，不把它误报为百炼模型无权限。
3. Omni 3.5 Flash/Plus 和 3.8 分别检查连接及连续提问。模型只朗读文字 AI 的已校验答复，不自行代答。核对换音色及打断后不播放上一轮，静音/短噪音不会生成空请求。
4. 3.5 选择厚，再切 3.8 应回归甜甜；3.8 选择泽恩再切 3.5 同样校正。切回转写显示系统默认。重开设置与应用验证偏好保存。
5. 使用无效 Key、无权限模型、超额和断网核对错误类型及模型/地域/错误码；输出不得包含密钥或用户转录。关闭窗口、后台及音频焦点转移后确认录音与连接停止。

## 交付

保留应用身份、版本、数据库与原签名。与本轮助手壁纸及分层玻璃一并交付，差异基线、APK 和源码见 [助手玻璃记录](2026-10-01-assistant-wallpaper-glass.md)。不自动安装、不推送、不发布。
