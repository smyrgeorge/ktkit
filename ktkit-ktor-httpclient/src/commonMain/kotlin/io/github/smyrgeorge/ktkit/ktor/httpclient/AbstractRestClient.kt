package io.github.smyrgeorge.ktkit.ktor.httpclient

import arrow.core.raise.catch
import arrow.core.raise.context.Raise
import io.github.smyrgeorge.ktkit.ktor.httpclient.RestClientErrorSpec.Companion.raise
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.options
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
 * Represents an abstract REST client providing helper methods to perform HTTP operations
 * such as GET, POST, PUT, PATCH, DELETE, HEAD, and OPTIONS. This class handles request execution, error
 * transformation, and response deserialization.
 *
 * @property json The instance of `Json` for JSON serialization and deserialization.
 * @property client The `HttpClient` responsible for executing HTTP requests.
 * @property baseUrl The base URL used to construct full request URIs.
 * @property toRestClientErrorSpec A function transforming an `HttpResponse` into a [RestClientErrorSpec] for structured error handling.
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
        val response: HttpResponse = executeOrRaise { client.get(buildUri(uri)) { builder() } }
        return if (!response.status.isSuccess()) toRestClientErrorSpec(response).raise()
        else response.bodyOrRaise()
    }

    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T, reified B> post(
        uri: String,
        body: B,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val response = executeOrRaise {
            client.post(buildUri(uri)) {
                contentType(ContentType.Application.Json)
                setBody(body); builder()
            }
        }
        return if (!response.status.isSuccess()) toRestClientErrorSpec(response).raise()
        else response.bodyOrRaise()
    }

    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T> postMultipart(
        uri: String,
        data: ByteArray,
        fileName: String = "file",
        contentType: ContentType = ContentType.Application.OctetStream,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val response = executeOrRaise {
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
        return if (!response.status.isSuccess()) toRestClientErrorSpec(response).raise()
        else response.bodyOrRaise()
    }

    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T, reified B> patch(
        uri: String,
        body: B? = null,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val response: HttpResponse = executeOrRaise {
            client.patch(buildUri(uri)) {
                body?.let {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
                builder()
            }
        }
        return if (!response.status.isSuccess()) toRestClientErrorSpec(response).raise()
        else response.bodyOrRaise()
    }

    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T, reified B> put(
        uri: String,
        body: B? = null,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val response: HttpResponse = executeOrRaise {
            client.put(buildUri(uri)) {
                body?.let {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
                builder()
            }
        }
        return if (!response.status.isSuccess()) toRestClientErrorSpec(response).raise()
        else response.bodyOrRaise()
    }

    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T> delete(
        uri: String,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val response: HttpResponse = executeOrRaise { client.delete(buildUri(uri)) { builder() } }
        return if (!response.status.isSuccess()) toRestClientErrorSpec(response).raise()
        else response.bodyOrRaise()
    }

    context(r: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T> head(
        uri: String,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val response: HttpResponse = executeOrRaise { client.head(buildUri(uri)) { builder() } }
        return if (!response.status.isSuccess()) toRestClientErrorSpec(response).raise()
        else response.bodyOrRaise()
    }

    context(r: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T> options(
        uri: String,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val response: HttpResponse = executeOrRaise { client.options(buildUri(uri)) { builder() } }
        return if (!response.status.isSuccess()) toRestClientErrorSpec(response).raise()
        else response.bodyOrRaise()
    }

    @PublishedApi
    context(_: Raise<RestClientErrorSpec>)
    internal suspend inline fun executeOrRaise(block: suspend () -> HttpResponse): HttpResponse =
        catch({ block() }) { e -> RestClientErrorSpec.RestClientRequestError(e).raise() }

    @PublishedApi
    context(_: Raise<RestClientErrorSpec>)
    internal suspend inline fun <reified T> HttpResponse.bodyOrRaise(): T =
        catch({ body() }) { e -> RestClientErrorSpec.RestClientDeserializationError(e).raise() }
}
