package io.github.smyrgeorge.ktkit.ktor.httpclient.impl

import arrow.core.raise.context.Raise
import arrow.core.raise.either
import arrow.core.raise.recover
import io.github.smyrgeorge.ktkit.api.error.ErrorSpec
import io.github.smyrgeorge.ktkit.ktor.httpclient.DemoError
import io.github.smyrgeorge.ktkit.ktor.httpclient.RestClientErrorSpec
import io.github.smyrgeorge.ktkit.ktor.httpclient.apiErrorBody
import io.github.smyrgeorge.ktkit.ktor.httpclient.demoMapError
import io.github.smyrgeorge.ktkit.ktor.httpclient.demoTypedClient
import io.github.smyrgeorge.ktkit.ktor.httpclient.jsonHeaders
import io.github.smyrgeorge.ktkit.ktor.httpclient.mockHttpClient
import io.github.smyrgeorge.ktkit.ktor.httpclient.testJson
import io.github.smyrgeorge.ktkit.ktor.httpclient.toDomainError
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TypedRestClientTest {
    /** A success is untouched by the error machinery. */
    @Test
    fun successPassesThrough() = runTest {
        val client = demoTypedClient(MockEngine { respond("""{"value":"hi"}""", HttpStatusCode.OK, jsonHeaders) })
        assertEquals("""{"value":"hi"}""", either { client.get<String>("a") }.getOrNull())
    }

    /** A non-2xx goes through `mapError` first, then `transform`. */
    @Test
    fun anErrorResponseIsMappedThenTransformed() = runTest {
        val client = demoTypedClient(
            MockEngine { respond(apiErrorBody(404, detail = "gone"), HttpStatusCode.NotFound, jsonHeaders) }
        )
        val error = assertIs<DemoError.Api>(either { client.get<String>("a") }.leftOrNull())
        assertEquals(404, error.statusCode)
        assertEquals("gone", error.detail)
    }

    /**
     * With no unauthorized variant on the transport side, `transform` is where a `401` is
     * recognised — from the status the server sent.
     */
    @Test
    fun unauthorizedBecomesTheDomainUnauthorized() = runTest {
        val client = demoTypedClient(
            MockEngine { respond(apiErrorBody(401), HttpStatusCode.Unauthorized, jsonHeaders) }
        )
        assertSame(DemoError.Unauthorized, either { client.get<String>("a") }.leftOrNull())
    }

    /**
     * The two errors the client raises by itself never reach `mapError`, so only `transform` can
     * carry them into [DemoError]. This is what makes `transform` — not `mapError` — the total one.
     */
    @Test
    fun transportFailureIsTransformedEvenThoughMapErrorNeverRuns() = runTest {
        var mapErrorCalls = 0
        val client = TypedRestClient(
            json = testJson,
            baseUrl = "https://api.test/",
            client = mockHttpClient(MockEngine { throw RuntimeException("connection refused") }),
            mapError = { mapErrorCalls++; demoMapError()(this) },
            transform = ::toDomainError,
        )
        val error = assertIs<DemoError.Network>(either { client.get<String>("a") }.leftOrNull())
        assertEquals("connection refused", error.cause.message)
        assertEquals(0, mapErrorCalls)
    }

    @Test
    fun decodeFailureIsTransformedEvenThoughMapErrorNeverRuns() = runTest {
        var mapErrorCalls = 0
        val client = TypedRestClient(
            json = testJson,
            baseUrl = "https://api.test/",
            client = mockHttpClient(MockEngine { respond("not json", HttpStatusCode.OK, jsonHeaders) }),
            mapError = { mapErrorCalls++; demoMapError()(this) },
            transform = ::toDomainError,
        )
        assertIs<DemoError.Decode>(either { client.get<JsonObject>("a") }.leftOrNull())
        assertEquals(0, mapErrorCalls)
    }

    /**
     * A compile-time proof that no [RestClientErrorSpec] can reach the caller: the `when` over the
     * sealed [DemoError] has no `else`, so it would not compile if a foreign error could arrive.
     */
    @Test
    fun exhaustiveWhenOverSealedDomainError() = runTest {
        val client = demoTypedClient(
            MockEngine { respond(apiErrorBody(401), HttpStatusCode.Unauthorized, jsonHeaders) }
        )
        val action: String = when (val error = either { client.get<String>("a") }.leftOrNull()) {
            null -> "ok"
            DemoError.Unauthorized -> "retry-auth"
            is DemoError.Api -> "dead-letter-${error.statusCode}"
            is DemoError.Network -> "retry"
            is DemoError.Decode -> "dead-letter"
        }
        assertEquals("retry-auth", action)
    }

    /** All eight operations are exposed, reach their own verb, and share the transformed channel. */
    @Test
    fun everyVerbIsAvailableAndSharesTheErrorChannel() = runTest {
        val seen = mutableListOf<String>()
        val probes = mutableListOf<String?>()
        val client = demoTypedClient(
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
        assertTrue(errors.all { it is DemoError.Api && it.statusCode == 403 }, "every verb must transform: $errors")
    }

    /** The wrapper forwards its wiring to the [RestClient] it holds. */
    @Test
    fun theConstructorArgumentsReachTheInnerClient() {
        val http = mockHttpClient(MockEngine { respond("", HttpStatusCode.OK) })
        val transform: (RestClientErrorSpec) -> DemoError = ::toDomainError
        val client = TypedRestClient(
            json = testJson,
            baseUrl = "https://api.test/",
            client = http,
            mapError = demoMapError(),
            transform = transform,
        )
        assertEquals("https://api.test/", client.inner.baseUrl)
        assertSame(testJson, client.inner.json)
        assertSame(http, client.inner.client)
        assertSame(transform, client.transform)
    }

    /** `recover` still works, so the domain channel can carry control flow (PUT, then POST). */
    @Test
    fun recoverInteropForControlFlow() = runTest {
        var calls = 0
        val client = demoTypedClient(
            MockEngine { request ->
                calls++
                if (request.method.value == "PUT") respondError(HttpStatusCode.NotFound, apiErrorBody(404), jsonHeaders)
                else respond("", HttpStatusCode.OK)
            }
        )
        assertEquals(Unit, either { updateOrCreate(client) }.getOrNull())
        assertEquals(2, calls)
    }

    context(_: Raise<DemoError>)
    private suspend fun updateOrCreate(client: TypedRestClient<DemoError>) {
        recover({ client.put<Unit, Unit>("customers/me") }) { _: DemoError ->
            client.post<Unit, Map<String, String>>("customers/me", emptyMap())
        }
    }

    /** `transform` is applied to every error, so a caller can rewrite the whole channel at once. */
    @Test
    fun transformIsTheOnlyThingTheCallerSees() = runTest {
        val client = TypedRestClient<DemoError>(
            json = testJson,
            baseUrl = "https://api.test/",
            client = mockHttpClient(MockEngine { respond("", HttpStatusCode(418, "I'm a teapot")) }),
            mapError = { RestClientErrorSpec.RestClientRequestError(RuntimeException("ignored")) },
            transform = { DemoError.Api(ErrorSpec.HttpStatus.IM_A_TEAPOT.code, "rewritten") },
        )
        val error = assertIs<DemoError.Api>(either { client.get<String>("a") }.leftOrNull())
        assertEquals("rewritten", error.detail)
        assertEquals(418, error.statusCode)
    }
}
