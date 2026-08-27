package io.github.smyrgeorge.ktkit.ktor.httpclient.impl

import arrow.core.raise.context.Raise
import arrow.core.raise.recover
import io.github.smyrgeorge.ktkit.api.error.ErrorSpec
import io.github.smyrgeorge.ktkit.ktor.httpclient.HttpClientFactory
import io.github.smyrgeorge.ktkit.ktor.httpclient.RestClientErrorSpec
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import kotlinx.serialization.json.Json

class TypedRestClient<E : ErrorSpec>(
    json: Json,
    baseUrl: String = "",
    client: HttpClient = HttpClientFactory.create(json = json),
    mapError: suspend context(Raise<RestClientErrorSpec>) HttpResponse.() -> RestClientErrorSpec,
    val transform: (RestClientErrorSpec) -> E,
) {
    @PublishedApi
    internal val inner: RestClient = RestClient(
        json = json,
        baseUrl = baseUrl,
        client = client,
        mapError = mapError,
    )

    context(r: Raise<E>)
    suspend inline fun <reified T> get(
        uri: String,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = recover({ inner.get<T>(uri, builder) }) { r.raise(transform(it)) }

    context(r: Raise<E>)
    suspend inline fun <reified T, reified B> post(
        uri: String,
        body: B,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = recover({ inner.post<T, B>(uri, body, builder) }) { r.raise(transform(it)) }

    context(r: Raise<E>)
    suspend inline fun <reified T> postMultipart(
        uri: String,
        data: ByteArray,
        fileName: String = "file",
        contentType: ContentType = ContentType.Application.OctetStream,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = recover({ inner.postMultipart<T>(uri, data, fileName, contentType, builder) }) { r.raise(transform(it)) }

    context(r: Raise<E>)
    suspend inline fun <reified T, reified B> put(
        uri: String,
        body: B? = null,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = recover({ inner.put<T, B>(uri, body, builder) }) { r.raise(transform(it)) }

    context(r: Raise<E>)
    suspend inline fun <reified T, reified B> patch(
        uri: String,
        body: B? = null,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = recover({ inner.patch<T, B>(uri, body, builder) }) { r.raise(transform(it)) }

    context(r: Raise<E>)
    suspend inline fun <reified T> delete(
        uri: String,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = recover({ inner.delete<T>(uri, builder) }) { r.raise(transform(it)) }

    context(r: Raise<E>)
    suspend inline fun <reified T> head(
        uri: String,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = recover({ inner.head<T>(uri, builder) }) { r.raise(transform(it)) }

    context(r: Raise<E>)
    suspend inline fun <reified T> options(
        uri: String,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = recover({ inner.options<T>(uri, builder) }) { r.raise(transform(it)) }
}
