package io.github.smyrgeorge.ktkit.ktor.httpclient

import arrow.core.raise.context.Raise
import arrow.core.raise.context.raise
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
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
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
 * @property toRestClientErrorSpec A function that maps HTTP responses to structured error objects.
 */
abstract class AbstractRestClient(
    val json: Json,
    val client: HttpClient,
    val baseUrl: String,
    val toRestClientErrorSpec: suspend HttpResponse.() -> RestClientErrorSpec
) {
    @PublishedApi
    internal fun buildUri(uri: String) = "${baseUrl}$uri"

    context(r: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T> get(
        uri: String,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val response: HttpResponse = executeSafe { client.get(buildUri(uri)) { builder() } }
        return if (!response.status.isSuccess()) raise(toRestClientErrorSpec(response))
        else response.bodySafe()
    }

    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T, reified B> post(
        uri: String,
        body: B,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val response = executeSafe {
            client.post(buildUri(uri)) {
                contentType(ContentType.Application.Json)
                setBody(body); builder()
            }
        }
        return if (!response.status.isSuccess()) raise(toRestClientErrorSpec(response))
        else response.bodySafe()
    }

    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T> postMultipart(
        uri: String,
        data: ByteArray,
        fileName: String = "file",
        contentType: ContentType = ContentType.Application.OctetStream,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val response = executeSafe {
            client.post(buildUri(uri)) {
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
        }
        return if (!response.status.isSuccess()) raise(toRestClientErrorSpec(response))
        else response.bodySafe()
    }

    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T, reified B> patch(
        uri: String,
        body: B? = null,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val response: HttpResponse = executeSafe {
            client.patch(buildUri(uri)) {
                body?.let {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
                builder()
            }
        }
        return if (!response.status.isSuccess()) raise(toRestClientErrorSpec(response))
        else response.bodySafe()
    }

    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T, reified B> put(
        uri: String,
        body: B? = null,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val response: HttpResponse = executeSafe {
            client.put(buildUri(uri)) {
                body?.let {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
                builder()
            }
        }
        return if (!response.status.isSuccess()) raise(toRestClientErrorSpec(response))
        else response.bodySafe()
    }

    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T> delete(
        uri: String,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val response: HttpResponse = executeSafe { client.delete(buildUri(uri)) { builder() } }
        return if (!response.status.isSuccess()) raise(toRestClientErrorSpec(response))
        else response.bodySafe()
    }

    @PublishedApi
    context(_: Raise<RestClientErrorSpec>)
    internal suspend inline fun executeSafe(block: suspend () -> HttpResponse) =
        try {
            block()
        } catch (e: Throwable) {
            val error = RestClientErrorSpec.TransportError(
                message = "Could not fulfill the request (HttpClientRequestError): ${e.message}",
            )
            raise(error)
        }

    @PublishedApi
    context(_: Raise<RestClientErrorSpec>)
    internal suspend inline fun <reified T> HttpResponse.bodySafe(): T =
        try {
            body()
        } catch (e: Throwable) {
            val error = RestClientErrorSpec.DeserializationError(
                message = "Could not deserialize response body: ${e.message}",
            )
            raise(error)
        }
}
