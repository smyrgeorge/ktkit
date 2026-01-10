@file:Suppress("unused")

package io.github.smyrgeorge.ktkit.jvm.ktor.client.impl

import io.github.smyrgeorge.ktkit.api.auth.impl.UserToken
import io.github.smyrgeorge.ktkit.api.auth.impl.XRealNamePrincipalExtractor
import io.github.smyrgeorge.ktkit.api.auth.impl.XRealNamePrincipalExtractor.toXRealName
import io.github.smyrgeorge.ktkit.jvm.ktor.client.AbstractHttpClient
import io.ktor.client.HttpClient
import io.ktor.http.ContentType
import io.ktor.http.HeadersBuilder

/**
 * XRealNameHttpClient is an implementation of AbstractHttpClient that provides additional user token handling
 * through the use of the `x-real-name` header. It simplifies performing various HTTP operations
 * such as GET, POST, PATCH, PUT, and DELETE with pre-set configurations for attaching user tokens.
 *
 * @constructor Initializes an XRealNameHttpClient instance with a base URL and an HttpClient instance.
 *
 * @param client The HttpClient instance used for making HTTP requests.
 * @param baseUrl The base URL used for constructing request URLs.
 */
class XRealNameHttpClient(
    client: HttpClient,
    baseUrl: String,
) : AbstractHttpClient(client, baseUrl) {
    suspend inline fun <reified T> get(
        token: UserToken,
        uri: String,
    ): Result<T> =
        get<T>(
            uri = uri,
            headers = { h -> h.addUser(token) }
        )

    suspend inline fun <reified T> post(
        token: UserToken,
        uri: String,
        body: Any,
    ): Result<T> =
        post<T>(
            uri = uri,
            body = body,
            headers = { h -> h.addUser(token) }
        )

    suspend inline fun <reified T> postMultipartFile(
        token: UserToken,
        uri: String,
        multipartData: ByteArray,
        fileName: String = "file",
        contentType: ContentType = ContentType.Application.OctetStream,
    ): Result<T> =
        postMultipartFile<T>(
            uri = uri,
            multipartData = multipartData,
            fileName = fileName,
            contentType = contentType,
            headers = { h -> h.addUser(token) }
        )

    suspend inline fun <reified T> patch(
        token: UserToken,
        uri: String,
        body: Any? = null,
    ): Result<T> =
        patch<T>(
            uri = uri,
            body = body,
            headers = { h -> h.addUser(token) }
        )

    suspend inline fun <reified T> put(
        token: UserToken,
        uri: String,
        body: Any? = null,
    ): Result<T> =
        put<T>(
            uri = uri,
            body = body,
            headers = { h -> h.addUser(token) }
        )

    suspend inline fun <reified T> delete(
        token: UserToken,
        uri: String,
    ): Result<T> =
        delete<T>(
            uri = uri,
            headers = { h -> h.addUser(token) }
        )

    /**
     * Add to the headers the [x-real-name] header with the [Base64] formatted [UserToken].
     */
    fun HeadersBuilder.addUser(token: UserToken) {
        append(XRealNamePrincipalExtractor.HEADER_NAME, token.toXRealName())
    }
}
