package io.github.smyrgeorge.ktkit.ktor.httpclient

import arrow.core.raise.either
import io.github.smyrgeorge.ktkit.api.error.ErrorSpec
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.parameters
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Bodies are plain maps: this module's tests have no serialization plugin. */
private typealias Ok = Map<String, String>
private typealias Problem = Map<String, String>

private fun ok(value: String): Ok = mapOf("value" to value)
private fun problem(message: String): Problem = mapOf("message" to message)

private sealed interface TestError : ErrorSpec {
    data class Api(val problem: Problem, val status: HttpStatusCode) : TestError {
        override val message: String = problem.getValue("message")
        override val httpStatus: ErrorSpec.HttpStatus = ErrorSpec.HttpStatus.fromCode(status.value)
    }

    data class Network(val cause: Throwable) : TestError {
        override val message: String = "network"
        override val httpStatus: ErrorSpec.HttpStatus = ErrorSpec.HttpStatus.INTERNAL_SERVER_ERROR
    }

    data class Parse(val cause: Throwable) : TestError {
        override val message: String = "parse"
        override val httpStatus: ErrorSpec.HttpStatus = ErrorSpec.HttpStatus.INTERNAL_SERVER_ERROR
    }

    data object Unauthorized : TestError {
        override val message: String = "unauthorized"
        override val httpStatus: ErrorSpec.HttpStatus = ErrorSpec.HttpStatus.UNAUTHORIZED
    }
}

private fun mapper(unauthorized: TestError? = TestError.Unauthorized) =
    RestErrorMapper.of<TestError, Problem>(
        unauthorized = unauthorized,
        apiError = { problem, status -> TestError.Api(problem, status) },
        networkError = { TestError.Network(it) },
        parseError = { TestError.Parse(it) },
    )

private fun client(engine: MockEngine, unauthorized: TestError? = TestError.Unauthorized) =
    TypedRestClient(
        client = mockHttpClient(engine),
        baseUrl = "http://api.local",
        mapper = mapper(unauthorized),
    )

private fun MockRequestHandleScope.jsonResponse(
    body: String,
    status: HttpStatusCode = HttpStatusCode.OK,
): HttpResponseData = respond(body, status, jsonHeaders)

class TypedRestClientTest {
    @Test
    fun a2xxBodyDecodesThroughTheClientsContentNegotiation() = runTest {
        val engine = MockEngine { jsonResponse("""{"value":"ok"}""") }

        val result = either { client(engine).get<Ok>("/things/1") }

        assertEquals(ok("ok"), result.getOrNull())
        assertEquals("http://api.local/things/1", engine.requestHistory.single().url.toString())
    }

    @Test
    fun aNon2xxBodyDecodesToTheDomainErrorThroughTheSameNegotiation() = runTest {
        val engine = MockEngine { jsonResponse("""{"message":"nope"}""", HttpStatusCode.UnprocessableEntity) }

        val result = either { client(engine).get<Ok>("/things/1") }

        assertEquals(TestError.Api(problem("nope"), HttpStatusCode.UnprocessableEntity), result.leftOrNull())
    }

    @Test
    fun a401MapsToUnauthorizedWithoutDecodingTheBody() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.Unauthorized) }

        val result = either { client(engine).get<Ok>("/things/1") }

        assertEquals(TestError.Unauthorized, result.leftOrNull())
    }

    @Test
    fun withoutUnauthorizedA401DecodesLikeAnyOtherError() = runTest {
        val engine = MockEngine { jsonResponse("""{"message":"expired"}""", HttpStatusCode.Unauthorized) }

        val result = either { client(engine, unauthorized = null).get<Ok>("/things/1") }

        assertEquals(TestError.Api(problem("expired"), HttpStatusCode.Unauthorized), result.leftOrNull())
    }

    @Test
    fun anErrorBodyTheNegotiationCannotDecodeMapsToParseError() = runTest {
        val engine = MockEngine { respond("<html>bad gateway</html>", HttpStatusCode.BadGateway, htmlHeaders) }

        val result = either { client(engine).get<Ok>("/things/1") }

        assertIs<TestError.Parse>(result.leftOrNull())
    }

    @Test
    fun aSuccessBodyThatCannotDecodeMapsToParseError() = runTest {
        val engine = MockEngine { jsonResponse("""[1]""") }

        val result = either { client(engine).get<Ok>("/things/1") }

        assertIs<TestError.Parse>(result.leftOrNull())
    }

    @Test
    fun aFailedSendMapsToNetworkError() = runTest {
        val engine = MockEngine { error("connection refused") }

        val result = either { client(engine).get<Ok>("/things/1") }

        assertIs<TestError.Network>(result.leftOrNull())
    }

    @Test
    fun bodyVerbsSendTheBodyAsJson() = runTest {
        val engine = MockEngine { jsonResponse("""{"value":"done"}""") }

        val result = either {
            val api = client(engine)
            api.post<Ok, Ok>("/things", ok("new"))
            api.put<Ok, Ok>("/things/1", ok("new"))
            api.patch<Ok, Ok>("/things/1", ok("new"))
        }

        assertEquals(ok("done"), result.getOrNull())
        assertEquals(listOf(HttpMethod.Post, HttpMethod.Put, HttpMethod.Patch), engine.requestHistory.map { it.method })
        engine.requestHistory.forEach { request ->
            assertEquals(ContentType.Application.Json, request.body.contentType?.withoutParameters())
            assertEquals("""{"value":"new"}""", request.body.toByteArray().decodeToString())
        }
    }

    @Test
    fun putAndPatchWithoutABodySendNoContentType() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.NoContent) }

        val result = either {
            val api = client(engine)
            api.put<Unit, Unit>("/things/1")
            api.patch<Unit, Unit>("/things/1")
        }

        assertEquals(Unit, result.getOrNull())
        assertEquals(listOf(HttpMethod.Put, HttpMethod.Patch), engine.requestHistory.map { it.method })
        engine.requestHistory.forEach { assertEquals(null, it.body.contentType) }
    }

    @Test
    fun postMultipartSendsAMultipartFormDataBodyWithTheGivenParts() = runTest {
        val engine = MockEngine { jsonResponse("""{"value":"uploaded"}""") }

        val result = either {
            client(engine).postMultipart<Ok>("/uploads", parts = {
                append("name", "report")
                append(
                    "file",
                    "bytes".encodeToByteArray(),
                    headersOf(HttpHeaders.ContentDisposition, "filename=\"report.txt\""),
                )
            })
        }

        assertEquals(ok("uploaded"), result.getOrNull())
        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals(ContentType.MultiPart.FormData, request.body.contentType?.withoutParameters())
        val body = request.body.toByteArray().decodeToString()
        assertTrue("form-data; name=\"name\"" in body)
        assertTrue("report" in body)
        assertTrue("form-data; name=\"file\"; filename=\"report.txt\"" in body)
        assertTrue("bytes" in body)
    }

    @Test
    fun postFormSendsAFormUrlEncodedBody() = runTest {
        val engine = MockEngine { jsonResponse("""{"value":"token"}""") }

        val result = either {
            client(engine).postForm<Ok>(
                "/token",
                parameters {
                    append("grant_type", "client_credentials")
                    append("client_id", "sapp")
                },
            )
        }

        assertEquals(ok("token"), result.getOrNull())
        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals(ContentType.Application.FormUrlEncoded, request.body.contentType?.withoutParameters())
        assertEquals("grant_type=client_credentials&client_id=sapp", request.body.toByteArray().decodeToString())
    }

    @Test
    fun bodilessVerbsSendTheMatchingMethodAndAcceptAnEmptyBody() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.NoContent) }

        val result = either {
            val api = client(engine)
            api.get<Unit>("/things/1")
            api.delete<Unit>("/things/1")
            api.head<Unit>("/things/1")
            api.options<Unit>("/things")
        }

        assertEquals(Unit, result.getOrNull())
        assertEquals(
            listOf(HttpMethod.Get, HttpMethod.Delete, HttpMethod.Head, HttpMethod.Options),
            engine.requestHistory.map { it.method },
        )
    }

    @Test
    fun requestSendsAnArbitraryMethodAndAppliesTheBuilder() = runTest {
        val engine = MockEngine { jsonResponse("""{"value":"patched"}""") }

        val result = either {
            client(engine).request<Ok>(HttpMethod.Patch, "/things/1") { header("X-Test", "1") }
        }

        assertEquals(ok("patched"), result.getOrNull())
        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Patch, request.method)
        assertEquals("1", request.headers["X-Test"])
    }
}
