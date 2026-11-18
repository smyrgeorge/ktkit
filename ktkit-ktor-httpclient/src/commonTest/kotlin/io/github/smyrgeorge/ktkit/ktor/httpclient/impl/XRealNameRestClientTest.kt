package io.github.smyrgeorge.ktkit.ktor.httpclient.impl

import arrow.core.raise.either
import io.github.smyrgeorge.ktkit.api.auth.impl.UserToken
import io.github.smyrgeorge.ktkit.api.auth.impl.XRealNamePrincipalExtractor
import io.github.smyrgeorge.ktkit.context.Principal
import io.github.smyrgeorge.ktkit.ktor.httpclient.RestClientErrorSpec
import io.github.smyrgeorge.ktkit.ktor.httpclient.mockHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.parameters
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class XRealNameRestClientTest {
    private fun client(engine: MockEngine) = XRealNameRestClient(baseUrl = "https://api.test/", client = mockHttpClient(engine))

    @Test
    fun thePlainVerbsSendNoIdentityHeader() = runTest {
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

    /**
     * Encoding the [Principal] needs a running ktkit application. Without one, every tokenised verb
     * fails as a request error and nothing reaches the wire.
     */
    @Test
    fun noTokenisedVerbReachesTheWireWithoutARunningApplication() = runTest {
        var requests = 0
        val c = client(MockEngine { requests++; respond("", HttpStatusCode.OK) })
        val token: Principal = UserToken(uuid = Uuid.parse("00000000-0000-0000-0000-0000000000ff"), username = "alice")
        val errors = listOf(
            either { c.get<Unit>(token, "a") },
            either { c.post<Unit, Map<String, String>>(token, "a", mapOf("k" to "v")) },
            either { c.put<Unit, Map<String, String>>(token, "a", mapOf("k" to "v")) },
            either { c.patch<Unit, Map<String, String>>(token, "a", mapOf("k" to "v")) },
            either { c.delete<Unit>(token, "a") },
            either { c.head<Unit>(token, "a") },
            either { c.options<Unit>(token, "a") },
            either { c.postMultipart<Unit>(token, "a", parts = { append("k", "v") }) },
            either { c.postForm<Unit>(token, "a", parameters { append("k", "v") }) },
        ).map { it.leftOrNull() }

        assertEquals(0, requests)
        assertTrue(
            errors.all { it is RestClientErrorSpec.RestClientRequestError },
            "a header that cannot be built is a request failure, not a sent request: $errors",
        )
    }
}
