package io.github.smyrgeorge.ktkit.ktor.httpclient.impl

import arrow.core.raise.either
import io.github.smyrgeorge.ktkit.ktor.httpclient.AbstractRestClient
import io.github.smyrgeorge.ktkit.ktor.httpclient.RestClientErrorSpec
import io.github.smyrgeorge.ktkit.ktor.httpclient.apiErrorBody
import io.github.smyrgeorge.ktkit.ktor.httpclient.demoClient
import io.github.smyrgeorge.ktkit.ktor.httpclient.demoMapError
import io.github.smyrgeorge.ktkit.ktor.httpclient.jsonHeaders
import io.github.smyrgeorge.ktkit.ktor.httpclient.mockHttpClient
import io.github.smyrgeorge.ktkit.ktor.httpclient.testJson
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.pluginOrNull
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/**
 * [RestClient] adds nothing to [AbstractRestClient] — it only makes it constructible. What is worth
 * pinning down is therefore its wiring; the operations themselves are covered by
 * `AbstractRestClientTest`.
 */
class RestClientTest {
    /**
     * The widening assignment is the assertion: it would not compile if [RestClient] stopped being
     * an [AbstractRestClient], which is the one thing the class exists to provide.
     */
    @Test
    fun itIsAnAbstractRestClientWithNothingAdded() {
        val base: AbstractRestClient = demoClient(MockEngine { respond("", HttpStatusCode.OK) })
        assertEquals("https://api.test/", base.baseUrl)
    }

    @Test
    fun theConstructorArgumentsAreExposed() {
        val http = mockHttpClient(MockEngine { respond("", HttpStatusCode.OK) })
        val client = RestClient(json = testJson, baseUrl = "https://api.test/", client = http, mapError = demoMapError())
        assertSame(testJson, client.json)
        assertSame(http, client.client)
        assertEquals("https://api.test/", client.baseUrl)
    }

    /** `baseUrl` defaults to empty, so the operation's `uri` can be an absolute URL. */
    @Test
    fun baseUrlDefaultsToEmpty() {
        val client = RestClient(
            json = testJson,
            client = mockHttpClient(MockEngine { respond("", HttpStatusCode.OK) }),
            mapError = demoMapError(),
        )
        assertEquals("", client.baseUrl)
    }

    /** Omitting `client` falls back to [io.github.smyrgeorge.ktkit.ktor.httpclient.HttpClientFactory]. */
    @Test
    fun theClientDefaultsToAFactoryBuiltOne() {
        val client = RestClient(json = testJson, baseUrl = "https://api.test/", mapError = demoMapError())
        assertNotNull(client.client.pluginOrNull(ContentNegotiation))
        client.client.close()
    }

    /** The `mapError` it is given is the one that runs. */
    @Test
    fun itUsesTheMapErrorItWasGiven() = runTest {
        val client = RestClient(
            json = testJson,
            baseUrl = "https://api.test/",
            client = mockHttpClient(MockEngine { respond(apiErrorBody(404), HttpStatusCode.NotFound, jsonHeaders) }),
            mapError = { RestClientErrorSpec.RestClientRequestError(RuntimeException("mine: ${status.value}")) },
        )
        val error = assertIs<RestClientErrorSpec.RestClientRequestError>(either { client.get<String>("a") }.leftOrNull())
        assertEquals("mine: 404", error.cause.message)
    }
}
