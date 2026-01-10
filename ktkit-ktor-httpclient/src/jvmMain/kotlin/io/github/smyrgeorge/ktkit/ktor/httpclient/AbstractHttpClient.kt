package io.github.smyrgeorge.ktkit.ktor.httpclient

import io.github.smyrgeorge.ktkit.api.error.ErrorSpec
import io.github.smyrgeorge.ktkit.api.error.RuntimeError
import io.github.smyrgeorge.ktkit.api.error.impl.GenericError
import io.github.smyrgeorge.ktkit.api.error.impl.UnknownError
import io.github.smyrgeorge.ktkit.api.error.impl.details.EmptyErrorData
import io.github.smyrgeorge.ktkit.api.rest.ApiError
import io.github.smyrgeorge.ktkit.util.mapError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

typealias HeadersF = (h: HeadersBuilder) -> Unit

/**
 * An abstract class that provides a foundation for implementing an HTTP client for making HTTP requests.
 *
 * The `AbstractHttpClient` standardizes the use of an `HttpClient` instance to perform common HTTP operations,
 * such as GET, POST, PATCH, PUT, and DELETE, while offering support for customizing request headers and error handling.
 * This class is designed to streamline client-server communication by encapsulating reusable logic for building
 * requests and processing responses.
 *
 * @param client The HttpClient instance used for executing HTTP requests.
 * @param baseUrl The base URL of the target API to which requests are made. Default value is an empty string.
 */
abstract class AbstractHttpClient(
    val client: HttpClient,
    val baseUrl: String = "",
) {
    fun buildUri(uri: String) = "${baseUrl}$uri"

    suspend inline fun <reified T> get(
        uri: String,
        crossinline headers: HeadersF = {},
    ): Result<T> =
        runCatching<AbstractHttpClient, T & Any> {
            val response: HttpResponse = client.get(buildUri(uri)) {
                this.headers { headers(this) }
            }

            if (response.status.isSuccess()) {
                response.body()
            } else {
                throw response.createError()
            }
        }.mapError { it.toRuntimeError() }

    suspend inline fun <reified T> post(
        uri: String,
        body: Any,
        crossinline headers: HeadersF = {},
    ): Result<T> =
        runCatching<AbstractHttpClient, T & Any> {
            val response: HttpResponse = client.post(buildUri(uri)) {
                contentType(ContentType.Application.Json)
                setBody(body)
                this.headers { headers(this) }
            }

            if (response.status.isSuccess()) {
                response.body()
            } else {
                throw response.createError()
            }
        }.mapError { it.toRuntimeError() }

    suspend inline fun <reified T> postMultipartFile(
        uri: String,
        multipartData: ByteArray,
        fileName: String = "file",
        contentType: ContentType = ContentType.Application.OctetStream,
        crossinline headers: HeadersF = {},
    ): Result<T> =
        runCatching<AbstractHttpClient, T & Any> {
            val response: HttpResponse = client.post(buildUri(uri)) {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("file", multipartData, Headers.build {
                                append(HttpHeaders.ContentType, contentType.toString())
                                append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                            })
                        }
                    )
                )
                this.headers { headers(this) }
            }

            if (response.status.isSuccess()) {
                response.body()
            } else {
                throw response.createError()
            }
        }.mapError { it.toRuntimeError() }

    suspend inline fun <reified T> patch(
        uri: String,
        body: Any? = null,
        crossinline headers: HeadersF = {},
    ): Result<T> =
        runCatching<AbstractHttpClient, T & Any> {
            val response: HttpResponse = client.patch(buildUri(uri)) {
                if (body != null) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
                this.headers { headers(this) }
            }

            if (response.status.isSuccess()) {
                response.body()
            } else {
                throw response.createError()
            }
        }.mapError { it.toRuntimeError() }

    suspend inline fun <reified T> put(
        uri: String,
        body: Any? = null,
        crossinline headers: HeadersF = {},
    ): Result<T> =
        runCatching<AbstractHttpClient, T & Any> {
            val response: HttpResponse = client.put(buildUri(uri)) {
                if (body != null) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
                this.headers { headers(this) }
            }

            if (response.status.isSuccess()) {
                response.body()
            } else {
                throw response.createError()
            }
        }.mapError { it.toRuntimeError() }

    suspend inline fun <reified T> delete(
        uri: String,
        crossinline headers: HeadersF = {},
    ): Result<T> =
        runCatching<AbstractHttpClient, T & Any> {
            val response: HttpResponse = client.delete(buildUri(uri)) {
                this.headers { headers(this) }
            }

            if (response.status.isSuccess()) {
                response.body()
            } else {
                throw response.createError()
            }
        }.mapError { it.toRuntimeError() }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /**
         * Creates a default [RuntimeError] from an [HttpResponse].
         * This method attempts to read the response body and deserialize it into an [ApiError],
         * falling back to a generic error message if deserialization fails.
         *
         * @return a [RuntimeError] derived from the [HttpResponse].
         */
        suspend fun HttpResponse.createError(): RuntimeError {
            val bodyText = bodyAsText()
            val error: ErrorSpec = try {
                // Attempt to deserialize the error
                val apiError = json.decodeFromString<ApiError>(bodyText)
                GenericError(
                    message = apiError.detail,
                    data = apiError.data ?: EmptyErrorData,
                    httpStatus = ErrorSpec.HttpStatus.fromCode(status.value)
                )
            } catch (_: SerializationException) {
                // If deserialization fails, create a generic error with the raw body
                val msg = "Could not fulfill the request (HttpClientRequestError). Details: $bodyText"
                GenericError(message = msg, httpStatus = ErrorSpec.HttpStatus.fromCode(status.value))
            }

            return error.toThrowable()
        }

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
            is ResponseException -> {
                // Ktor-specific exception that wraps HTTP errors
                val msg = message ?: "HTTP error occurred"
                GenericError(
                    message = msg,
                    httpStatus = ErrorSpec.HttpStatus.fromCode(response.status.value)
                ).toThrowable(this)
            }

            else -> UnknownError(message ?: "null").toThrowable(this)
        }
    }
}
