package io.github.smyrgeorge.ktkit.ktor.httpclient.impl

import arrow.core.raise.either
import io.github.smyrgeorge.ktkit.api.error.ErrorSpec
import io.github.smyrgeorge.ktkit.ktor.httpclient.AbstractRestClient
import io.github.smyrgeorge.ktkit.ktor.httpclient.RestClientErrorSpec
import io.github.smyrgeorge.ktkit.ktor.httpclient.apiErrorBody
import io.github.smyrgeorge.ktkit.ktor.httpclient.htmlHeaders
import io.github.smyrgeorge.ktkit.ktor.httpclient.jsonHeaders
import io.github.smyrgeorge.ktkit.ktor.httpclient.mockHttpClient
import io.github.smyrgeorge.ktkit.ktor.httpclient.testJson
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.pluginOrNull
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/**
 * The client takes no `mapError` and does not expose one, so its built-in problem-details mapping is
 * exercised the only way a caller can: through the operations themselves.
 *
 * The `X-Real-Name` header is not covered here. `Principal.toXRealName` resolves the running
 * [io.github.smyrgeorge.ktkit.Application] out of the Koin context to reach its `Json`, so emitting
 * the header needs a started application rather than a unit test. Every test below therefore uses
 * the un-tokenised operations inherited from [AbstractRestClient], which run the same pipeline.
 */
class XRealNameRestClientTest {
    private fun client(engine: MockEngine) = XRealNameRestClient(
        json = testJson,
        baseUrl = "https://api.test/",
        client = mockHttpClient(engine),
    )

    @Test
    fun itIsAnAbstractRestClientAndKeepsItsWiring() {
        val base: AbstractRestClient = client(MockEngine { respond("ok", HttpStatusCode.OK) })
        assertEquals("https://api.test/", base.baseUrl)
        assertEquals(testJson, base.json)
    }

    /** Omitting `client` falls back to the factory, exactly as the other clients do. */
    @Test
    fun theClientDefaultsToAFactoryBuiltOne() {
        val client = XRealNameRestClient(json = testJson, baseUrl = "https://api.test/")
        assertNotNull(client.client.pluginOrNull(ContentNegotiation))
        client.client.close()
    }

    /** The built-in mapping reports the payload's own status, even when the wire disagrees. */
    @Test
    fun problemDetailsTakesThePayloadStatus() = runTest {
        val c = client(MockEngine { respond(apiErrorBody(500), HttpStatusCode.NotFound, jsonHeaders) })
        val error = assertIs<RestClientErrorSpec.RestClientReceiveError>(either { c.get<String>("a") }.leftOrNull())
        assertEquals(ErrorSpec.HttpStatus.INTERNAL_SERVER_ERROR, error.httpStatus)
        assertEquals(500, error.cause.status)
    }

    /**
     * This client's `mapError` reads the envelope with `bodyOrRaise<ApiError>()`, which goes through
     * content negotiation — so an error response that declares no `Content-Type` cannot be decoded
     * and arrives as a deserialization failure, however well-formed its body is.
     */
    @Test
    fun theErrorEnvelopeNeedsItsContentTypeHeader() = runTest {
        val c = client(MockEngine { respond(apiErrorBody(409), HttpStatusCode.Conflict) })
        assertIs<RestClientErrorSpec.RestClientDeserializationError>(either { c.get<String>("a") }.leftOrNull())
    }

    /** The whole RFC 9457 envelope survives into the error. */
    @Test
    fun problemDetailsKeepsTheDecodedPayload() = runTest {
        val c = client(
            MockEngine { respond(apiErrorBody(409, detail = "already exists"), HttpStatusCode.Conflict, jsonHeaders) }
        )
        val error = assertIs<RestClientErrorSpec.RestClientReceiveError>(either { c.get<String>("a") }.leftOrNull())
        assertEquals("nope", error.cause.title)
        assertEquals("already exists", error.cause.detail)
        assertEquals("req-7", error.cause.requestId)
        assertContains(error.message, "already exists")
    }

    /** A `401` from the service itself carries an `ApiError`, so it decodes like any other status. */
    @Test
    fun anUnauthorizedFromTheServiceDecodesLikeAnyOtherStatus() = runTest {
        val c = client(
            MockEngine { respond(apiErrorBody(401, detail = "expired"), HttpStatusCode.Unauthorized, jsonHeaders) }
        )
        val error = assertIs<RestClientErrorSpec.RestClientReceiveError>(either { c.get<String>("a") }.leftOrNull())
        assertEquals(ErrorSpec.HttpStatus.UNAUTHORIZED, error.httpStatus)
        assertEquals("expired", error.cause.detail)
    }

    /**
     * A `401` that never reached the service — a gateway rejecting the call — has no `ApiError` body,
     * so it arrives as a decode failure and the 401 is lost. That is the trade for having no
     * dedicated unauthorized variant.
     */
    @Test
    fun anUnauthorizedWithoutAnEnvelopeLosesItsStatus() = runTest {
        val c = client(MockEngine { respond("denied", HttpStatusCode.Unauthorized) })
        val error = assertIs<RestClientErrorSpec.RestClientDeserializationError>(
            either { c.get<String>("a") }.leftOrNull()
        )
        assertEquals(ErrorSpec.HttpStatus.INTERNAL_SERVER_ERROR, error.httpStatus)
    }

    /**
     * A body that is not an `ApiError` — an HTML error page from a proxy, say — is reported as a
     * deserialization failure and loses the wire status to a 500. That is the cost of a mapping
     * fixed to one envelope, and the reason a non-ktkit service should use [RestClient] instead.
     */
    @Test
    fun problemDetailsReportsANonApiErrorBodyAsADeserializationFailure() = runTest {
        val c = client(MockEngine { respond("<html>nope</html>", HttpStatusCode.NotFound, htmlHeaders) })
        val error = assertIs<RestClientErrorSpec.RestClientDeserializationError>(
            either { c.get<String>("a") }.leftOrNull()
        )
        assertEquals(ErrorSpec.HttpStatus.INTERNAL_SERVER_ERROR, error.httpStatus)
    }

    @Test
    fun problemDetailsReportsATransportFailureAsARequestError() = runTest {
        val c = client(MockEngine { throw RuntimeException("connection refused") })
        val error = assertIs<RestClientErrorSpec.RestClientRequestError>(either { c.get<String>("a") }.leftOrNull())
        assertEquals("connection refused", error.cause.message)
    }

    @Test
    fun problemDetailsReportsAnUndecodableSuccessBodyAsADeserializationError() = runTest {
        val c = client(MockEngine { respond("not json", HttpStatusCode.OK, jsonHeaders) })
        assertIs<RestClientErrorSpec.RestClientDeserializationError>(either { c.get<JsonObject>("a") }.leftOrNull())
    }

    /** A successful call is untouched by the error mapping. */
    @Test
    fun problemDetailsLeavesASuccessAlone() = runTest {
        val c = client(MockEngine { respond("""{"value":"hi"}""", HttpStatusCode.OK, jsonHeaders) })
        assertEquals("""{"value":"hi"}""", either { c.get<String>("a") }.getOrNull())
    }
}
