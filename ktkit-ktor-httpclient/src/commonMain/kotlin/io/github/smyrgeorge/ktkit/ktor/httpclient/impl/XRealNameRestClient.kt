@file:Suppress("unused")

package io.github.smyrgeorge.ktkit.ktor.httpclient.impl

import arrow.core.raise.context.Raise
import io.github.smyrgeorge.ktkit.api.auth.impl.XRealNamePrincipalExtractor
import io.github.smyrgeorge.ktkit.api.auth.impl.XRealNamePrincipalExtractor.toXRealName
import io.github.smyrgeorge.ktkit.context.Principal
import io.github.smyrgeorge.ktkit.ktor.httpclient.AbstractRestClient
import io.github.smyrgeorge.ktkit.ktor.httpclient.HttpClientFactory
import io.github.smyrgeorge.ktkit.ktor.httpclient.RestClientErrorSpec
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import kotlinx.serialization.json.Json

class XRealNameRestClient(
    json: Json,
    client: HttpClient = HttpClientFactory.create(json = json),
    baseUrl: String = "",
    toRestClientErrorSpec: suspend HttpResponse.() -> RestClientErrorSpec
) : AbstractRestClient(json, client, baseUrl, toRestClientErrorSpec) {
    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T> get(
        token: Principal,
        uri: String,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = get<T>(uri) {
        builder()
        xRealName(token)
    }

    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T, reified B> post(
        token: Principal,
        uri: String,
        body: B,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = post<T, B>(uri, body = body) {
        builder()
        xRealName(token)
    }

    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T> postMultipart(
        token: Principal,
        uri: String,
        data: ByteArray,
        fileName: String = "file",
        contentType: ContentType = ContentType.Application.OctetStream,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = postMultipart<T>(uri, data, fileName, contentType) {
        builder()
        xRealName(token)
    }

    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T, reified B> patch(
        token: Principal,
        uri: String,
        body: B? = null,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = patch<T, B>(uri, body) {
        builder()
        xRealName(token)
    }

    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T, reified B> put(
        token: Principal,
        uri: String,
        body: B? = null,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = put<T, B>(uri, body) {
        builder()
        xRealName(token)
    }

    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T> delete(
        token: Principal,
        uri: String,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = delete<T>(uri) {
        builder()
        xRealName(token)
    }

    @PublishedApi
    context(_: Raise<RestClientErrorSpec>)
    internal fun HttpRequestBuilder.xRealName(token: Principal) {
        headers[XRealNamePrincipalExtractor.HEADER_NAME] = token.toXRealName()
    }
}
