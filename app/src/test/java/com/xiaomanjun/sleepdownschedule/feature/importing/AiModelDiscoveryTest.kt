package com.xiaomanjun.sleepdownschedule.feature.importing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class AiModelDiscoveryTest {
    @Test
    fun modelEndpointUsesProviderNormalizationAndPreservesGatewayPrefix() {
        assertEquals("https://api.openai.com/v1/models", aiModelListEndpoint(AiProviderPresets.openAI.copy(baseUrl = "https://api.openai.com")))
        assertEquals("https://api.deepseek.com/models", aiModelListEndpoint(AiProviderPresets.deepSeek.copy(baseUrl = "https://api.deepseek.com/v1/chat/completions")))
        assertEquals("https://gateway.example/tenant/v1/models", aiModelListEndpoint(AiProviderPresets.custom.copy(baseUrl = " https://gateway.example/tenant/v1/responses/ ")))
        assertEquals("https://gateway.example/v1/models", aiModelListEndpoint(AiProviderPresets.custom.copy(baseUrl = "https://gateway.example/v1/models")))
        assertEquals("https://api.xiaomimimo.com/v1/models", aiModelListEndpoint(AiProviderPresets.mimo))
    }

    @Test
    fun rejectsInvalidOrCredentialBearingUrlsAndManagedProvider() {
        listOf("", "ftp://example.com", "https://user:secret@example.com/v1", "https://example.com/v1?key=secret", "https://example.com/v1#secret").forEach {
            assertThrows(IllegalArgumentException::class.java) { aiModelListEndpoint(AiProviderPresets.custom.copy(baseUrl = it)) }
        }
        assertThrows(IllegalArgumentException::class.java) { aiModelListEndpoint(AiProviderPresets.dailyFree) }
        assertThrows(IllegalArgumentException::class.java) { aiModelListEndpoint(AiProviderPresets.none) }
    }

    @Test
    fun parsesIdsAndNamesWithoutGuessingFromModelNames() {
        val models = parseAiModelList("""{"data":[{"id":"vendor/model-b","name":"Model B"},{"id":" model-a "},{"id":"model-a"},{"id":null},{"id":123},{}]}""")
        assertEquals(listOf(DiscoveredAiModel("model-a"), DiscoveredAiModel("vendor/model-b", "Model B")), models)
        assertEquals(listOf(DiscoveredAiModel("abc")), parseAiModelList("""["abc","abc","",null]"""))
    }

    @Test
    fun distinguishesEmptyCatalogFromBrokenOrPartialResponse() {
        assertTrue(parseAiModelList("""{"data":[]}""").isEmpty())
        listOf("<html>login</html>", "{}", """{"error":{"message":"secret"}}""", """{"data":[{"name":"not an id"}]}""", """{"data":[{"id":"a"}],"has_more":true}""").forEach { body ->
            val failure = assertThrows(IllegalStateException::class.java) { parseAiModelList(body) }
            assertFalse(failure.message.orEmpty().contains("secret"))
        }
    }

    @Test
    fun addingModelsPreservesCurrentOrderAndExistingChoices() {
        assertEquals(listOf("existing", "default", "new"), mergeAiModelIds(listOf("existing", "default"), listOf("EXISTING", " new ", "")))
        assertEquals(listOf("existing"), mergeAiModelIds(listOf("existing"), emptyList()))
    }

    @Test
    fun usesDraftCredentialsAndCorrectAuthHeadersWithoutModelOrPost() = runBlocking {
        withServer { server ->
            var receivedHeader: String? = null
            var method: String? = null
            server.createContext("/v1/models") { exchange ->
                method = exchange.requestMethod
                receivedHeader = exchange.requestHeaders["authorization"] ?: exchange.requestHeaders["api-key"]
                val body = """{"data":[{"id":"remote-model"}]}""".toByteArray()
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
            for ((auth, expected) in listOf(AiAuthType.ApiKeyBearer to "Bearer draft-key", AiAuthType.CustomHeader to "draft-key")) {
                val profile = AiProviderPresets.custom.copy(baseUrl = endpoint(server), defaultModel = "", authType = auth)
                assertEquals(listOf("remote-model"), discoverAiModels(AiImportSettings(profile, " draft-key ")).map { it.id })
                assertEquals("GET", method)
                assertEquals(expected, receivedHeader)
            }
        }
    }

    @Test
    fun errorsNeverExposeRemoteBodyOrFollowRedirects() = runBlocking {
        withServer { server ->
            var redirected = false
            server.createContext("/v1/models") { exchange ->
                exchange.responseHeaders["Location"] = "${endpoint(server)}/redirect"
                val body = "echoed-private-api-key".toByteArray()
                exchange.sendResponseHeaders(302, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
            server.createContext("/v1/redirect") { redirected = true; it.close() }
            val error = runCatching { discoverAiModels(AiImportSettings(AiProviderPresets.custom.copy(baseUrl = endpoint(server)), "test-key")) }.exceptionOrNull()
            assertNotNull(error)
            assertFalse(error!!.message.orEmpty().contains("echoed-private-api-key"))
            assertFalse(redirected)
        }
    }

    @Test
    fun changingOrLeavingConnectionCanCancelAnInflightRequest() = runBlocking {
        withServer { server ->
            val received = CountDownLatch(1)
            val release = CountDownLatch(1)
            server.createContext("/v1/models") { exchange ->
                received.countDown()
                release.await(5, TimeUnit.SECONDS)
                exchange.close()
            }
            try {
                val request = async { discoverAiModels(AiImportSettings(AiProviderPresets.custom.copy(baseUrl = endpoint(server)), "test-key")) }
                withContext(Dispatchers.IO) { assertTrue(received.await(3, TimeUnit.SECONDS)) }
                withTimeout(2_000) { request.cancelAndJoin() }
                assertTrue(request.isCancelled)
            } finally {
                release.countDown()
            }
        }
    }

    private fun endpoint(server: ModelListTestServer) = "http://127.0.0.1:${server.port}/v1"

    private suspend fun withServer(block: suspend (ModelListTestServer) -> Unit) {
        val server = ModelListTestServer()
        try { block(server) } finally { server.close() }
    }

    // Android's unit-test compile classpath has java.net, but not the JDK-only httpserver module.
    private class ModelListTestServer : AutoCloseable {
        private val socket = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
        private val executor = Executors.newCachedThreadPool()
        private val handlers = ConcurrentHashMap<String, (Exchange) -> Unit>()
        val port: Int get() = socket.localPort

        init {
            executor.execute {
                while (!socket.isClosed) {
                    val client = runCatching { socket.accept() }.getOrNull() ?: break
                    executor.execute {
                        client.use {
                            it.soTimeout = 5_000
                            val input = it.getInputStream().bufferedReader(Charsets.US_ASCII)
                            val first = input.readLine()?.split(' ') ?: return@execute
                            val headers = mutableMapOf<String, String>()
                            while (true) {
                                val line = input.readLine()?.takeIf(String::isNotEmpty) ?: break
                                headers[line.substringBefore(':').lowercase()] = line.substringAfter(':').trim()
                            }
                            handlers[first[1]]?.invoke(Exchange(it, first[0], headers))
                        }
                    }
                }
            }
        }

        fun createContext(path: String, handler: (Exchange) -> Unit) { handlers[path] = handler }
        override fun close() { socket.close(); executor.shutdownNow() }
    }

    private class Exchange(
        private val socket: Socket,
        val requestMethod: String,
        val requestHeaders: Map<String, String>
    ) {
        val responseHeaders = mutableMapOf<String, String>()
        val responseBody: OutputStream get() = socket.getOutputStream()
        fun sendResponseHeaders(status: Int, size: Long) {
            val header = "HTTP/1.1 $status Test\r\nContent-Length: $size\r\nConnection: close\r\n" +
                responseHeaders.entries.joinToString("") { "${it.key}: ${it.value}\r\n" } + "\r\n"
            responseBody.write(header.toByteArray(Charsets.US_ASCII))
        }
        fun close() { socket.close() }
    }
}
