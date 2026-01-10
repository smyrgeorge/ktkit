@file:Suppress("unused")

package io.github.smyrgeorge.ktkit.jvm.ktor.client.impl

import io.github.smyrgeorge.ktkit.jvm.ktor.client.AbstractHttpClient
import io.github.smyrgeorge.ktkit.jvm.ktor.client.HeadersF
import io.ktor.client.HttpClient
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders

/**
 * A specialized HTTP client that facilitates interaction with APIs using bearer token authentication.
 * This client provides various HTTP methods such as GET, POST, PATCH, PUT, and DELETE, and supports
 * customization of request headers.
 *
 * @constructor Initializes an instance of [BearerHttpClient] with a specified base [HttpClient] and base URL.
 * @param client The base [HttpClient] used for making HTTP requests.
 * @param baseUrl The base URL for all requests made by this client.
 */
class BearerHttpClient(
    client: HttpClient,
    baseUrl: String,
) : AbstractHttpClient(client, baseUrl) {
    suspend inline fun <reified T> get(
        token: String,
        uri: String,
        crossinline headers: HeadersF = {},
    ): Result<T> =
        get<T>(
            uri = uri,
            headers = { h ->
                headers(h)
                h.append(HttpHeaders.Authorization, "Bearer $token")
            }
        )

    suspend inline fun <reified T> post(
        token: String,
        uri: String,
        body: Any,
        crossinline headers: HeadersF = {},
    ): Result<T> =
        post<T>(
            uri = uri,
            body = body,
            headers = { h ->
                headers(h)
                h.append(HttpHeaders.Authorization, "Bearer $token")
            }
        )

    suspend inline fun <reified T> postMultipartFile(
        token: String,
        uri: String,
        multipartData: ByteArray,
        fileName: String = "file",
        contentType: ContentType = ContentType.Application.OctetStream,
        crossinline headers: HeadersF = {},
    ): Result<T> =
        postMultipartFile<T>(
            uri = uri,
            multipartData = multipartData,
            fileName = fileName,
            contentType = contentType,
            headers = { h ->
                headers(h)
                h.append(HttpHeaders.Authorization, "Bearer $token")
            }
        )

    suspend inline fun <reified T> patch(
        token: String,
        uri: String,
        body: Any? = null,
        crossinline headers: HeadersF = {},
    ): Result<T> =
        patch<T>(
            uri = uri,
            body = body,
            headers = { h ->
                headers(h)
                h.append(HttpHeaders.Authorization, "Bearer $token")
            }
        )

    suspend inline fun <reified T> put(
        token: String,
        uri: String,
        body: Any? = null,
        crossinline headers: HeadersF = {},
    ): Result<T> =
        put<T>(
            uri = uri,
            body = body,
            headers = { h ->
                headers(h)
                h.append(HttpHeaders.Authorization, "Bearer $token")
            }
        )

    suspend inline fun <reified T> delete(
        token: String,
        uri: String,
        crossinline headers: HeadersF = {},
    ): Result<T> =
        delete<T>(
            uri = uri,
            headers = { h ->
                headers(h)
                h.append(HttpHeaders.Authorization, "Bearer $token")
            }
        )
}
