package com.xiaomanjun.sleepdownschedule.core.remoteconfig

import com.xiaomanjun.sleepdownschedule.testutil.LocalHttpResponse
import com.xiaomanjun.sleepdownschedule.testutil.LocalHttpServer
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteConfigClientTest {
    @Test
    fun bootstrapUsesEtagAndAccepts304() {
        val receivedEtag = AtomicReference<String?>(null)
        val server = LocalHttpServer { request ->
            receivedEtag.set(request.headers["if-none-match"])
            if (receivedEtag.get() == "W/\"bootstrap-9-v26\"") {
                LocalHttpResponse(304)
            } else {
                val body = """{"schemaVersion":1,"serverTime":1787000000,"notices":[],"agreements":{"privacy":null,"terms":null},"ai":null}"""
                    .toByteArray()
                LocalHttpResponse(
                    status = 200,
                    headers = mapOf(
                        "Content-Type" to "application/json",
                        "ETag" to "W/\"bootstrap-9-v26\""
                    ),
                    body = body
                )
            }
        }
        try {
            val client = RemoteConfigClient(server.baseUrl)
            val first = client.fetchBootstrap(null)
            assertTrue(first is BootstrapFetchResult.Updated)
            first as BootstrapFetchResult.Updated
            assertEquals("W/\"bootstrap-9-v26\"", first.etag)

            val second = client.fetchBootstrap(first.etag)
            assertEquals(BootstrapFetchResult.NotModified, second)
            assertEquals(first.etag, receivedEtag.get())
        } finally {
            server.close()
        }
    }
}
