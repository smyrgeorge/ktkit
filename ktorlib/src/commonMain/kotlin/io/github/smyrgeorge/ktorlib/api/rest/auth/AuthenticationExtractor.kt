package io.github.smyrgeorge.ktorlib.api.rest.auth

import io.github.smyrgeorge.ktorlib.domain.UserToken
import io.ktor.server.application.*

/**
 * Interface for extracting user authentication information from HTTP requests.
 *
 * Implementations should define how to extract user tokens from specific
 * authentication mechanisms (e.g., Bearer tokens, custom headers, etc.).
 */
interface AuthenticationExtractor {

    /**
     * Extracts user authentication from the ApplicationCall.
     *
     * @param call The Ktor ApplicationCall containing the HTTP request
     * @return UserToken if authentication is successful, null otherwise
     */
    suspend fun extract(call: ApplicationCall): UserToken?
}
