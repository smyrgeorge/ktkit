package io.github.smyrgeorge.ktkit.ktor.httpclient.impl

import arrow.core.raise.either
import io.github.smyrgeorge.ktkit.ktor.httpclient.AbstractRestClient
import io.github.smyrgeorge.ktkit.ktor.httpclient.RestClientErrorSpec
import io.github.smyrgeorge.ktkit.ktor.httpclient.apiErrorBody
import io.github.smyrgeorge.ktkit.ktor.httpclient.demoBearerClient
import io.github.smyrgeorge.ktkit.ktor.httpclient.demoMapError
import io.github.smyrgeorge.ktkit.ktor.httpclient.jsonHeaders
import io.github.smyrgeorge.ktkit.ktor.httpclient.mockHttpClient
import io.github.smyrgeorge.ktkit.ktor.httpclient.testJson
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BearerRestClientTest {
    /** All eight operations carry the token, and each still reaches its own verb. */
    @Test
    fun everyOperationSendsTheBearerHeader() = runTest {
        val seen = mutableListOf<Pair<String, String?>>()
        val client = demoBearerClient(
            MockEngine { request ->
                seen += request.method.value to request.headers[HttpHeaders.Authorization]
                respond("", HttpStatusCode.OK)
            }
        )
        either {
            client.get<Unit>("t0k", "a")
            client.post<Unit, Map<String, String>>("t0k", "a", mapOf("k" to "v"))
            client.postMultipart<Unit>("t0k", "a", byteArrayOf(1))
            client.patch<Unit, Map<String, String>>("t0k", "a", mapOf("k" to "v"))
            client.put<Unit, Map<String, String>>("t0k", "a", mapOf("k" to "v"))
            client.delete<Unit>("t0k", "a")
            client.head<Unit>("t0k", "a")
            client.options<Unit>("t0k", "a")
        }
        assertEquals(
            listOf("GET", "POST", "POST", "PATCH", "PUT", "DELETE", "HEAD", "OPTIONS"),
            seen.map { it.first },
        )
        assertTrue(seen.all { it.second == "Bearer t0k" }, "every request must carry the token: $seen")
    }

    /** The token is the only thing added: a body-carrying verb still sends its JSON body. */
    @Test
    fun theTokenIsAddedAlongsideTheJsonBody() = runTest {
        var contentType: ContentType? = null
        var text: String? = null
        var authorization: String? = null
        val client = demoBearerClient(
            MockEngine { request ->
                authorization = request.headers[HttpHeaders.Authorization]
                contentType = request.body.contentType
                text = assertIs<TextContent>(request.body).text
                respond("", HttpStatusCode.OK)
            }
        )
        either { client.put<Unit, Map<String, String>>("t0k", "a", mapOf("k" to "v")) }
        assertEquals("Bearer t0k", authorization)
        assertEquals(ContentType.Application.Json, contentType?.withoutParameters())
        assertEquals("""{"k":"v"}""", text)
    }

    /** `builder` runs first and the header is then *appended*, so it does not replace what the caller set. */
    @Test
    fun theBuilderRunsBeforeTheHeaderIsAppended() = runTest {
        var values: List<String>? = null
        val client = demoBearerClient(
            MockEngine { request ->
                values = request.headers.getAll(HttpHeaders.Authorization)
                respond("", HttpStatusCode.OK)
            }
        )
        either { client.get<Unit>("t0k", "a") { header(HttpHeaders.Authorization, "Basic other") } }
        assertEquals(listOf("Basic other", "Bearer t0k"), values)
    }

    /** The `builder` is still a full `HttpRequestBuilder.() -> Unit`. */
    @Test
    fun theBuilderCanStillShapeTheRequest() = runTest {
        var probe: String? = null
        val client = demoBearerClient(
            MockEngine { request ->
                probe = request.headers["X-Probe"]
                respond("", HttpStatusCode.OK)
            }
        )
        either { client.delete<Unit>("t0k", "a") { header("X-Probe", "1") } }
        assertEquals("1", probe)
    }

    /** It is an [AbstractRestClient], so the un-tokenised operations remain available — and send nothing. */
    @Test
    fun itIsAnAbstractRestClientAndKeepsItsWiring() = runTest {
        var authorization: String? = null
        val client = BearerRestClient(
            json = testJson,
            baseUrl = "https://api.test/",
            client = mockHttpClient(
                MockEngine { request ->
                    authorization = request.headers[HttpHeaders.Authorization]
                    respond("ok", HttpStatusCode.OK)
                }
            ),
            mapError = demoMapError(),
        )
        val base: AbstractRestClient = client
        assertEquals("https://api.test/", base.baseUrl)
        assertEquals("ok", either { client.get<String>(token = "t0k", uri = "a") }.getOrNull())
        assertEquals("Bearer t0k", authorization)

        assertEquals("ok", either { client.get<String>("a") }.getOrNull())
        assertNull(authorization)
    }

    /** Errors run through the `mapError` the client was given. */
    @Test
    fun errorsFlowThroughTheSuppliedMapError() = runTest {
        val client = demoBearerClient(
            MockEngine { respondError(HttpStatusCode.Forbidden, apiErrorBody(403, detail = "denied"), jsonHeaders) }
        )
        val error = assertIs<RestClientErrorSpec.RestClientReceiveError>(
            either { client.get<String>("t0k", "a") }.leftOrNull()
        )
        assertEquals(403, error.httpStatus.code)
        assertEquals("denied", error.cause.detail)
    }

    /** A 401 runs the same mapping here as on the base client. */
    @Test
    fun unauthorizedIsMappedTheSameWayAsOnTheBaseClient() = runTest {
        val client = demoBearerClient(
            MockEngine { respond(apiErrorBody(401, detail = "expired"), HttpStatusCode.Unauthorized, jsonHeaders) }
        )
        val error = assertIs<RestClientErrorSpec.RestClientReceiveError>(
            either { client.get<String>("t0k", "a") }.leftOrNull()
        )
        assertEquals(401, error.httpStatus.code)
        assertEquals("expired", error.cause.detail)
    }
}
