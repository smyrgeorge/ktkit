package io.github.smyrgeorge.ktkit.ktor.httpclient.impl

import arrow.core.raise.either
import io.github.smyrgeorge.ktkit.api.error.ErrorSpec
import io.github.smyrgeorge.ktkit.ktor.httpclient.RestClientErrorSpec
import io.github.smyrgeorge.ktkit.ktor.httpclient.RestErrorMapper
import io.github.smyrgeorge.ktkit.ktor.httpclient.apiErrorBody
import io.github.smyrgeorge.ktkit.ktor.httpclient.htmlHeaders
import io.github.smyrgeorge.ktkit.ktor.httpclient.jsonHeaders
import io.github.smyrgeorge.ktkit.ktor.httpclient.mockHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** What [RestClient] adds to the core client: [RestErrorMapper.default], which reads a ktkit service's errors. */
class RestClientTest {
    private fun client(engine: MockEngine) = RestClient(baseUrl = "https://api.test/", client = mockHttpClient(engine))

    /** The whole RFC 9457 envelope survives into the error, and its own status is the one reported. */
    @Test
    fun anApiErrorBodyBecomesAReceiveError() = runTest {
        val c = client(MockEngine { respond(apiErrorBody(500, detail = "boom"), HttpStatusCode.NotFound, jsonHeaders) })
        val error = assertIs<RestClientErrorSpec.RestClientReceiveError>(either { c.get<String>("a") }.leftOrNull())
        assertEquals(ErrorSpec.HttpStatus.INTERNAL_SERVER_ERROR, error.httpStatus)
        assertEquals("nope", error.cause.title)
        assertEquals("req-7", error.cause.requestId)
        assertContains(error.message, "boom")
    }

    /** The default sets no `unauthorized`, so a service's own 401 keeps its envelope. */
    @Test
    fun anUnauthorizedFromTheServiceDecodesLikeAnyOtherStatus() = runTest {
        val c = client(
            MockEngine { respond(apiErrorBody(401, detail = "expired"), HttpStatusCode.Unauthorized, jsonHeaders) }
        )
        val error = assertIs<RestClientErrorSpec.RestClientReceiveError>(either { c.get<String>("a") }.leftOrNull())
        assertEquals(ErrorSpec.HttpStatus.UNAUTHORIZED, error.httpStatus)
        assertEquals("expired", error.cause.detail)
    }

    @Test
    fun aNonApiErrorBodyBecomesADeserializationError() = runTest {
        val c = client(MockEngine { respond("<html>nope</html>", HttpStatusCode.NotFound, htmlHeaders) })
        assertIs<RestClientErrorSpec.RestClientDeserializationError>(either { c.get<String>("a") }.leftOrNull())
    }

    @Test
    fun aTransportFailureBecomesARequestError() = runTest {
        val c = client(MockEngine { throw RuntimeException("connection refused") })
        val error = assertIs<RestClientErrorSpec.RestClientRequestError>(either { c.get<String>("a") }.leftOrNull())
        assertEquals("connection refused", error.cause.message)
    }

    @Test
    fun theMapperItIsGivenReplacesTheDefault() = runTest {
        val mine = RestErrorMapper.of<RestClientErrorSpec, String>(
            apiError = { body, status -> RestClientErrorSpec.RestClientRequestError(RuntimeException("$status $body")) },
            networkError = { RestClientErrorSpec.RestClientRequestError(it) },
            parseError = { RestClientErrorSpec.RestClientDeserializationError(it) },
        )
        val c = RestClient(
            client = mockHttpClient(MockEngine { respond("gone", HttpStatusCode.Gone) }),
            mapper = mine,
        )
        val error = assertIs<RestClientErrorSpec.RestClientRequestError>(either { c.get<String>("a") }.leftOrNull())
        assertEquals("410 Gone gone", error.cause.message)
    }
}
