package com.xiaomanjun.sleepdownschedule.feature.agent.voice

import com.xiaomanjun.sleepdownschedule.feature.importing.DiscoveredAiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceModelDiscoveryTest {
    @Test
    fun onlyKeepsRealtimeFamiliesSupportedByTheSelectedVoiceProtocol() {
        val aliyun = filterVoiceModelCatalog(VoicePlatform.ALIYUN, listOf(
            DiscoveredAiModel("qwen3-asr-flash-realtime"),
            DiscoveredAiModel("qwen3-asr-flash-realtime-2026-02-10"),
            DiscoveredAiModel("qwen3.5-omni-plus-realtime"),
            DiscoveredAiModel("qwen3.8-omni-flash-realtime"),
            DiscoveredAiModel("qwen3.9-omni-flash-realtime"),
            DiscoveredAiModel("qwen3-tts-flash"),
            DiscoveredAiModel("qwen-plus")
        )).map { it.id }
        assertEquals(listOf("qwen3-asr-flash-realtime", "qwen3-asr-flash-realtime-2026-02-10", "qwen3.5-omni-plus-realtime", "qwen3.8-omni-flash-realtime"), aliyun)

        val openAi = filterVoiceModelCatalog(VoicePlatform.OPENAI, listOf(
            DiscoveredAiModel("gpt-realtime-mini"), DiscoveredAiModel("gpt-4o-realtime-preview"), DiscoveredAiModel("gpt-4o-mini")
        )).map { it.id }
        assertEquals(listOf("gpt-realtime-mini"), openAi)

        val glm = filterVoiceModelCatalog(VoicePlatform.GLM, listOf(
            DiscoveredAiModel("glm-realtime-air"), DiscoveredAiModel("glm-4.5-flash")
        )).map { it.id }
        assertEquals(listOf("glm-realtime-air"), glm)
    }

    @Test
    fun siliconFlowOffersOnlyPairsWhoseAsrAndTtsAreBothListed() {
        val result = filterVoiceModelCatalog(VoicePlatform.SILICON_FLOW, listOf(
            DiscoveredAiModel("FunAudioLLM/SenseVoiceSmall"),
            DiscoveredAiModel("FunAudioLLM/CosyVoice2-0.5B"),
            DiscoveredAiModel("fnlp/MOSS-TTSD-v0.5")
        ))
        assertEquals(setOf(SiliconVoicePreset.SENSE_COSY.id, SiliconVoicePreset.SENSE_MOSS.id), result.map { it.id }.toSet())
    }

    @Test
    fun parsesOfficialAliyunPageShapeAndRequiresPaginationTotal() {
        val page = parseAliyunModelPage("""{"output":{"total":21,"page_no":1,"page_size":20,"models":[{"model":"qwen3.8-omni-flash-realtime","name":"Omni realtime"},{"model":"qwen-plus"},{"model":"qwen-plus"},{"name":"missing-id"}]}}""")
        assertEquals(21, page.total)
        assertEquals(listOf(DiscoveredAiModel("qwen3.8-omni-flash-realtime", "Omni realtime"), DiscoveredAiModel("qwen-plus")), page.models)
        assertTrue(runCatching { parseAliyunModelPage("""{"output":{"models":[]}}""") }.isFailure)
    }

    @Test
    fun manualModelsMustMatchSupportedProtocolAndPersistedOptionsKeepBuiltins() {
        assertTrue(isSupportedVoiceModel(VoicePlatform.ALIYUN, "qwen3.8-omni-flash-realtime"))
        assertFalse(isSupportedVoiceModel(VoicePlatform.ALIYUN, "qwen3.9-omni-flash-realtime"))
        assertFalse(isSupportedVoiceModel(VoicePlatform.OPENAI, "gpt-4o-realtime-preview"))
        assertFalse(isSupportedVoiceModel(VoicePlatform.GLM, "glm-4.5-flash"))
        val settings = VoiceSettings(VoicePlatform.GLM).withAddedModels(listOf("glm-realtime-pro", "glm-4.5-flash"))
        assertEquals(listOf("glm-realtime-flash", "glm-realtime-air", "glm-realtime-pro"), settings.modelOptions)
        assertEquals("glm-realtime-pro", settings.withModel("glm-realtime-pro").model)
    }

    @Test
    fun manualModelsMustAlsoBeResolvableByTheCaseSensitiveVoiceCatalog() {
        assertFalse(isSupportedVoiceModel(VoicePlatform.ALIYUN, "QWEN3.8-OMNI-FLASH-REALTIME"))
        assertFalse(isSupportedVoiceModel(VoicePlatform.SILICON_FLOW, SiliconVoicePreset.SENSE_COSY.id.uppercase()))
        val dated = VoiceSettings(VoicePlatform.ALIYUN).withModel("qwen3.8-omni-flash-realtime-2026-07-14")
        assertTrue(dated.availableVoices.isNotEmpty())
        assertEquals(AliyunVoiceCatalog.voices("qwen3.8-omni-flash-realtime"), dated.availableVoices)
    }
}
