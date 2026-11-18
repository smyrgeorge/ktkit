package io.github.smyrgeorge.ktkit.ktor.httpclient.impl

import arrow.core.raise.either
import io.github.smyrgeorge.ktkit.ktor.httpclient.mockHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.parameters
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BearerRestClientTest {
    private fun client(engine: MockEngine) = BearerRestClient(baseUrl = "https://api.test/", client = mockHttpClient(engine))

    /** Every tokenised verb carries the token, and each still reaches its own method. */
    @Test
    fun everyTokenisedVerbSendsTheBearerHeader() = runTest {
        val seen = mutableListOf<Pair<String, String?>>()
        val c = client(
            MockEngine { request ->
                seen += request.method.value to request.headers[HttpHeaders.Authorization]
                respond("", HttpStatusCode.OK)
            }
        )
        either {
            c.get<Unit>("t0k", "a")
            c.post<Unit, Map<String, String>>("t0k", "a", mapOf("k" to "v"))
            c.put<Unit, Map<String, String>>("t0k", "a", mapOf("k" to "v"))
            c.patch<Unit, Map<String, String>>("t0k", "a", mapOf("k" to "v"))
            c.delete<Unit>("t0k", "a")
            c.head<Unit>("t0k", "a")
            c.options<Unit>("t0k", "a")
            c.postMultipart<Unit>("t0k", "a", parts = { append("k", "v") })
            c.postForm<Unit>("t0k", "a", parameters { append("k", "v") })
        }
        assertEquals(
            listOf("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS", "POST", "POST"),
            seen.map { it.first },
        )
        assertTrue(seen.all { it.second == "Bearer t0k" }, "every request must carry the token: $seen")
    }

    /** `builder` runs first and the token is then appended, so it does not replace what the caller set. */
    @Test
    fun theBuilderRunsBeforeTheHeaderIsAppended() = runTest {
        var values: List<String>? = null
        val c = client(
            MockEngine { request ->
                values = request.headers.getAll(HttpHeaders.Authorization)
                respond("", HttpStatusCode.OK)
            }
        )
        either { c.get<Unit>("t0k", "a") { header(HttpHeaders.Authorization, "Basic other") } }
        assertEquals(listOf("Basic other", "Bearer t0k"), values)
    }

    @Test
    fun thePlainVerbsSendNoToken() = runTest {
        var authorization: String? = "unset"
        val c = client(
            MockEngine { request ->
                authorization = request.headers[HttpHeaders.Authorization]
                respond("", HttpStatusCode.OK)
            }
        )
        either { c.get<Unit>("a") }
        assertNull(authorization)
    }
}
