@file:Suppress("unused")

package io.github.smyrgeorge.ktkit.jvm.spring.webclient.impl

import io.github.smyrgeorge.ktkit.jvm.spring.webclient.AbstractWebClient
import io.github.smyrgeorge.ktkit.jvm.spring.webclient.HeadersF
import io.github.smyrgeorge.ktkit.jvm.spring.webclient.OnErrorF
import org.springframework.web.reactive.function.client.WebClient
import tools.jackson.databind.json.JsonMapper

/**
 * A specialized web client that facilitates interaction with APIs using bearer token authentication.
 * This client provides various HTTP methods such as GET, POST, PATCH, PUT, and DELETE, and supports
 * customization of request headers and error handling.
 *
 * @constructor Initializes an instance of [BearerWebClient] with a specified base [WebClient], base URL,
 *              and JSON mapper.
 * @param client The base [WebClient] used for making HTTP requests.
 * @param baseUrl The base URL for all requests made by this client.
 * @param mapper The [JsonMapper] instance for handling serialization and deserialization of JSON payloads.
 */
class BearerWebClient(
    client: WebClient,
    baseUrl: String,
    val mapper: JsonMapper,
) : AbstractWebClient(client, baseUrl) {
    suspend inline fun <reified T> get(
        token: String,
        uri: String,
        crossinline headers: HeadersF = {},
        crossinline onError: OnErrorF = { r -> r.toRuntimeError(mapper) }
    ): Result<T> =
        get<T>(
            uri = uri,
            headers = { h ->
                headers(h)
                h.setBearerAuth(token)
            },
            onError = onError
        )

    suspend inline fun <reified T> post(
        token: String,
        uri: String,
        body: Any,
        crossinline headers: HeadersF = {},
        crossinline onError: OnErrorF = { r -> r.toRuntimeError(mapper) }
    ): Result<T> =
        post<T>(
            uri = uri,
            body = body,
            headers = { h ->
                headers(h)
                h.setBearerAuth(token)
            },
            onError = onError
        )

    suspend inline fun <reified T> postMultipartFile(
        token: String,
        uri: String,
        body: Any,
        crossinline headers: HeadersF = {},
        crossinline onError: OnErrorF = { r -> r.toRuntimeError(mapper) },
    ): Result<T> =
        postMultipartFile<T>(
            uri = uri,
            multipartData = body,
            headers = { h ->
                headers(h)
                h.setBearerAuth(token)
            },
            onError = onError
        )

    suspend inline fun <reified T> patch(
        token: String,
        uri: String,
        body: Any? = null,
        crossinline headers: HeadersF = {},
        crossinline onError: OnErrorF = { r -> r.toRuntimeError(mapper) }
    ): Result<T> =
        patch<T>(
            uri = uri,
            body = body,
            headers = { h ->
                headers(h)
                h.setBearerAuth(token)
            },
            onError = onError
        )

    suspend inline fun <reified T> put(
        token: String,
        uri: String,
        body: Any? = null,
        crossinline headers: HeadersF = {},
        crossinline onError: OnErrorF = { r -> r.toRuntimeError(mapper) }
    ): Result<T> =
        put<T>(
            uri = uri,
            body = body,
            headers = { h ->
                headers(h)
                h.setBearerAuth(token)
            },
            onError = onError
        )

    suspend inline fun <reified T> delete(
        token: String,
        uri: String,
        crossinline headers: HeadersF = {},
        crossinline onError: OnErrorF = { r -> r.toRuntimeError(mapper) }
    ): Result<T> =
        delete<T>(
            uri = uri,
            headers = { h ->
                headers(h)
                h.setBearerAuth(token)
            },
            onError = onError
        )
}
