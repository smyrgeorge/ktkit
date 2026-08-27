package io.github.smyrgeorge.ktkit.ktor.httpclient

import arrow.core.raise.catch
import arrow.core.raise.context.Raise
import io.github.smyrgeorge.ktkit.api.error.ErrorSpec
import io.github.smyrgeorge.ktkit.api.rest.ApiError
import io.github.smyrgeorge.ktkit.ktor.httpclient.impl.BearerRestClient
import io.github.smyrgeorge.ktkit.ktor.httpclient.impl.RestClient
import io.github.smyrgeorge.ktkit.ktor.httpclient.impl.TypedRestClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
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
internal fun apiErrorBody(status: Int, detail: String = "d", title: String = "nope") =
    """{"type":null,"title":"$title","status":$status,"detail":"$detail","requestId":"req-7","data":null}"""

/**
 * A representative `mapError`: every non-2xx body decodes as an [ApiError] — whose own `status`
 * member is what [RestClientErrorSpec.RestClientReceiveError] reports — and anything that will not
 * decode is a deserialization failure.
 *
 * It reads the body as text rather than through content negotiation, so it works on a response that
 * declares no `Content-Type`. `XRealNameRestClient` takes the other route, `bodyOrRaise<ApiError>()`,
 * and therefore needs the header; both are legitimate and the tests cover each.
 */
internal fun demoMapError(
    json: Json = testJson,
): suspend context(Raise<RestClientErrorSpec>)
HttpResponse.() -> RestClientErrorSpec = {
    catch({
        RestClientErrorSpec.RestClientReceiveError(json.decodeFromString(ApiError.serializer(), bodyAsText()))
    }) { e -> RestClientErrorSpec.RestClientDeserializationError(e) }
}

internal fun demoClient(engine: MockEngine): RestClient = RestClient(
    json = testJson,
    baseUrl = "https://api.test/",
    client = mockHttpClient(engine),
    mapError = demoMapError(),
)

internal fun demoBearerClient(engine: MockEngine): BearerRestClient = BearerRestClient(
    json = testJson,
    baseUrl = "https://api.test/",
    client = mockHttpClient(engine),
    mapError = demoMapError(),
)

internal fun demoTypedClient(engine: MockEngine): TypedRestClient<DemoError> = TypedRestClient(
    json = testJson,
    baseUrl = "https://api.test/",
    client = mockHttpClient(engine),
    mapError = demoMapError(),
    transform = ::toDomainError,
)

/**
 * A caller's own error type: **sealed**, and with no relationship to [RestClientErrorSpec]. The
 * exhaustive `when` in `TypedRestClientTest` would not compile if a foreign [ErrorSpec] could reach
 * the caller.
 */
internal sealed interface DemoError : ErrorSpec {
    data object Unauthorized : DemoError {
        override val message: String = "unauthorized"
        override val httpStatus: ErrorSpec.HttpStatus = ErrorSpec.HttpStatus.UNAUTHORIZED
    }

    data class Api(val statusCode: Int, val detail: String) : DemoError {
        override val message: String = "api: $detail"
        override val httpStatus: ErrorSpec.HttpStatus = ErrorSpec.HttpStatus.fromCode(statusCode)
    }

    data class Network(val cause: Throwable) : DemoError {
        override val message: String = "network: ${cause.message}"
        override val httpStatus: ErrorSpec.HttpStatus = ErrorSpec.HttpStatus.INTERNAL_SERVER_ERROR
    }

    data class Decode(val cause: Throwable) : DemoError {
        override val message: String = "decode: ${cause.message}"
        override val httpStatus: ErrorSpec.HttpStatus = ErrorSpec.HttpStatus.INTERNAL_SERVER_ERROR
    }
}

/**
 * Total over [RestClientErrorSpec], which is what keeps [DemoError] exhaustive for the caller. With
 * no dedicated unauthorized variant on the transport side, a `401` is recognised here — from the
 * status the server actually sent — rather than by the client.
 */
internal fun toDomainError(error: RestClientErrorSpec): DemoError = when (error) {
    is RestClientErrorSpec.RestClientReceiveError ->
        if (error.httpStatus == ErrorSpec.HttpStatus.UNAUTHORIZED) DemoError.Unauthorized
        else DemoError.Api(error.httpStatus.code, error.cause.detail)
    is RestClientErrorSpec.RestClientRequestError -> DemoError.Network(error.cause)
    is RestClientErrorSpec.RestClientDeserializationError -> DemoError.Decode(error.cause)
    else -> DemoError.Network(RuntimeException("unexpected: $error"))
}
