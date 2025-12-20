package io.github.smyrgeorge.ktorlib.api.rest.auth

import io.github.smyrgeorge.ktorlib.domain.UserToken
import io.ktor.server.application.ApplicationCall

/**
 * Interface for extracting user authentication information from HTTP requests.
 *
 * Implementations should define how to extract user tokens from specific
 * authentication mechanisms (e.g., Bearer tokens, custom headers, API keys, etc.).
 *
 * Built-in implementations:
 * - [io.github.smyrgeorge.ktorlib.api.rest.auth.impl.XRealNamePrincipalExtractor] - Extracts user from Base64-encoded JSON header
 *
 * Usage with the generic ktorlib provider:
 * ```
 * install(Authentication) {
 *     ktorlib {
 *         extractor = XRealNameAuthenticationExtractor()
 *     }
 * }
 * ```
 *
 * Creating a custom extractor:
 * ```
 * class CustomExtractor : AuthenticationExtractor {
 *     override suspend fun extract(call: ApplicationCall): UserToken? {
 *         val token = call.request.headers["X-Custom-Auth"] ?: return null
 *         // Parse and validate token
 *         return UserToken(...)
 *     }
 * }
 * ```
 */
interface PrinciplaExtractor {

    /**
     * Extracts user authentication from the ApplicationCall.
     *
     * @param call The Ktor ApplicationCall containing the HTTP request
     * @return UserToken if authentication is successful, null otherwise
     */
    suspend fun extract(call: ApplicationCall): UserToken?
}
