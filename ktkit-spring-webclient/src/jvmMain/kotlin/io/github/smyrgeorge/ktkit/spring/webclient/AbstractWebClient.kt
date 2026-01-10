package io.github.smyrgeorge.ktkit.spring.webclient

import io.github.smyrgeorge.ktkit.api.error.ErrorSpec
import io.github.smyrgeorge.ktkit.api.error.RuntimeError
import io.github.smyrgeorge.ktkit.api.error.impl.GenericError
import io.github.smyrgeorge.ktkit.api.error.impl.UnknownError
import io.github.smyrgeorge.ktkit.api.error.impl.details.EmptyErrorData
import io.github.smyrgeorge.ktkit.api.rest.ApiError
import io.github.smyrgeorge.ktkit.util.mapError
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.awaitBody
import reactor.core.publisher.Mono
import tools.jackson.databind.json.JsonMapper

typealias HeadersF = (h: HttpHeaders) -> Unit
typealias OnErrorF = (r: ClientResponse) -> Mono<RuntimeError>

/**
 * An abstract class that provides a foundation for implementing a web client for making HTTP requests.
 *
 * The `AbstractWebClient` standardizes the use of a `WebClient` instance to perform common HTTP operations,
 * such as GET, POST, PATCH, PUT, and DELETE, while offering support for customizing request headers and error handling.
 * This class is designed to streamline client-server communication by encapsulating reusable logic for building
 * requests and processing responses.
 *
 * @param client The WebClient instance used for executing HTTP requests.
 * @param baseUrl The base URL of the target API to which requests are made. Default value is an empty string.
 */
abstract class AbstractWebClient(
    val client: WebClient,
    val baseUrl: String = "",
) {
    fun buildUri(uri: String) = "${baseUrl}$uri"

    suspend inline fun <reified T> get(
        uri: String,
        crossinline headers: HeadersF = {},
        crossinline onError: OnErrorF = { r -> r.createError() },
    ): Result<T> =
        runCatching<AbstractWebClient, T & Any> {
            client.get()
                .uri(buildUri(uri))
                .headers { h -> headers(h) }
                .retrieve()
                .onStatus({ it.isError }) { r -> onError(r) }
                .awaitBody()
        }.mapError { it.toRuntimeError() }

    suspend inline fun <reified T> post(
        uri: String,
        body: Any,
        crossinline headers: HeadersF = {},
        crossinline onError: OnErrorF = { r -> r.createError() },
    ): Result<T> =
        runCatching<AbstractWebClient, T & Any> {
            client.post()
                .uri(buildUri(uri))
                .body(BodyInserters.fromValue(body))
                .headers { h -> headers(h) }
                .retrieve()
                .onStatus({ it.isError }) { r -> onError(r) }
                .awaitBody()
        }.mapError { it.toRuntimeError() }

    suspend inline fun <reified T> postMultipartFile(
        uri: String,
        multipartData: Any,
        crossinline headers: HeadersF = {},
        crossinline onError: OnErrorF = { r -> r.createError() },
    ): Result<T> =
        runCatching<AbstractWebClient, T & Any> {
            client.post()
                .uri(buildUri(uri))
                .body(BodyInserters.fromMultipartData("file", multipartData))
                .headers { h -> headers(h) }
                .retrieve()
                .onStatus({ it.isError }) { r -> onError(r) }
                .awaitBody()
        }.mapError { it.toRuntimeError() }

    suspend inline fun <reified T> patch(
        uri: String,
        body: Any? = null,
        crossinline headers: HeadersF = {},
        crossinline onError: OnErrorF = { r -> r.createError() },
    ): Result<T> =
        runCatching<AbstractWebClient, T & Any> {
            client.patch()
                .uri(buildUri(uri))
                .body(body?.let { BodyInserters.fromValue(it) } ?: BodyInserters.empty<Any>())
                .headers { h -> headers(h) }
                .retrieve()
                .onStatus({ it.isError }) { r -> onError(r) }
                .awaitBody()
        }.mapError { it.toRuntimeError() }

    suspend inline fun <reified T> put(
        uri: String,
        body: Any? = null,
        crossinline headers: HeadersF = {},
        crossinline onError: OnErrorF = { r -> r.createError() },
    ): Result<T> =
        runCatching<AbstractWebClient, T & Any> {
            client.put()
                .uri(buildUri(uri))
                .body(body?.let { BodyInserters.fromValue(it) } ?: BodyInserters.empty<Any>())
                .headers { h -> headers(h) }
                .retrieve()
                .onStatus({ it.isError }) { r -> onError(r) }
                .awaitBody()
        }.mapError { it.toRuntimeError() }

    suspend inline fun <reified T> delete(
        uri: String,
        crossinline headers: HeadersF = {},
        crossinline onError: OnErrorF = { r -> r.createError() },
    ): Result<T> =
        runCatching<AbstractWebClient, T & Any> {
            client.delete()
                .uri(buildUri(uri))
                .headers { h -> headers(h) }
                .retrieve()
                .onStatus({ it.isError }) { r -> onError(r) }
                .awaitBody()
        }.mapError { it.toRuntimeError() }

    companion object {
        private fun WebClientResponseException.toRuntimeError(jm: JsonMapper): RuntimeError {
            val error: ErrorSpec = try {
                // Deserialize the error.
                val error = jm.readValue(responseBodyAsByteArray, ApiError::class.java)
                GenericError(
                    message = error.detail,
                    data = error.data ?: EmptyErrorData,
                    httpStatus = ErrorSpec.HttpStatus.fromCode(statusCode.value())
                )
            } catch (_: Exception) {
                val msg = "Could not fulfill the request (WebclientRequestError). Details: $responseBodyAsString"
                GenericError(message = msg, httpStatus = ErrorSpec.HttpStatus.fromCode(statusCode.value()))
            }

            return error.toThrowable(this)
        }

        /**
         * Converts a [ClientResponse] into a [RuntimeError] by creating an exception instance
         * and mapping it using the provided JSON mapper.
         *
         * @param mapper the JSON mapper used for deserializing the response body into a structured error.
         * @return a [Mono] emitting a [RuntimeError] derived from the [ClientResponse].
         */
        fun ClientResponse.toRuntimeError(mapper: JsonMapper): Mono<RuntimeError> =
            createException().map { it.toRuntimeError(mapper) }

        /**
         * Converts a [Throwable] instance into a [RuntimeError].
         *
         * If the current throwable is already an instance of [RuntimeError], it is returned as-is.
         * Otherwise, wraps the throwable into a [RuntimeError] of type [UnknownError].
         *
         * @return A [RuntimeError] instance representing the current throwable.
         */
        fun Throwable.toRuntimeError(): RuntimeError = when (this) {
            is RuntimeError -> this
            else -> UnknownError(message ?: "null").toThrowable(this)
        }
    }
}
