package com.xiaomanjun.sleepdownschedule.feature.agent.voice

/** Never display a server body: it may echo credentials, URLs or a user's transcript.
 * Use its description for classification only, and expose a bounded identifier.
 */
internal fun voiceFailureMessage(failure: VoiceEvent.Failure, settings: VoiceSettings): String {
    val category = (failure.code + " " + failure.detail).lowercase()
    val hint = when {
        "network" in category || "closed_" in category -> "语音连接中断，请检查网络后重试"
        listOf("authentication", "unauthorized", "invalid_api_key", "apikey", "api key", "http_401").any { it in category } ->
            "语音鉴权失败，请核对 API Key 所属地域与业务空间"
        "http_403" in category || "permission" in category || "accessdenied" in category || "access_denied" in category ->
            "当前账号无权使用此模型，请核对百炼模型权限、地域与业务空间"
        "http_429" in category || "rate_limit" in category || "quota" in category -> "语音服务额度或并发受限，请检查账号后重试"
        "model" in category && ("not" in category || "invalid" in category || "unsupported" in category) ->
            "所选模型在当前地域或业务空间不可用，请检查模型名称及授权"
        "voice" in category -> "当前模型不支持所选音色，请重新选择音色"
        "invalid_protocol" in category -> "语音服务返回了无法识别的协议事件"
        else -> "语音服务拒绝此请求，请检查模型及会话配置"
    }
    val code = failure.code.takeIf { it.matches(Regex("[A-Za-z0-9_.:-]{1,64}")) &&
        !it.startsWith("sk-", ignoreCase = true) && (settings.apiKey.isBlank() || !it.contains(settings.apiKey)) } ?: "service_error"
    val location = if (settings.platform == VoicePlatform.ALIYUN)
        " · ${if (settings.region == "cn-beijing") "北京" else "新加坡"}" else ""
    return "$hint\n${settings.model}$location · $code"
}
