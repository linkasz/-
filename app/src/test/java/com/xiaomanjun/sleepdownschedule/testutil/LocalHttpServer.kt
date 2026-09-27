package com.xiaomanjun.sleepdownschedule.testutil

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import kotlin.concurrent.thread

internal data class LocalHttpRequest(
    val method: String,
    val path: String,
    val headers: Map<String, String>,
    val body: String
)

internal data class LocalHttpResponse(
    val status: Int,
    val headers: Map<String, String> = emptyMap(),
    val body: ByteArray = byteArrayOf()
)

/** A small loopback HTTP fixture that works without the optional JDK jdk.httpserver module. */
internal class LocalHttpServer(
    private val respond: (LocalHttpRequest) -> LocalHttpResponse
) : AutoCloseable {
    private val server = ServerSocket(0, 0, InetAddress.getByName("127.0.0.1"))
    private val worker = thread(name = "local-http-test-server", isDaemon = true) {
        while (!server.isClosed) {
            val socket = try {
                server.accept()
            } catch (_: Exception) {
                break
            }
            socket.use(::handle)
        }
    }

    val baseUrl: String = "http://127.0.0.1:${server.localPort}"

    override fun close() {
        server.close()
        worker.join(1_000)
    }

    private fun handle(socket: Socket) {
        socket.soTimeout = 10_000
        val input = socket.getInputStream()
        val requestLine = input.readHttpLine()?.toString(StandardCharsets.ISO_8859_1).orEmpty()
        val requestParts = requestLine.split(' ', limit = 3)
        val headers = linkedMapOf<String, String>()
        while (true) {
            val line = input.readHttpLine() ?: break
            if (line.isEmpty()) break
            val decoded = line.toString(StandardCharsets.ISO_8859_1)
            val separator = decoded.indexOf(':')
            if (separator > 0) {
                headers[decoded.substring(0, separator).trim().lowercase()] =
                    decoded.substring(separator + 1).trim()
            }
        }
        val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
        val bodyBytes = input.readNBytes(contentLength)
        val response = respond(
            LocalHttpRequest(
                method = requestParts.getOrElse(0) { "" },
                path = requestParts.getOrElse(1) { "" },
                headers = headers,
                body = bodyBytes.toString(StandardCharsets.UTF_8)
            )
        )
        val output = socket.getOutputStream()
        val reason = when (response.status) {
            200 -> "OK"
            304 -> "Not Modified"
            else -> "Error"
        }
        output.write("HTTP/1.1 ${response.status} $reason\r\n".toByteArray(StandardCharsets.ISO_8859_1))
        response.headers.forEach { (name, value) ->
            output.write("$name: $value\r\n".toByteArray(StandardCharsets.ISO_8859_1))
        }
        output.write("Content-Length: ${response.body.size}\r\nConnection: close\r\n\r\n".toByteArray(StandardCharsets.ISO_8859_1))
        output.write(response.body)
        output.flush()
    }

    private fun InputStream.readHttpLine(): ByteArray? {
        val line = ByteArrayOutputStream()
        var previous = -1
        while (true) {
            val next = read()
            if (next < 0) return if (line.size() == 0 && previous < 0) null else line.toByteArray()
            if (previous == '\r'.code && next == '\n'.code) {
                val bytes = line.toByteArray()
                return if (bytes.lastOrNull() == '\r'.code.toByte()) bytes.copyOf(bytes.size - 1) else bytes
            }
            line.write(next)
            previous = next
        }
    }
}
