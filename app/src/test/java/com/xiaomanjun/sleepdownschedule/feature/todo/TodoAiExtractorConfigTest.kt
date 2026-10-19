package com.xiaomanjun.sleepdownschedule.feature.todo

import com.xiaomanjun.sleepdownschedule.feature.importing.AiAuthType
import com.xiaomanjun.sleepdownschedule.feature.importing.AiEndpointStyle
import com.xiaomanjun.sleepdownschedule.feature.importing.AiImportSettings
import com.xiaomanjun.sleepdownschedule.feature.importing.AiProviderPresets
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoAiExtractorConfigTest {
    @Test
    fun emptyLegacyDefaultsMigrateToFollowingTheMainConnection() {
        assertTrue(
            shouldTodoFollowMainConnection(
                hasSavedFlag = false,
                savedFlag = false,
                apiKey = "",
                apiUrl = TodoAiExtractor.DEFAULT_URL,
                textModel = "glm-4-flash",
                visionModel = "glm-4v-flash"
            )
        )
        assertTrue(TodoAiConfig().followMainConnection)
    }

    @Test
    fun savedStandaloneCredentialsOrCustomSettingsRemainIndependent() {
        assertFalse(shouldFollow(apiKey = "saved-key"))
        assertFalse(shouldFollow(apiKey = "saved-encrypted-credential"))
        assertFalse(shouldFollow(apiUrl = "https://example.test/v1/chat/completions"))
        assertFalse(shouldFollow(textModel = "custom-text"))
        assertFalse(shouldFollow(visionModel = "custom-vision"))
        assertTrue(shouldFollow(hasFlag = true, savedFlag = true, apiKey = "saved-key"))
        assertFalse(shouldFollow(hasFlag = true, savedFlag = false))
    }

    @Test
    fun followingUsesTheCurrentMainModelForTextAndImages() {
        val first = AiImportSettings(
            AiProviderPresets.zhipu.copy(defaultModel = "glm-current"),
            "main-secret"
        )
        val todo = TodoAiConfig(followMainConnection = true)
        assertEquals("glm-current", resolveTodoAiRequestConfig(todo, first).model)
        assertEquals("glm-current", resolveTodoAiRequestConfig(todo, first, useVisionModel = true).model)

        val switched = first.copy(profile = first.profile.copy(defaultModel = "glm-next"))
        assertEquals("glm-next", resolveTodoAiRequestConfig(todo, switched).model)
    }

    @Test
    fun standaloneKeepsItsSeparateTextAndVisionModels() {
        val todo = TodoAiConfig(
            apiUrl = "https://standalone.test/v1/chat/completions",
            apiKey = "standalone-secret",
            textModel = "text-model",
            visionModel = "vision-model",
            followMainConnection = false
        )
        assertEquals("text-model", resolveTodoAiRequestConfig(todo, null).model)
        val vision = resolveTodoAiRequestConfig(todo, null, useVisionModel = true)
        assertEquals("vision-model", vision.model)
        assertEquals(todo.apiUrl, vision.endpoint())
        assertTrue(buildTodoAiRequestBody(vision, "提取", emptyList()).keys.none { it == "response_format" })
    }

    @Test
    fun responsesRequestUsesConfiguredModelSchemaAndImageParts() {
        val settings = AiImportSettings(AiProviderPresets.openAI, "main-secret")
        val config = resolveTodoAiRequestConfig(TodoAiConfig(), settings)
        assertEquals(AiEndpointStyle.RESPONSES, config.endpointStyle)
        assertEquals("https://api.openai.com/v1/responses", config.endpoint())
        assertEquals(AiAuthType.ApiKeyBearer, config.authType)

        val body = buildTodoAiRequestBody(config, "提取待办", listOf("data:image/png;base64,abc"))
        assertEquals(settings.profile.defaultModel, body["model"]?.jsonPrimitive?.content)
        assertEquals("json_schema", body["text"]?.jsonObject?.get("format")?.jsonObject?.get("type")?.jsonPrimitive?.content)
        val content = body["input"]!!.jsonArray.first().jsonObject["content"]!!.jsonArray
        assertEquals("input_text", content[0].jsonObject["type"]?.jsonPrimitive?.content)
        assertEquals("input_image", content[1].jsonObject["type"]?.jsonPrimitive?.content)

        val providerRoot = settings.copy(profile = settings.profile.copy(baseUrl = "https://api.openai.com"))
        assertEquals(
            "https://api.openai.com/v1/responses",
            resolveTodoAiRequestConfig(TodoAiConfig(), providerRoot).endpoint()
        )
    }

    @Test
    fun responsesOutputTextIsParsedForTheExistingTodoDecoder() {
        val parsed = parseTodoAiResponseText(
            """{"output":[{"type":"message","content":[{"type":"output_text","text":"{\"title\":\"交作业\",\"description\":\"\",\"due_at\":null,\"priority\":\"NORMAL\",\"repeat_rule\":\"NONE\",\"subtasks\":[]}"}]}]}""",
            AiEndpointStyle.RESPONSES
        )
        assertTrue(parsed.startsWith("{"))
        assertTrue(parsed.contains("交作业"))
    }

    private fun shouldFollow(
        hasFlag: Boolean = false,
        savedFlag: Boolean = true,
        apiKey: String = "",
        apiUrl: String = TodoAiExtractor.DEFAULT_URL,
        textModel: String = "glm-4-flash",
        visionModel: String = "glm-4v-flash"
    ) = shouldTodoFollowMainConnection(hasFlag, savedFlag, apiKey, apiUrl, textModel, visionModel)
}
