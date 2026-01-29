package io.github.smyrgeorge.ktkit.ktor.httpclient

import arrow.core.raise.context.Raise
import arrow.core.raise.context.bind
import arrow.core.raise.context.either
import io.github.smyrgeorge.ktkit.api.error.ErrorSpec
import io.github.smyrgeorge.ktkit.api.error.impl.GenericError
import io.github.smyrgeorge.ktkit.api.error.impl.UnknownError
import io.github.smyrgeorge.ktkit.api.error.impl.details.EmptyErrorData
import io.github.smyrgeorge.ktkit.api.rest.ApiError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * An abstract base class for implementing HTTP clients with common HTTP operations.
 *
 * This class provides utility methods for making HTTP requests such as GET, POST, PATCH, PUT,
 * and DELETE. It also includes error-handling mechanisms to map HTTP responses into structured
 * error objects and perform optional JSON deserialization of response bodies.
 *
 * @property json A JSON configuration instance, which is used for serializing and deserializing objects.
 * @property client The underlying HTTP client used for executing requests.
 * @property baseUrl The base URL to use for all requests. Individual request URIs are appended to this value.
 * @property toErrorSpec A function used to map HTTP responses into structured error objects. Defaults to a generic error with the raw response body.
 */
abstract class AbstractRestClient(
    val json: Json,
    val client: HttpClient,
    val baseUrl: String,
    val toErrorSpec: suspend HttpResponse.() -> ErrorSpec = { toErrorSpecDefault(json) }
) {
    @PublishedApi
    internal fun buildUri(uri: String) = "${baseUrl}$uri"

    context(_: Raise<ErrorSpec>)
    suspend inline fun <reified T> get(
        uri: String,
        noinline toErrorSpec: (suspend HttpResponse.() -> ErrorSpec)? = null,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val response: HttpResponse = client.get(buildUri(uri)) {
            builder()
        }
        val errorHandling = toErrorSpec ?: this.toErrorSpec
        return if (!response.status.isSuccess()) errorHandling(response).raise()
        else response.bodySafe()
    }

    context(_: Raise<ErrorSpec>)
    suspend inline fun <reified T, reified B> post(
        uri: String,
        body: B,
        noinline toErrorSpec: (suspend HttpResponse.() -> ErrorSpec)? = null,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val response: HttpResponse = client.post(buildUri(uri)) {
            contentType(ContentType.Application.Json)
            setBody(body)
            builder()
        }
        val errorHandling = toErrorSpec ?: this.toErrorSpec
        return if (!response.status.isSuccess()) errorHandling(response).raise()
        else response.bodySafe()
    }

    context(_: Raise<ErrorSpec>)
    suspend inline fun <reified T> postMultipart(
        uri: String,
        data: ByteArray,
        fileName: String = "file",
        contentType: ContentType = ContentType.Application.OctetStream,
        noinline toErrorSpec: (suspend HttpResponse.() -> ErrorSpec)? = null,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val response: HttpResponse = client.post(buildUri(uri)) {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            key = "file",
                            value = data,
                            headers = Headers.build {
                                append(HttpHeaders.ContentType, contentType.toString())
                                append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                            }
                        )
                    }
                )
            )
            builder()
        }
        val errorHandling = toErrorSpec ?: this.toErrorSpec
        return if (!response.status.isSuccess()) errorHandling(response).raise()
        else response.bodySafe()
    }

    context(_: Raise<ErrorSpec>)
    suspend inline fun <reified T, reified B> patch(
        uri: String,
        body: B? = null,
        noinline toErrorSpec: (suspend HttpResponse.() -> ErrorSpec)? = null,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val response: HttpResponse = client.patch(buildUri(uri)) {
            body?.let {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            builder()
        }
        val errorHandling = toErrorSpec ?: this.toErrorSpec
        return if (!response.status.isSuccess()) errorHandling(response).raise()
        else response.bodySafe()
    }

    context(_: Raise<ErrorSpec>)
    suspend inline fun <reified T, reified B> put(
        uri: String,
        body: B? = null,
        noinline toErrorSpec: (suspend HttpResponse.() -> ErrorSpec)? = null,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val response: HttpResponse = client.put(buildUri(uri)) {
            body?.let {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            builder()
        }
        val errorHandling = toErrorSpec ?: this.toErrorSpec
        return if (!response.status.isSuccess()) errorHandling(response).raise()
        else response.bodySafe()
    }

    context(_: Raise<ErrorSpec>)
    suspend inline fun <reified T> delete(
        uri: String,
        noinline toErrorSpec: (suspend HttpResponse.() -> ErrorSpec)? = null,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val response: HttpResponse = client.delete(buildUri(uri)) {
            builder()
        }
        val errorHandling = toErrorSpec ?: this.toErrorSpec
        return if (!response.status.isSuccess()) errorHandling(response).raise()
        else response.bodySafe()
    }

    @PublishedApi
    context(_: Raise<ErrorSpec>)
    internal suspend inline fun <reified T> HttpResponse.bodySafe(): T =
        either<Throwable, T> { body() }
            .mapLeft {
                UnknownError(
                    message = it.message ?: "Could not deserialize response body",
                    httpStatus = ErrorSpec.HttpStatus.INTERNAL_SERVER_ERROR
                )
            }.bind()


    companion object {
        @PublishedApi
        internal suspend fun HttpResponse.toErrorSpecDefault(json: Json): ErrorSpec {
            val bodyText: String = try {
                bodyAsText()
            } catch (e: Throwable) {
                return UnknownError(
                    message = e.message ?: "Could not deserialize response body",
                    httpStatus = ErrorSpec.HttpStatus.INTERNAL_SERVER_ERROR
                )
            }

            return try {
                // Attempt to deserialize the error
                val error = json.decodeFromString<ApiError>(bodyText)
                GenericError(
                    message = error.detail,
                    data = error.data ?: EmptyErrorData,
                    httpStatus = ErrorSpec.HttpStatus.fromCode(status.value)
                )
            } catch (_: SerializationException) {
                // If deserialization fails, create a generic error with the raw body
                val msg = "Could not fulfill the request (HttpClientRequestError). Details: $bodyText"
                UnknownError(message = msg, httpStatus = ErrorSpec.HttpStatus.fromCode(status.value))
            }
        }
    }
}
