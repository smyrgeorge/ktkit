package io.github.smyrgeorge.ktkit.ktor.httpclient

import arrow.core.raise.Raise
import arrow.core.raise.catch
import io.github.smyrgeorge.ktkit.api.error.ErrorSpec
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormBuilder
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.isSuccess

/**
 * A REST client whose every call works in [Raise] of the caller's error type [E].
 *
 * A request that cannot be sent raises [RestErrorMapper.networkError], a non-2xx status raises
 * [RestErrorMapper.errorOf], and a success body that does not decode raises
 * [RestErrorMapper.parseError]. Success and error bodies both decode through [client]'s content
 * negotiation, so the codec lives in the [HttpClient] (see [HttpClientFactory.create]).
 *
 * [request] is the general entry point: any method, any body Ktor can send. The verbs are sugar over
 * it: [get], [post], [put], [patch], [delete], [head] and [options] speak JSON; [postMultipart] and
 * [postForm] send the two form encodings. Each takes a `builder` for headers, query parameters and
 * anything else on the request.
 *
 * @property client the [HttpClient] that sends the requests; it must not throw on a non-2xx status.
 * @property baseUrl prefixed to every `uri`; leave it empty to pass absolute URLs.
 * @property mapper maps each failure to [E].
 */
open class TypedRestClient<E : ErrorSpec>(
    @PublishedApi internal val client: HttpClient = HttpClientFactory.create(),
    @PublishedApi internal val baseUrl: String = "",
    @PublishedApi internal val mapper: RestErrorMapper<E, *>,
) {
    /**
     * Sends [method] to [uri] with whatever [builder] sets, and decodes the body as [T], raising the
     * mapped [E] at each failure point.
     */
    context(r: Raise<E>)
    suspend inline fun <reified T> request(
        method: HttpMethod,
        uri: String,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val response = catch({
            client.request(baseUrl + uri) {
                this.method = method
                builder()
            }
        }) { e -> r.raise(mapper.networkError(e)) }
        if (!response.status.isSuccess()) r.raise(mapper.errorOf(response))
        return catch({ response.body<T>() }) { e -> r.raise(mapper.parseError(e)) }
    }

    context(_: Raise<E>)
    suspend inline fun <reified T> get(
        uri: String,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = request(HttpMethod.Get, uri, builder)

    context(_: Raise<E>)
    suspend inline fun <reified T, reified B> post(
        uri: String,
        body: B,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = request(HttpMethod.Post, uri) {
        jsonBody(body)
        builder()
    }

    context(_: Raise<E>)
    suspend inline fun <reified T, reified B> put(
        uri: String,
        body: B? = null,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = request(HttpMethod.Put, uri) {
        jsonBody(body)
        builder()
    }

    context(_: Raise<E>)
    suspend inline fun <reified T, reified B> patch(
        uri: String,
        body: B? = null,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = request(HttpMethod.Patch, uri) {
        jsonBody(body)
        builder()
    }

    context(_: Raise<E>)
    suspend inline fun <reified T> delete(
        uri: String,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = request(HttpMethod.Delete, uri, builder)

    context(_: Raise<E>)
    suspend inline fun <reified T> head(
        uri: String,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = request(HttpMethod.Head, uri, builder)

    context(_: Raise<E>)
    suspend inline fun <reified T> options(
        uri: String,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = request(HttpMethod.Options, uri, builder)

    /** POSTs a `multipart/form-data` body assembled from [parts] (see [FormBuilder.append]). */
    context(_: Raise<E>)
    suspend inline fun <reified T> postMultipart(
        uri: String,
        crossinline parts: FormBuilder.() -> Unit,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = request(HttpMethod.Post, uri) {
        setBody(MultiPartFormDataContent(formData { parts() }))
        builder()
    }

    /** POSTs [parameters] as an `application/x-www-form-urlencoded` body. */
    context(_: Raise<E>)
    suspend inline fun <reified T> postForm(
        uri: String,
        parameters: Parameters,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = request(HttpMethod.Post, uri) {
        setBody(FormDataContent(parameters))
        builder()
    }

    /** Sends [body] as JSON; a `null` body sends no body and no `Content-Type`. */
    @PublishedApi
    internal inline fun <reified B> HttpRequestBuilder.jsonBody(body: B?) {
        if (body == null) return
        contentType(ContentType.Application.Json)
        setBody(body)
    }
}
