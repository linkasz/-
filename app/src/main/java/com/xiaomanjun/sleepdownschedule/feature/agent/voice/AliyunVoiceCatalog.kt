package com.xiaomanjun.sleepdownschedule.feature.agent.voice

/** Model-specific built-in voices, checked against the official list on 2026-10-01.
 * https://help.aliyun.com/zh/model-studio/omni-voice-list
 * ASR models produce text only; their replies use Android TTS, never an Omni voice.
 */
internal object AliyunVoiceCatalog {
    private val common = linkedMapOf(
        "Tina" to "甜甜", "Cindy" to "林欣宜", "Liora Mira" to "清欢",
        "Raymond" to "林川野", "Theo Calm" to "予安", "Serena" to "苏瑶",
        "Maia" to "四月", "Evan" to "江晨", "Qiao" to "小乔妹", "Momo" to "茉兔",
        "Wil" to "伟伦", "Angel" to "安琪", "Li Cassian" to "李公公", "Mia" to "舒然",
        "Joyner" to "阿逗", "Gold" to "金爷", "Katerina" to "卡捷琳娜", "Ryan" to "甜茶"
    )
    private val omni35 = common + linkedMapOf("Sunnybobi" to "知芝", "Ethan" to "晨煦", "Harvey" to "厚")
    private val omni38 = common + linkedMapOf(
        "Zane" to "泽恩", "Cici" to "绵绵", "Sunny" to "晴儿 · 四川话",
        "Dylan" to "晓东 · 北京话", "Eric" to "程川 · 四川话", "Peter" to "李彼得 · 天津话",
        "Rocky" to "阿强 · 粤语", "Kiki" to "阿清 · 粤语", "Sohee" to "素熙"
    )

    fun transcriptionOnly(model: String) = model.startsWith("qwen3-asr-")
    fun voices(model: String): List<String> = when {
        transcriptionOnly(model) -> listOf("系统默认")
        model.startsWith("qwen3.8-omni-") -> omni38.keys.toList()
        model.startsWith("qwen3.5-omni-") -> omni35.keys.toList()
        else -> emptyList()
    }
    fun label(voice: String): String = (omni35[voice] ?: omni38[voice])?.let { "$it · $voice" } ?: voice
}
