package io.github.smyrgeorge.ktkit.ktor.httpclient.impl

import arrow.core.raise.Raise
import io.github.smyrgeorge.ktkit.ktor.httpclient.HttpClientFactory
import io.github.smyrgeorge.ktkit.ktor.httpclient.RestClientErrorSpec
import io.github.smyrgeorge.ktkit.ktor.httpclient.RestErrorMapper
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.FormBuilder
import io.ktor.http.Parameters
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * A [RestClient] that sends `Authorization: Bearer <token>` on every call made with a token.
 *
 * Each verb takes the token first and otherwise matches [RestClient]'s; the plain verbs stay available.
 */
class BearerRestClient(
    json: Json = Json,
    baseUrl: String = "",
    client: HttpClient = HttpClientFactory.create(negotiation = { json(json) }),
    mapper: RestErrorMapper<RestClientErrorSpec, *> = RestErrorMapper.default(),
) : RestClient(json = json, baseUrl = baseUrl, client = client, mapper = mapper) {
    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T> get(
        token: String,
        uri: String,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = get<T>(uri) {
        builder()
        bearerAuth(token)
    }

    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T> delete(
        token: String,
        uri: String,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = delete<T>(uri) {
        builder()
        bearerAuth(token)
    }

    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T> head(
        token: String,
        uri: String,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = head<T>(uri) {
        builder()
        bearerAuth(token)
    }

    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T> options(
        token: String,
        uri: String,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = options<T>(uri) {
        builder()
        bearerAuth(token)
    }

    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T, reified B> post(
        token: String,
        uri: String,
        body: B,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = post<T, B>(uri, body) {
        builder()
        bearerAuth(token)
    }

    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T, reified B> put(
        token: String,
        uri: String,
        body: B? = null,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = put<T, B>(uri, body) {
        builder()
        bearerAuth(token)
    }

    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T, reified B> patch(
        token: String,
        uri: String,
        body: B? = null,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = patch<T, B>(uri, body) {
        builder()
        bearerAuth(token)
    }

    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T> postMultipart(
        token: String,
        uri: String,
        crossinline parts: FormBuilder.() -> Unit,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = postMultipart<T>(uri, parts) {
        builder()
        bearerAuth(token)
    }

    context(_: Raise<RestClientErrorSpec>)
    suspend inline fun <reified T> postForm(
        token: String,
        uri: String,
        parameters: Parameters,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): T = postForm<T>(uri, parameters) {
        builder()
        bearerAuth(token)
    }
}
