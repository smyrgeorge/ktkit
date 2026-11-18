package io.github.smyrgeorge.ktkit.ktor.httpclient

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal val testJson = Json { ignoreUnknownKeys = true }

internal val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
internal val htmlHeaders = headersOf(HttpHeaders.ContentType, ContentType.Text.Html.toString())

/**
 * The client every test drives: a [MockEngine] under the same two settings the production
 * [HttpClientFactory] applies — `expectSuccess = false` and kotlinx `ContentNegotiation`.
 */
internal fun mockHttpClient(engine: MockEngine): HttpClient = HttpClient(engine) {
    expectSuccess = false
    install(ContentNegotiation) { json(testJson) }
}

/** The RFC 9457 body a ktkit service answers with. */
internal fun apiErrorBody(status: Int, detail: String = "d") =
    """{"type":null,"title":"nope","status":$status,"detail":"$detail","requestId":"req-7","data":null}"""
