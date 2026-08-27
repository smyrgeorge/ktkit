package io.github.smyrgeorge.ktkit.ktor.httpclient.impl

import arrow.core.raise.either
import io.github.smyrgeorge.ktkit.api.auth.impl.UserToken
import io.github.smyrgeorge.ktkit.api.auth.impl.XRealNamePrincipalExtractor
import io.github.smyrgeorge.ktkit.api.error.ErrorSpec
import io.github.smyrgeorge.ktkit.context.Principal
import io.github.smyrgeorge.ktkit.ktor.httpclient.AbstractRestClient
import io.github.smyrgeorge.ktkit.ktor.httpclient.RestClientErrorSpec
import io.github.smyrgeorge.ktkit.ktor.httpclient.apiErrorBody
import io.github.smyrgeorge.ktkit.ktor.httpclient.htmlHeaders
import io.github.smyrgeorge.ktkit.ktor.httpclient.jsonHeaders
import io.github.smyrgeorge.ktkit.ktor.httpclient.mockHttpClient
import io.github.smyrgeorge.ktkit.ktor.httpclient.testJson
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.pluginOrNull
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

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
    fun anUnauthorizedWithoutAnEnvelopeLosesItsStatus() = runTest {
        val c = client(MockEngine { respond("denied", HttpStatusCode.Unauthorized) })
        val error = assertIs<RestClientErrorSpec.RestClientDeserializationError>(
            either { c.get<String>("a") }.leftOrNull()
        )
        assertEquals(ErrorSpec.HttpStatus.INTERNAL_SERVER_ERROR, error.httpStatus)
    }

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

    @Test
    fun everyVerbGoesThroughTheBuiltInErrorMapping() = runTest {
        val seen = mutableListOf<String>()
        val probes = mutableListOf<String?>()
        val c = client(
            MockEngine { request ->
                seen += request.method.value
                probes += request.headers["X-Probe"]
                respondError(HttpStatusCode.Forbidden, apiErrorBody(403, detail = "denied"), jsonHeaders)
            }
        )
        val errors = listOf(
            either { c.get<String>("a") { header("X-Probe", "1") } },
            either { c.post<String, Map<String, String>>("a", mapOf("k" to "v")) { header("X-Probe", "1") } },
            either { c.postMultipart<String>("a", byteArrayOf(1, 2)) { header("X-Probe", "1") } },
            either { c.patch<String, Map<String, String>>("a", mapOf("k" to "v")) { header("X-Probe", "1") } },
            either { c.put<String, Map<String, String>>("a", mapOf("k" to "v")) { header("X-Probe", "1") } },
            either { c.delete<String>("a") { header("X-Probe", "1") } },
            either { c.head<String>("a") { header("X-Probe", "1") } },
            either { c.options<String>("a") { header("X-Probe", "1") } },
        ).map { it.leftOrNull() }

        assertEquals(listOf("GET", "POST", "POST", "PATCH", "PUT", "DELETE", "HEAD", "OPTIONS"), seen)
        assertEquals(List<String?>(8) { "1" }, probes)
        assertTrue(
            errors.all { it is RestClientErrorSpec.RestClientReceiveError && it.cause.detail == "denied" },
            "every verb must decode the same envelope: $errors",
        )
    }

    @Test
    fun theInheritedOperationsSendNoIdentityHeader() = runTest {
        var header: String? = "unset"
        val c = client(
            MockEngine { request ->
                header = request.headers[XRealNamePrincipalExtractor.HEADER_NAME]
                respond("", HttpStatusCode.OK)
            }
        )
        assertEquals(Unit, either { c.get<Unit>("a") }.getOrNull())
        assertNull(header)
    }

    @Test
    fun noTokenisedOperationReachesTheWireWithoutARunningApplication() = runTest {
        var requests = 0
        val c = client(MockEngine { requests++; respond("", HttpStatusCode.OK) })
        val token: Principal = UserToken(uuid = Uuid.parse("00000000-0000-0000-0000-0000000000ff"), username = "alice")
        val errors = listOf(
            either { c.get<Unit>(token, "a") },
            either { c.post<Unit, Map<String, String>>(token, "a", mapOf("k" to "v")) },
            either { c.postMultipart<Unit>(token, "a", byteArrayOf(1, 2)) },
            either { c.patch<Unit, Map<String, String>>(token, "a", mapOf("k" to "v")) },
            either { c.put<Unit, Map<String, String>>(token, "a", mapOf("k" to "v")) },
            either { c.delete<Unit>(token, "a") },
            either { c.head<Unit>(token, "a") },
            either { c.options<Unit>(token, "a") },
        ).map { it.leftOrNull() }

        assertEquals(0, requests)
        assertTrue(
            errors.all { it is RestClientErrorSpec.RestClientRequestError },
            "a header that cannot be built is a request failure, not a sent request: $errors",
        )
    }
}
