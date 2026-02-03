@file:Suppress("unused")

package io.github.smyrgeorge.ktkit.ktor.httpclient.impl

import arrow.core.raise.context.Raise
import io.github.smyrgeorge.ktkit.ktor.httpclient.AbstractRestClient
import io.github.smyrgeorge.ktkit.ktor.httpclient.HttpClientFactory
import io.github.smyrgeorge.ktkit.ktor.httpclient.RestClientErrorSpec
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json

class BearerRestClient(
    json: Json,
    mapError: suspend context(Raise<RestClientErrorSpec>) HttpResponse.() -> RestClientErrorSpec,
    baseUrl: String = "",
    client: HttpClient = HttpClientFactory.create(json = json),
) : AbstractRestClient(json, client, baseUrl, mapError) {
    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T> get(
        token: String,
        uri: String,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = get<T>(uri) {
        builder()
        bearer(token)
    }

    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T, reified B> post(
        token: String,
        uri: String,
        body: B,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = post<T, B>(uri, body) {
        builder()
        bearer(token)
    }

    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T> postMultipart(
        token: String,
        uri: String,
        data: ByteArray,
        fileName: String = "file",
        contentType: ContentType = ContentType.Application.OctetStream,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = postMultipart<T>(uri, data, fileName, contentType) {
        builder()
        bearer(token)
    }

    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T, reified B> patch(
        token: String,
        uri: String,
        body: B? = null,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = patch<T, B>(uri, body) {
        builder()
        bearer(token)
    }

    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T, reified B> put(
        token: String,
        uri: String,
        body: B? = null,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = put<T, B>(uri, body) {
        builder()
        bearer(token)
    }

    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T> delete(
        token: String,
        uri: String,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = delete<T>(uri) {
        builder()
        bearer(token)
    }

    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T> head(
        token: String,
        uri: String,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = head<T>(uri) {
        builder()
        bearer(token)
    }

    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T> options(
        token: String,
        uri: String,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = options<T>(uri) {
        builder()
        bearer(token)
    }

    @PublishedApi
    internal fun HttpRequestBuilder.bearer(token: String) {
        headers.append(HttpHeaders.Authorization, "Bearer $token")
    }
}
