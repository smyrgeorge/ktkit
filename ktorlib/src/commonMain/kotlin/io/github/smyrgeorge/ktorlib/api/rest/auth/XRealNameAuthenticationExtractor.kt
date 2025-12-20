package io.github.smyrgeorge.ktorlib.api.rest.auth

import io.github.smyrgeorge.ktorlib.domain.UserToken
import io.github.smyrgeorge.ktorlib.error.types.UnauthorizedImpl
import io.ktor.server.application.*
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

/**
 * Authentication extractor that retrieves user information from a custom header.
 *
 * This extractor expects a Base64-encoded JSON representation of [UserToken]
 * in the specified header (default: "x-real-name").
 *
 * Common use cases:
 * - Internal service-to-service communication
 * - API gateway authentication forwarding
 * - Testing and development environments
 */
class XRealNameAuthenticationExtractor : AuthenticationExtractor {
    private val headerName: String = "x-real-name"
    private val serde: Json = Json { ignoreUnknownKeys = true }

    override suspend fun extract(call: ApplicationCall): UserToken? {
        val header = call.request.headers[headerName] ?: return null
        return try {
            val json = Base64.decode(header).decodeToString()
            serde.decodeFromString<UserToken>(json)
        } catch (e: Exception) {
            UnauthorizedImpl("Cannot extract $headerName header: ${e.message}").ex(e)
        }
    }
}
