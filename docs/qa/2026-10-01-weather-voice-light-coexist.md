# 时序清单：浅色弹层、语音、天气与双包并存

## 范围与证据

- 按用户确认的范围：设置弹层使用可读的白色玻璃底；首次安装默认浅色；保留主动切换深色、已有主题偏好及课程壁纸。
- 天气未指定城市时使用获准的大致定位，进入应用先解释用途，再请求权限。拒绝后仍支持指定城市。
- 工作树原有修改保留；没有清除数据、改签名、自动安装、提交、推送或发布。
- 本轮个人 Wiki 搜索“时序清单 天气 语音”没有结果。
- 图 5 是聊天记录截图，能证明回复重复出现查询前言，不能证明某次 HTTP 查询成功。没有收到原始 AndroidRuntime/网络日志，因此没有将猜测写成设备根因。

## 已定位的问题与修改

| 问题 | 代码证据 | 本轮修复 |
| --- | --- | --- |
| 设置弹层灰暗、与下层文字重叠 | LiquidDialogSurface 浅色表面只有 0.18 透明度，叠加遮罩后下层文字仍透出 | 主题设置弹层改为白色 0.96，深色 0.94；主动采用壁纸对比的弹层保留玻璃透明度 |
| 系统深色下首次启动发暗 | ScheduleConfig 默认 followSystemDarkMode=true，night 初始 Window 背景为黑色 | 新配置默认 false；night 初始窗口使用中性浅色；API29+ forceDarkAllowed=false；已有偏好保留 |
| 天气前言重复、需要再次提问 | 旧天气快照不能按城市/日期查询；工具重复命中后返回指示文本而不是实际结果；没有独立天气工具 | 新增天气直达链路和 WEATHER_QUICK 工具，天气问题直接发送实际结果；重复工具仍返回原事实；异步工具结果进入同一轮上下文 |
| ASR final 重复触发、查询状态混乱 | 无稳定 item ID 的转录可能多次 final；SpeechStarted 会影响上一轮处理 | final 等待 800ms，标准化文本 5 秒去重；新 SpeechStarted 不取消查询，仅在说话中执行打断；QUERYING 状态可打断 |
| 两应用安装冲突 | GitHub Manifest 中 ColorOS Provider authority 和 signature permission 是原包硬编码名称 | 改为 ${applicationId} 派生；运行时 ColorOSContract 同样读取 BuildConfig.APPLICATION_ID；FileProvider/Shizuku 原本已经按包名分离 |

## 声纹方案对比与选型

| 方案 | 适用用途 | 本轮决定 |
| --- | --- | --- |
| [compose-audiowaveform](https://github.com/lincollincol/compose-audiowaveform) | Compose 音频波形、播放进度；音频文件振幅可配合 Amplituda | 未引入文件解码流程 |
| [WaveformSeekBar](https://github.com/massoudss/waveformSeekBar) | Android View 波形与音频拖动进度 | 不给 Compose 助手增加文件 SeekBar |
| [SiriWave](https://github.com/kopiro/siriwave) | 可用外部 amplitude 控制的彩色镜像波形 | 移植 MIT iOS9 衰减曲线到原生 Compose Canvas，固定三层，复用 Path |

- 实际录音 PCM RMS / 播放 PCM / 系统识别 RMS 驱动振幅；无声音为平线。等待查询使用独立圆点，避免用循环波形假装声音。
- 使用 withFrameNanos 跟随显示刷新，振幅分别做快速上升与缓慢释放；麦克风每帧约 40ms。采样间隔与绘制结构是源码事实，60fps 和设备端到端延迟尚未测量。
- OFF 表示空闲；LISTENING、THINKING、QUERYING、SPEAKING 分开处理，另有 CONNECTING/ERROR。停止、离页、后台、焦点丢失沿用会话释放路径。
- MIT 全文保留于 licenses/SiriWave-MIT.txt 和 APK 的 assets/licenses/SiriWave-MIT.txt；THIRD_PARTY_NOTICES.md 记录来源和移植范围。

## 液态玻璃

沿用项目已集成的 [AndroidLiquidGlass / Backdrop](https://github.com/Kyant0/AndroidLiquidGlass)，避免再引入第二套采样系统。业务层调用 GlassSurface，页面 underlay 仍为采样源。

- TopAssistantSurface 保留背景 blur 与 lens 折射，加入清晰边缘高光、外阴影、内阴影。
- 增加按几何缓存的细颗粒与随真实音量变化的径向高光；InteractiveHighlight 保留触摸反馈。
- 首页助手读取明暗主题前景与表面色；展开后的顺时针彩色边缘继续保留。
- 复用原有 API26 / 无 Backdrop 降级，没有为低端机增加 WebView 或第二层全屏模糊。设备 GPU 帧时间、文字对比与降级外观待验。

## weather.quick 契约

对助手的逻辑接口为 `weather.quick({city?, date?})`。兼容模型的函数 wire name 为 `WEATHER_QUICK`，因为部分平台不允许点号；同时兼容读取 `weather.quick` 返回名称。

- city 明确时用 [Open-Meteo Geocoding](https://open-meteo.com/en/docs/geocoding-api)；缺省时用已获准的 Android 大致定位，不猜城市。
- date 使用设备可信日期或明确 ISO 日期；查询 [Forecast](https://open-meteo.com/en/docs) 的对应日期，返回 ok/city/date/text/temperature、降水概率、时区及来源。
- 定位结果标为“当前位置”，没有冒充精确市名。查询时坐标发送给 Open-Meteo，不写入日志或备份；只在用户查询天气时取位置，不后台追踪。
- 未来日期不使用今天的 current 数据；缺失日期、无定位、未知城市、网络/服务错误均明确失败，不填虚构天气。
- 进程内 Mutex 串行化查询，同城市/日期结果 5 秒内合并；HTTP 临时网络错误或 429/5xx 重试一次，普通 4xx 不重试。取消释放锁及网络/定位监听。
- 常见独立天气问题走直达回复，不依赖模型多轮前言；复合任务仍交给现有工具循环，保留修改操作确认。
- 语音、会话、天气使用同一 traceId。记录查询开始/结束、合并、聊天回复、朗读开始/结束/取消；不记录密钥、原始录音或坐标。自动朗读遵守用户开关。

公开接口实测：中文“长沙”可解析；2026-10-01 的公开预报返回 Asia/Shanghai、current.time、温度、weather_code、daily 日期及降水概率。此次是公共 HTTP 实测，不是手机定位或完整语音链路验收。

## 验证记录

最终 `compileGithubReleaseKotlin :app:testGithubDebugUnitTest assembleGithubRelease generateSchedulePlusUpdateManifest exportBrandedGithubReleaseApk` 成功，耗时 **7 分 32 秒**，保留 R8、资源压缩、lintVital 与现有签名。日志为 `tmp/weather-voice-release-final.log`。

定向测试 **63/63 通过**，0 失败、0 错误、0 跳过：QuickWeatherTest 7、VoiceWaveformTest 3、RealtimeVoiceProtocolTest 8、AgentToolsTest 36、AgentTaskLoopTest 4、ColorOSCourseProviderContractTest 5。2026-10-01 13:13（本地时间）的 XML 测试报告属于最终代码；之前的 871 项全量通过属于上一个候选，本轮没有重跑全量套件。

- 公共天气 HTTP 请求实测成功；网络重试分支经源码检查，未声称断网设备回归已通过。
- `apksigner verify --verbose --print-certs` 通过，v2 签名有效，单签名者；证书 SHA-256 与既有“时序清单”一致：`a6510d8496d8b3e4397bd7f30a03488db9408d38b5b4864c67375d4038924886`。
- `aapt2 dump badging/xmltree` 核对最终 APK：应用名“时序清单”、`com.scheduleplus.student`、`1.0.1 / 36`、min API26、target API36；实际包内所有自有 Provider authority 按该包名隔离，旧 ColorOS authority/权限名及 sharedUserId 均不存在。查询其他应用的 `<queries>` authority 不是本应用注册的 Provider。
- 已实际从 APK ZIP 读取 `assets/licenses/SiriWave-MIT.txt`，与源码许可证全文一致。
- 构建 APK、本地交付副本与更新清单 SHA-256 一致。交付 **6,404,160 字节**：`app/build/outputs/delivery/时序清单-v1.0.1-正式版.apk`。
- APK SHA-256：`43c7500171b14ee36457ef7157b2b64931c747db297c0f316e121b61e6d27923`。
- 同目录提供 `.apk.sha256.txt` 和 `scheduleplus-update.json`；清单的远端 URL 尚未上传，不是已发布下载地址。
- `git diff --check` 通过；没有自动安装或运行新 APK。最后一次 `adb devices -l` 为空。

首次生产编译缺少 booleanOrNull 导入，已修复。随后测试编译发现旧 AgentTaskLoopTest 同步调用改为 suspend 的工具链，已改为 runBlocking；失败轮次不算通过。

## 本轮 diff 与源码

本轮 patch 使用上一交付源码 ZIP（SHA-256 `a2911c336ed08ddf91414226fbfef3a1de87c3c4a1eba0de7035a41f6b2109c0`）作为基线，逐文件比较，避免把工作树此前改动算成本轮修复。该基线另存为 `时序清单-v1.0.1-源码基线-上一候选.zip`。

- 本轮 diff：`app/build/outputs/delivery/时序清单-天气语音与浅色修复.patch`，可用 `git apply --reverse --check` 对当前源码校验；该命令只检查，不修改文件。
- 修改路径及前后 SHA-256：同目录 `时序清单-本轮修改清单.json`。
- 最新完整源码：同目录 `时序清单-v1.0.1-源码候选.zip`；排除私有后端、签名、凭据、本机配置、构建缓存和用户数据库/备份，ZIP 每个条目与源文件摘要核对。

关键改动为主题配置默认 `followSystemDarkMode=false`、设置表面透明度 `0.18→0.96`、Provider/permission 使用 `${applicationId}`、WEATHER_QUICK 异步结果回到同一会话、语音 final `delay(800)` 与 5 秒合并，以及真实音量驱动的 SiriWave 曲线。完整上下文见 patch。

## 实机验收步骤

1. 备份现有数据；分别在系统浅/深色下启动新包，检查设置弹层白底、无下层文字干扰；主动切深色后确认持久生效。旧偏好用户无需重置主题。
2. 首次进入查看定位用途说明；允许/拒绝分别询问“今天天气怎么样”和“长沙今天天气怎么样”；关闭定位服务后应给出可操作错误。
3. 开启语音，连续说同一句天气问题；核对 800ms 等待、同意图 5 秒内一次实际查询、单轮回复进入聊天、朗读开关与实际声音。采集 ShixuWeather/ShixuVoice 的同 traceId 摘要。
4. 测试查询中打断、朗读中打断、后台/离页、权限撤回、断网与鉴权失败；停止后麦克风、播放与连接释放。
5. 检查收起/展开声纹随音量变化、静音平线、等待圆点；用 Perfetto/帧统计测 60Hz 帧时间，另验大字体、壁纸和 API26 降级。
6. 保留原 SleepDown 安装新 时序清单，不卸载旧包；检查两图标启动、组件/分享各归自己的应用。

当前 ADB 未连接设备，以上实机项均未执行；真实云端 ASR/TTS 使用用户自行配置的服务凭据验收。

## 兼容性与风险

- applicationId、版本、数据库和现有“时序清单”证书保持；不通过卸载、清库或替换证书绕过冲突。
- ColorOS 源 Provider 的旧固定 authority 与权限名已改变，旧第三方代理若硬编码它们需同步更新；不能保留旧别名，否则两应用仍冲突。自己的桥接入口使用同一新 Contract。
- 已有深色/跟随系统偏好继续生效；白色默认只影响新配置。没有实机证据时不声称彻底排除其他包冲突来源。
- 天气受定位授权、系统定位供应器、Open-Meteo 网络/服务限制影响；预报有误差。
- 本轮修复与物理设备/服务验收分开，未声称像素级复刻或测得 60fps。
