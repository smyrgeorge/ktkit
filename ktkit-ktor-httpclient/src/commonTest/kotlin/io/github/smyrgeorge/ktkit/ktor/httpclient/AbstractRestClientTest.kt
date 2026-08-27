package io.github.smyrgeorge.ktkit.ktor.httpclient

import arrow.core.raise.context.Raise
import arrow.core.raise.either
import arrow.core.raise.recover
import io.github.smyrgeorge.ktkit.api.error.ErrorSpec
import io.github.smyrgeorge.ktkit.ktor.httpclient.impl.RestClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AbstractRestClientTest {
    @Test
    fun successfulResponseIsDecoded() = runTest {
        val client = demoClient(MockEngine { respond("""{"value":"hi"}""", HttpStatusCode.OK, jsonHeaders) })
        assertEquals("""{"value":"hi"}""", either { client.get<String>("a") }.getOrNull())
    }

    // --- the three failure channels ------------------------------------------------------------

    /** A `401` is now mapped like any other error status — there is no variant that short-circuits it. */
    @Test
    fun unauthorizedIsMappedLikeAnyOtherErrorStatus() = runTest {
        val client = demoClient(
            MockEngine { respond(apiErrorBody(401, detail = "expired"), HttpStatusCode.Unauthorized, jsonHeaders) }
        )
        val error =
            assertIs<RestClientErrorSpec.RestClientReceiveError>(either { client.get<String>("a") }.leftOrNull())
        assertEquals(ErrorSpec.HttpStatus.UNAUTHORIZED, error.httpStatus)
        assertEquals("expired", error.cause.detail)
    }

    /**
     * The cost of that: a `401` whose body is not the error envelope — a gateway rejecting the call
     * before it reaches the service — is a decode failure, and the 401 is lost to a 500.
     */
    @Test
    fun unauthorizedWithANonEnvelopeBodyLosesItsStatus() = runTest {
        val client = demoClient(MockEngine { respond("no token", HttpStatusCode.Unauthorized) })
        val error = assertIs<RestClientErrorSpec.RestClientDeserializationError>(
            either { client.get<String>("a") }.leftOrNull()
        )
        assertEquals(ErrorSpec.HttpStatus.INTERNAL_SERVER_ERROR, error.httpStatus)
    }

    /**
     * The reported status is the payload's own `status` member, not the one seen on the wire.
     * `RestClientReceiveError` derives it from the decoded [io.github.smyrgeorge.ktkit.api.rest.ApiError],
     * so a body that disagrees with its transport status wins.
     */
    @Test
    fun errorStatusComesFromThePayloadAndNotTheWire() = runTest {
        val client = demoClient(MockEngine { respond(apiErrorBody(500), HttpStatusCode.NotFound, jsonHeaders) })
        val error =
            assertIs<RestClientErrorSpec.RestClientReceiveError>(either { client.get<String>("a") }.leftOrNull())
        assertEquals(ErrorSpec.HttpStatus.INTERNAL_SERVER_ERROR, error.httpStatus)
        assertEquals(500, error.cause.status)
    }

    /**
     * `mapError` derives the reported status with `HttpStatus.fromCode`, so a wire status the enum
     * does not model degrades to a 500 rather than throwing.
     */
    @Test
    fun anUnmodelledWireStatusFallsBackToInternalServerError() = runTest {
        val client = demoClient(
            MockEngine { respond(apiErrorBody(599), HttpStatusCode(599, "Custom"), jsonHeaders) }
        )
        val error =
            assertIs<RestClientErrorSpec.RestClientReceiveError>(either { client.get<String>("a") }.leftOrNull())
        assertEquals(ErrorSpec.HttpStatus.INTERNAL_SERVER_ERROR, error.httpStatus)
        assertEquals(599, error.cause.status)
    }

    /** No `Content-Type` at all: the error envelope is still decoded. */
    @Test
    fun errorBodyDecodesWithoutAContentTypeHeader() = runTest {
        val client =
            demoClient(MockEngine { respond(apiErrorBody(502, detail = "gateway"), HttpStatusCode.BadGateway) })
        val error =
            assertIs<RestClientErrorSpec.RestClientReceiveError>(either { client.get<String>("a") }.leftOrNull())
        assertEquals("gateway", error.cause.detail)
    }

    /** An HTML error page is not an `ApiError`, so this `mapError` reports a decode failure. */
    @Test
    fun anUndecodableErrorBodyBecomesADeserializationError() = runTest {
        val client = demoClient(MockEngine { respond("<html>nope</html>", HttpStatusCode.NotFound, htmlHeaders) })
        assertIs<RestClientErrorSpec.RestClientDeserializationError>(either { client.get<String>("a") }.leftOrNull())
    }

    /** A 2xx body that will not decode into `T` is always a deserialization error — never pluggable. */
    @Test
    fun malformedSuccessBodyBecomesADecodeFailure() = runTest {
        val client = demoClient(MockEngine { respond("not json", HttpStatusCode.OK, jsonHeaders) })
        assertIs<RestClientErrorSpec.RestClientDeserializationError>(either { client.get<JsonObject>("a") }.leftOrNull())
    }

    /** A call that never produced a response is always a request error — never pluggable. */
    @Test
    fun transportFailureBecomesARequestError() = runTest {
        val client = demoClient(MockEngine { throw RuntimeException("connection refused") })
        val error =
            assertIs<RestClientErrorSpec.RestClientRequestError>(either { client.get<String>("a") }.leftOrNull())
        assertEquals("connection refused", error.cause.message)
    }

    /** Cancellation must propagate rather than be reported as a retryable domain error. */
    @Test
    fun cancellationIsNotSwallowed() = runTest {
        val client = demoClient(MockEngine { throw CancellationException("shutting down") })
        assertFailsWith<CancellationException> { either { client.get<String>("a") } }
    }

    // --- what `mapError` is, and is not, asked to do -------------------------------------------

    /** `mapError` is consulted for non-2xx only; a success never reaches it. */
    @Test
    fun mapErrorIsNotConsultedForASuccess() = runTest {
        var calls = 0
        val client =
            countingClient(MockEngine { respond("""{"value":"ok"}""", HttpStatusCode.OK, jsonHeaders) }) { calls++ }
        assertEquals("""{"value":"ok"}""", either { client.get<String>("a") }.getOrNull())
        assertEquals(0, calls)
    }

    /** Nor for the other two failure channels: those are fixed, so `mapError` is bypassed entirely. */
    @Test
    fun mapErrorIsNotConsultedForTransportOrDecodeFailures() = runTest {
        var transportCalls = 0
        val transport = countingClient(MockEngine { throw RuntimeException("down") }) { transportCalls++ }
        assertIs<RestClientErrorSpec.RestClientRequestError>(either { transport.get<String>("a") }.leftOrNull())
        assertEquals(0, transportCalls)

        var decodeCalls = 0
        val decode =
            countingClient(MockEngine { respond("not json", HttpStatusCode.OK, jsonHeaders) }) { decodeCalls++ }
        assertIs<RestClientErrorSpec.RestClientDeserializationError>(either { decode.get<JsonObject>("a") }.leftOrNull())
        assertEquals(0, decodeCalls)
    }

    /** The response is `mapError`'s receiver, so it can read the status, the headers and the body. */
    @Test
    fun mapErrorSeesTheWholeResponseAsItsReceiver() = runTest {
        val client = RestClient(
            json = testJson,
            baseUrl = "https://api.test/",
            client = mockHttpClient(
                MockEngine { respondError(HttpStatusCode.Conflict, "raw body", headersOf("X-Trace", "t-1")) }
            ),
            mapError = {
                RestClientErrorSpec.RestClientRequestError(
                    RuntimeException("${status.value}|${headers["X-Trace"]}|${bodyAsText()}")
                )
            },
        )
        val error =
            assertIs<RestClientErrorSpec.RestClientRequestError>(either { client.get<String>("a") }.leftOrNull())
        assertEquals("409|t-1|raw body", error.cause.message)
    }

    /**
     * `mapError` runs with a `Raise<RestClientErrorSpec>` in context, so it can short-circuit itself
     * instead of returning a value. That is the whole reason the context parameter is in the type.
     */
    @Test
    fun mapErrorCanRaiseInsteadOfReturning() = runTest {
        val client = RestClient(
            json = testJson,
            baseUrl = "https://api.test/",
            client = mockHttpClient(MockEngine { respond("", HttpStatusCode.Conflict) }),
            mapError = { raiseError(RestClientErrorSpec.RestClientRequestError(RuntimeException("raised"))) },
        )
        val error =
            assertIs<RestClientErrorSpec.RestClientRequestError>(either { client.get<String>("a") }.leftOrNull())
        assertEquals("raised", error.cause.message)
    }

    // --- constructor wiring and buildUri -------------------------------------------------------

    /** The three visible constructor arguments stay reachable; `mapError` deliberately does not. */
    @Test
    fun theConstructorArgumentsAreExposed() {
        val http = mockHttpClient(MockEngine { respond("", HttpStatusCode.OK) })
        val client =
            RestClient(json = testJson, baseUrl = "https://api.test/", client = http, mapError = demoMapError())
        assertSame(testJson, client.json)
        assertSame(http, client.client)
        assertEquals("https://api.test/", client.baseUrl)
    }

    /** `buildUri` concatenates verbatim — it inserts no separator, so `baseUrl` carries its own. */
    @Test
    fun buildUriConcatenatesBaseUrlAndUriVerbatim() = runTest {
        var url: String? = null
        val client = RestClient(
            json = testJson,
            baseUrl = "https://api.test/v1/",
            client = mockHttpClient(
                MockEngine { request ->
                    url = request.url.toString()
                    respond("", HttpStatusCode.OK)
                }
            ),
            mapError = demoMapError(),
        )
        either { client.get<Unit>("customers/me") }
        assertEquals("https://api.test/v1/customers/me", url)
    }

    /** With the default empty `baseUrl` the operation's `uri` has to be the whole URL. */
    @Test
    fun anEmptyBaseUrlLetsTheUriCarryTheWholeUrl() = runTest {
        var url: String? = null
        val client = RestClient(
            json = testJson,
            client = mockHttpClient(
                MockEngine { request ->
                    url = request.url.toString()
                    respond("", HttpStatusCode.OK)
                }
            ),
            mapError = demoMapError(),
        )
        either { client.get<Unit>("https://elsewhere.test/thing") }
        assertEquals("https://elsewhere.test/thing", url)
    }

    // --- request bodies ------------------------------------------------------------------------

    /** `post` sends `application/json`; `jsonBody` is what sets the content type. */
    @Test
    fun postSendsAnApplicationJsonBody() = runTest {
        var contentType: ContentType? = null
        var text: String? = null
        val client = demoClient(
            MockEngine { request ->
                contentType = request.body.contentType
                text = assertIs<TextContent>(request.body).text
                respond("", HttpStatusCode.OK)
            }
        )
        either { client.post<Unit, Map<String, String>>("a", mapOf("k" to "v")) }
        assertEquals(ContentType.Application.Json, contentType?.withoutParameters())
        assertEquals("""{"k":"v"}""", text)
    }

    /**
     * `reified B` is load-bearing: the body keeps its complete type, so a `List<Map<..>>`
     * serializes as itself rather than through an erased `List<*>` serializer.
     */
    @Test
    fun theRequestBodyKeepsItsCompleteGenericType() = runTest {
        var text: String? = null
        val client = demoClient(
            MockEngine { request ->
                text = assertIs<TextContent>(request.body).text
                respond("", HttpStatusCode.OK)
            }
        )
        either {
            client.post<Unit, List<Map<String, String>>>("a", listOf(mapOf("k" to "v"), mapOf("k" to "w")))
        }
        assertEquals("""[{"k":"v"},{"k":"w"}]""", text)
    }

    /** `T = Unit` against a genuinely empty body, and `B = Unit` meaning "no request body". */
    @Test
    fun unitBodiesInBothDirections() = runTest {
        var bodyContentType: String? = null
        var headerContentType: String? = null
        val client = demoClient(
            MockEngine { request ->
                bodyContentType = request.body.contentType?.toString()
                headerContentType = request.headers[HttpHeaders.ContentType]
                respond("", HttpStatusCode.OK)
            }
        )
        assertEquals(Unit, either { client.put<Unit, Unit>("a") }.getOrNull())
        assertNull(bodyContentType)
        assertNull(headerContentType)
    }

    /** `patch` and `put` take a nullable body: null means "no body", not "a JSON null". */
    @Test
    fun aNullPatchBodySendsNothingAtAll() = runTest {
        var bodyContentType: ContentType? = null
        var headerContentType: String? = null
        val client = demoClient(
            MockEngine { request ->
                bodyContentType = request.body.contentType
                headerContentType = request.headers[HttpHeaders.ContentType]
                respond("", HttpStatusCode.OK)
            }
        )
        either { client.patch<Unit, Map<String, String>>("a", null) }
        assertNull(bodyContentType)
        // The header is the load-bearing half. `request.body.contentType` is null whenever no body
        // was set at all, so on its own it cannot tell a skipped `jsonBody` from one that ran.
        assertNull(headerContentType)
    }

    /** `postMultipart` builds one `multipart/form-data` part under the form key `file`. */
    @Test
    fun postMultipartSendsASingleFilePart() = runTest {
        var contentType: ContentType? = null
        var body: OutgoingContent? = null
        val client = demoClient(
            MockEngine { request ->
                contentType = request.body.contentType
                body = request.body
                respond("", HttpStatusCode.OK)
            }
        )
        either {
            client.postMultipart<Unit>(
                uri = "a",
                data = "hello".encodeToByteArray(),
                fileName = "report.pdf",
                contentType = ContentType.Application.Pdf,
            )
        }
        assertEquals(ContentType.MultiPart.FormData, contentType?.withoutParameters())
        assertNotNull(contentType?.parameter("boundary"))
        val wire = assertNotNull(body).toByteArray().decodeToString()
        assertContains(wire, """name="file"""")
        assertContains(wire, """filename="report.pdf"""")
        assertContains(wire, "application/pdf")
        assertContains(wire, "hello")
    }

    /** Its defaults: the form key doubles as the file name, and the type is opaque bytes. */
    @Test
    fun postMultipartDefaultsToAnOctetStreamNamedFile() = runTest {
        var body: OutgoingContent? = null
        val client = demoClient(
            MockEngine { request ->
                body = request.body
                respond("", HttpStatusCode.OK)
            }
        )
        either { client.postMultipart<Unit>("a", byteArrayOf(1, 2, 3)) }
        val wire = assertNotNull(body).toByteArray().decodeToString()
        assertContains(wire, """filename="file"""")
        assertContains(wire, ContentType.Application.OctetStream.toString())
    }

    // --- the error channel across every verb, and interop --------------------------------------

    /** All eight operations reach the wire and share one error channel. */
    @Test
    fun everyVerbIsAvailableAndSharesTheErrorChannel() = runTest {
        val seen = mutableListOf<String>()
        val probes = mutableListOf<String?>()
        val client = demoClient(
            MockEngine { request ->
                seen += request.method.value
                probes += request.headers["X-Probe"]
                respondError(HttpStatusCode.Forbidden, apiErrorBody(403, detail = "denied"), jsonHeaders)
            }
        )
        val errors = listOf(
            either { client.get<String>("a") { header("X-Probe", "1") } },
            either { client.post<String, Map<String, String>>("a", mapOf("k" to "v")) { header("X-Probe", "1") } },
            either { client.postMultipart<String>("a", byteArrayOf(1, 2)) { header("X-Probe", "1") } },
            either { client.patch<String, Map<String, String>>("a", mapOf("k" to "v")) { header("X-Probe", "1") } },
            either { client.put<String, Map<String, String>>("a", mapOf("k" to "v")) { header("X-Probe", "1") } },
            either { client.delete<String>("a") { header("X-Probe", "1") } },
            either { client.head<String>("a") { header("X-Probe", "1") } },
            either { client.options<String>("a") { header("X-Probe", "1") } },
        ).map { it.leftOrNull() }

        assertEquals(listOf("GET", "POST", "POST", "PATCH", "PUT", "DELETE", "HEAD", "OPTIONS"), seen)
        assertEquals(List<String?>(8) { "1" }, probes)
        assertTrue(
            errors.all { it is RestClientErrorSpec.RestClientReceiveError && it.httpStatus.code == 403 },
            "every verb must report the same error: $errors",
        )
    }

    /** The `builder` lambda is a real `HttpRequestBuilder.() -> Unit`, so it can shape the URL. */
    @Test
    fun builderCanShapeTheRequest() = runTest {
        var url: String? = null
        val client = demoClient(
            MockEngine { request ->
                url = request.url.toString()
                respond("""{"value":"ok"}""", HttpStatusCode.OK, jsonHeaders)
            }
        )
        val body: String? = either {
            client.get<String>("policies") {
                url { parameters["page"] = "2" }
            }
        }.getOrNull()
        assertEquals("""{"value":"ok"}""", body)
        assertEquals("https://api.test/policies?page=2", url)
    }

    /** `recover` really recovers, so the error channel can carry control flow (PUT, then POST). */
    @Test
    fun recoverInteropForControlFlow() = runTest {
        var calls = 0
        val client = demoClient(
            MockEngine { request ->
                calls++
                if (request.method.value == "PUT") respondError(HttpStatusCode.NotFound, apiErrorBody(404), jsonHeaders)
                else respond("", HttpStatusCode.OK)
            }
        )
        assertEquals(Unit, either { updateOrCreate(client) }.getOrNull())
        assertEquals(2, calls)
    }

    context(_: Raise<RestClientErrorSpec>)
    private suspend fun updateOrCreate(client: RestClient) {
        recover({ client.put<Unit, Unit>("customers/me") }) { _: RestClientErrorSpec ->
            client.post<Unit, Map<String, String>>("customers/me", emptyMap())
        }
    }

    /** A client whose `mapError` records that it ran before delegating to the usual mapping. */
    private fun countingClient(engine: MockEngine, onCall: () -> Unit): RestClient = RestClient(
        json = testJson,
        baseUrl = "https://api.test/",
        client = mockHttpClient(engine),
        mapError = {
            onCall()
            demoMapError()(this)
        },
    )
}

/** Lets a `mapError` body short-circuit through the `Raise` its type carries. */
context(r: Raise<RestClientErrorSpec>)
private fun raiseError(error: RestClientErrorSpec): Nothing = r.raise(error)
