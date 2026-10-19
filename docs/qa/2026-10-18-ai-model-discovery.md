# AI 平台模型列表获取与添加

## 当前实现

- AI 设置的“连接与模型”分组新增“获取可用模型”。使用当前编辑的接口地址与 API Key；未填写新 Key 时使用该平台本机已保存的 Key，不需要预先选择模型或保存配置。
- 对现有 OpenAI / Responses / OpenAI 兼容平台请求 `GET <规范化基址>/models`，沿用平台鉴权类型（Bearer 或 MiMo `api-key`）。保留网关路径前缀，不尝试其他域名、不跟随重定向、不调用生成接口。
- 获取后搜索模型名称或 ID，多选、全选搜索结果、清空选择、确认添加；已经配置的条目标记为“已添加”。新增项合并到平台原有模型列表，保留当前模型、既有模型顺序及平台隔离，复用原有设置保存和模型菜单。
- 所有自备服务商均可手动补充模型 ID。平台不支持列表接口、Key 权限不足、空列表和网络异常均有提示，不以静态预设冒充获取结果。
- 设置页显示已有的各平台预设。新增模型重新进入设置时继续按已配置选项识别，避免误显示为自定义输入。
- 获取状态属于设置会话，列表滚动不会销毁请求；更换平台、地址、Key 或退出页面时取消。明确取消后才允许重试，避免旧请求的结束状态覆盖新请求。
- 读取有连接、响应和整体超时及 4 MiB 响应上限。错误正文不会显示、保存或写入日志，密钥不进入诊断。未改变 Room、备份结构、版本、签名和业务确认保存接口。

## 接口依据与范围

- [OpenAI List models](https://developers.openai.com/api/reference/resources/models/methods/list)：读取模型列表的 `data[].id`。
- [DeepSeek Lists Models](https://api-docs.deepseek.com/api/list-models/)：`GET /models`，读取 `data[].id` 与可选的 `name`。
- 兼容接口也接受顶层模型数组和字符串条目。返回格式不兼容或声明仍有后续分页时明确失败，不把部分结果当成完整列表。
- 列表代表平台返回的目录，不承诺每个模型都能在本应用中用于对话、图片识别或工具调用；继续使用原有能力设置和连接测试。未新增 Gemini / Anthropic 原生推理协议适配。

## 验证

已执行：

- `:app:compileGithubReleaseKotlin` 与 `:app:compileGithubDebugKotlin` 通过。
- `:app:testGithubDebugUnitTest` 定向运行 `AiModelDiscoveryTest`（8 项）与 `AiProviderPresetsTest`（10 项），共 18 项通过，0 失败、0 错误。最终测试任务 `BUILD SUCCESSFUL`。
- 本地 Socket HTTP 服务验证 GET 路径、未保存草稿 Key、Bearer / MiMo `api-key` 鉴权、取消、重定向拒绝、远端错误正文不暴露；解析测试覆盖空目录、无效结构、分页未完成和模型 ID 合并。取消场景本次耗时 0.024 秒，仅代表 JVM 测试结果。
- 本轮两个既有源码文件的 `git diff --check` 通过；新文件另检查行尾空白及合并标记。
- 本机有效 JDK 为 `D:/Android/Android Studio/jbr`，依赖缓存使用仓库内 `.gradle-user-home`，通过 `ANDROID_HOME` 给复合构建指定已安装 SDK。使用临时参数 `-Dorg.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8`、单 worker、离线模式完成验证，没有改写长期 Gradle 配置。

测试使用本地服务及虚拟凭据；不读取真实账户 Key，不操作 USB 手机。本轮不生成新 APK，不进行安装、远端推送或发布。

人工待验：浅深主题、窄屏与大字体、键盘搜索、跨平台切换、添加后退出并重新进入、真实平台模型目录及选中模型的连接测试。模拟或单元测试不作为手机视觉与性能验收结论。

## 工作树恢复

首次编译暴露了上一轮未恢复暂存导致的跨版本不一致。原暂存对象 `c408757af1f78089408bad96ed867e278938ec14` 仍可读取；逐项比较确认 206 个路径与暂存前一致，两个本轮修改的设置文件另行合并。

- 先恢复本地 stash 引用，保留该暂存，不执行 pop/drop。
- 在 `tmp/ai-model-discovery-recovery-before/` 保存恢复前文件及摘要清单，再恢复 206 个未继续变化的路径。该目录仅供本地恢复，不提交或上传。
- 两个设置文件保留本轮功能；服务商列表继续排除原先已退役的远程免费平台。
- 保留原有未跟踪文件、人格真实核心、私有后端和宣传视频工程。没有 reset、清库、安装或改写 Git 历史。
