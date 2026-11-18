package io.github.smyrgeorge.ktkit.ktor.httpclient.impl

import arrow.core.raise.Raise
import io.github.smyrgeorge.ktkit.context.Principal
import io.github.smyrgeorge.ktkit.ktor.httpclient.HttpClientFactory
import io.github.smyrgeorge.ktkit.ktor.httpclient.RestClientErrorSpec
import io.github.smyrgeorge.ktkit.ktor.httpclient.RestErrorMapper
import io.github.smyrgeorge.ktkit.ktor.httpclient.xRealName
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormBuilder
import io.ktor.http.Parameters
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * A [RestClient] that forwards the caller's [Principal] in the `x-real-name` header, for ktkit
 * services behind a gateway.
 *
 * Each verb takes the [Principal] first and otherwise matches [RestClient]'s; the plain verbs stay available.
 */
class XRealNameRestClient(
    json: Json = Json,
    baseUrl: String = "",
    client: HttpClient = HttpClientFactory.create(negotiation = { json(json) }),
    mapper: RestErrorMapper<RestClientErrorSpec, *> = RestErrorMapper.default(),
) : RestClient(json = json, baseUrl = baseUrl, client = client, mapper = mapper) {
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
    suspend inline fun <reified T> delete(
        token: Principal,
        uri: String,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = delete<T>(uri) {
        builder()
        xRealName(token)
    }

    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T> head(
        token: Principal,
        uri: String,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = head<T>(uri) {
        builder()
        xRealName(token)
    }

    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T> options(
        token: Principal,
        uri: String,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = options<T>(uri) {
        builder()
        xRealName(token)
    }

    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T, reified B> post(
        token: Principal,
        uri: String,
        body: B,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = post<T, B>(uri, body) {
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
    suspend inline fun <reified T> postMultipart(
        token: Principal,
        uri: String,
        crossinline parts: FormBuilder.() -> Unit,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = postMultipart<T>(uri, parts) {
        builder()
        xRealName(token)
    }

    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T> postForm(
        token: Principal,
        uri: String,
        parameters: Parameters,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = postForm<T>(uri, parameters) {
        builder()
        xRealName(token)
    }
}
