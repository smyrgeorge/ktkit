package io.github.smyrgeorge.ktkit.ktor.httpclient

import io.ktor.client.engine.cio.CIOEngineConfig
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import io.ktor.serialization.ContentConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.reflect.TypeInfo
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charset
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.pluginOrNull
import kotlinx.coroutines.job
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.assertNull

class HttpClientFactoryTest {
    @Test
    fun createInstallsNegotiationTimeoutsAndTheDefaultRequest() {
        HttpClientFactory.create().use { client ->
            assertNotNull(client.pluginOrNull(ContentNegotiation))
            assertNotNull(client.pluginOrNull(HttpTimeout))
            assertNotNull(client.pluginOrNull(DefaultRequest))
        }
    }

    /** Logging is opt-in: it is off unless `enableLogging` is passed. */
    @Test
    fun loggingIsNotInstalledByDefault() {
        HttpClientFactory.create().use { client ->
            assertNull(client.pluginOrNull(Logging))
        }
    }

    @Test
    fun loggingIsInstalledWhenEnabled() {
        HttpClientFactory.create(enableLogging = true, logLevel = LogLevel.BODY).use { client ->
            assertNotNull(client.pluginOrNull(Logging))
        }
    }

    @Test
    fun maxConnectionsReachesTheEngineConfiguration() {
        HttpClientFactory.create().use { client ->
            assertEquals(128, assertIs<CIOEngineConfig>(client.engine.config).maxConnectionsCount)
        }
        HttpClientFactory.create(maxConnections = 7).use { client ->
            assertEquals(7, assertIs<CIOEngineConfig>(client.engine.config).maxConnectionsCount)
        }
    }

    @Test
    fun eachCallReturnsAnIndependentClient() {
        val a = HttpClientFactory.create()
        val b = HttpClientFactory.create()
        assertNotSame(a, b)
        a.close()
        assertFalse(a.coroutineContext.job.isActive)
        assertTrue(b.coroutineContext.job.isActive)
        b.close()
    }

    @Test
    fun configureCanInstallAPlugin() {
        HttpClientFactory.create { install(UserAgent) { agent = "test" } }.use { client ->
            assertNotNull(client.pluginOrNull(UserAgent))
        }
    }

    /** Without `negotiation`, bodies go out as kotlinx JSON. */
    @Test
    fun theDefaultCodecIsKotlinxJson() = runTest {
        val sent = sendCaptured(HttpClientFactory.create { install(captureAndAbort) }) {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("value", "x") })
        }
        assertEquals("""{"value":"x"}""", assertIs<TextContent>(sent.body).text)
    }

    /** `negotiation` replaces the default codec: the body goes out through the given converter only. */
    @Test
    fun negotiationReplacesTheDefaultCodec() = runTest {
        val sent = sendCaptured(
            HttpClientFactory.create(negotiation = { register(ContentType.Application.Json, StubConverter) }) {
                install(captureAndAbort)
            },
        ) {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("value", "ignored") })
        }
        assertEquals("stub", assertIs<TextContent>(sent.body).text)
    }

    /** A `defaultRequest` in `configure` adds to the factory's own: both headers go out. */
    @Test
    fun configureAddsToTheDefaultRequest() = runTest {
        val sent = sendCaptured(
            HttpClientFactory.create(negotiation = { json(testJson) }) {
                install(captureAndAbort)
                defaultRequest { header("X-AUTH-TOKEN", "t") }
            },
        )
        assertEquals("t", sent.headers["X-AUTH-TOKEN"])
        assertEquals(ContentType.Application.Json.toString(), sent.headers[HttpHeaders.Accept])
    }
}

private object StubConverter : ContentConverter {
    override suspend fun serialize(contentType: ContentType, charset: Charset, typeInfo: TypeInfo, value: Any?): OutgoingContent =
        TextContent("stub", contentType)

    override suspend fun deserialize(charset: Charset, typeInfo: TypeInfo, content: ByteReadChannel): Any? = null
}

/** Thrown by [captureAndAbort] after it records the request, so no test reaches the network. */
private class Captured(val request: HttpRequestBuilder) : RuntimeException()

/** Records the fully built request at the send stage, then aborts it with [Captured]. */
private val captureAndAbort = createClientPlugin("CaptureAndAbort") {
    on(Send) { request -> throw Captured(request) }
}

/** POSTs through [client] (which must install [captureAndAbort]) and returns the request it would have sent. */
private suspend fun sendCaptured(
    client: io.ktor.client.HttpClient,
    builder: HttpRequestBuilder.() -> Unit = {},
): HttpRequestBuilder = client.use {
    assertFailsWith<Captured> { it.post("http://localhost/x", builder) }.request
}
