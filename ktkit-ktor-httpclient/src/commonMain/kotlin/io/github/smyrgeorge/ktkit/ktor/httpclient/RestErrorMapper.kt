package io.github.smyrgeorge.ktkit.ktor.httpclient

import arrow.core.raise.catch
import io.github.smyrgeorge.ktkit.api.error.ErrorSpec
import io.github.smyrgeorge.ktkit.api.rest.ApiError
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.util.reflect.TypeInfo
import io.ktor.util.reflect.typeInfo

/**
 * Maps every failure of a [TypedRestClient] call to the caller's error type [E].
 *
 * - [apiError] maps a non-2xx response: its body decoded as the error payload [P], and its status.
 * - [unauthorized], when set, is raised on a `401` without reading the body. A `401` often comes
 *   from a gateway or an identity provider, not from the API, so its body is rarely a [P].
 * - [networkError] wraps a request that could not be sent; [parseError] wraps a body, success or
 *   error, that could not be decoded.
 *
 * The payload decodes through the client's own content negotiation, like a success body, so the
 * mapper holds no codec. Build one with [of], which captures [P] by reification, or take [default].
 */
class RestErrorMapper<E : ErrorSpec, P : Any>(
    private val payloadType: TypeInfo,
    val unauthorized: E? = null,
    val apiError: (P, HttpStatusCode) -> E,
    val networkError: (Throwable) -> E,
    val parseError: (Throwable) -> E,
) {
    /** The [E] for a non-2xx [response]: [unauthorized] on a `401` when set, else [apiError] over the decoded body. */
    suspend fun errorOf(response: HttpResponse): E {
        if (unauthorized != null && response.status == HttpStatusCode.Unauthorized) return unauthorized
        return catch({ apiError(response.body<P>(payloadType), response.status) }) { e -> parseError(e) }
    }

    companion object {
        inline fun <E : ErrorSpec, reified P : Any> of(
            unauthorized: E? = null,
            noinline apiError: (P, HttpStatusCode) -> E,
            noinline networkError: (Throwable) -> E,
            noinline parseError: (Throwable) -> E,
        ): RestErrorMapper<E, P> = RestErrorMapper(typeInfo<P>(), unauthorized, apiError, networkError, parseError)

        /**
         * The mapper for a ktkit service: a non-2xx body is an [ApiError] and becomes a
         * [RestClientErrorSpec.RestClientReceiveError]; a send failure is a
         * [RestClientErrorSpec.RestClientRequestError]; an undecodable body is a
         * [RestClientErrorSpec.RestClientDeserializationError].
         */
        fun default(): RestErrorMapper<RestClientErrorSpec, ApiError> = DEFAULT

        private val DEFAULT: RestErrorMapper<RestClientErrorSpec, ApiError> = of(
            apiError = { error, _ -> RestClientErrorSpec.RestClientReceiveError(error) },
            networkError = { RestClientErrorSpec.RestClientRequestError(it) },
            parseError = { RestClientErrorSpec.RestClientDeserializationError(it) },
        )
    }
}
