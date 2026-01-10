@file:Suppress("unused")

package io.github.smyrgeorge.ktkit.spring.webclient.impl

import io.github.smyrgeorge.ktkit.api.auth.impl.UserToken
import io.github.smyrgeorge.ktkit.api.auth.impl.XRealNamePrincipalExtractor
import io.github.smyrgeorge.ktkit.api.auth.impl.XRealNamePrincipalExtractor.toXRealName
import io.github.smyrgeorge.ktkit.spring.webclient.AbstractWebClient
import io.github.smyrgeorge.ktkit.spring.webclient.OnErrorF
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient
import tools.jackson.databind.json.JsonMapper
import java.util.Base64

/**
 * XRealNameWebClient is an implementation of AbstractWebClient that provides additional user token handling
 * through the use of the `x-real-name` header. It simplifies performing various HTTP operations
 * such as GET, POST, PATCH, PUT, and DELETE with pre-set configurations for attaching user tokens.
 *
 * @constructor Initializes an XRealNameWebClient instance with a base URL, a WebClient instance, and a JSON mapper.
 *
 * @param client The WebClient instance used for making HTTP requests.
 * @param baseUrl The base URL used for constructing request URLs.
 * @param jm The JsonMapper instance responsible for handling JSON serialization and deserialization.
 */
class XRealNameWebClient(
    client: WebClient,
    baseUrl: String,
    val jm: JsonMapper,
) : AbstractWebClient(client, baseUrl) {
    suspend inline fun <reified T> get(
        token: UserToken,
        uri: String,
        crossinline onError: OnErrorF = { r -> r.toRuntimeError(jm) }
    ): Result<T> =
        get<T>(
            uri = uri,
            headers = { h -> h.addUser(token) },
            onError = onError
        )

    suspend inline fun <reified T> post(
        token: UserToken,
        uri: String,
        body: Any,
        crossinline onError: OnErrorF = { r -> r.toRuntimeError(jm) },
    ): Result<T> =
        post<T>(
            uri = uri,
            body = body,
            headers = { h -> h.addUser(token) },
            onError = onError
        )

    suspend inline fun <reified T> postMultipartFile(
        token: UserToken,
        uri: String,
        body: Any,
        crossinline onError: OnErrorF = { r -> r.toRuntimeError(jm) },
    ): Result<T> =
        postMultipartFile<T>(
            uri = uri,
            multipartData = body,
            headers = { h -> h.addUser(token) },
            onError = onError
        )

    suspend inline fun <reified T> patch(
        token: UserToken,
        uri: String,
        body: Any? = null,
        crossinline onError: OnErrorF = { r -> r.toRuntimeError(jm) },
    ): Result<T> =
        patch<T>(
            uri = uri,
            body = body,
            headers = { h -> h.addUser(token) },
            onError = onError
        )

    suspend inline fun <reified T> put(
        token: UserToken,
        uri: String,
        body: Any? = null,
        crossinline onError: OnErrorF = { r -> r.toRuntimeError(jm) },
    ): Result<T> =
        put<T>(
            uri = uri,
            body = body,
            headers = { h -> h.addUser(token) },
            onError = onError
        )

    suspend inline fun <reified T> delete(
        token: UserToken,
        uri: String,
        crossinline onError: OnErrorF = { r -> r.toRuntimeError(jm) },
    ): Result<T> =
        delete<T>(
            uri = uri,
            headers = { h -> h.addUser(token) },
            onError = onError
        )

    /**
     * Add to the headers the [x-real-name] header with the [Base64] formatted [UserToken].
     */
    fun HttpHeaders.addUser(token: UserToken) {
        set(XRealNamePrincipalExtractor.HEADER_NAME, token.toXRealName())
    }
}
