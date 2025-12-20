package io.github.smyrgeorge.ktorlib.api.rest.auth

import io.github.smyrgeorge.ktorlib.domain.UserToken
import io.github.smyrgeorge.ktorlib.error.types.UnauthorizedImpl
import io.ktor.server.application.*
import io.ktor.util.*

/**
 * Configuration for the Authentication plugin.
 *
 * @property extractor The authentication extractor to use for extracting user tokens
 */
class AuthenticationConfig {
    var extractor: AuthenticationExtractor? = null
}

/**
 * Ktor plugin for handling authentication.
 *
 * This plugin extracts user authentication information from incoming requests
 * and stores it in the call attributes for later access.
 *
 * Usage:
 * ```
 * install(Authentication) {
 *     extractor = XRealNameAuthenticationExtractor()
 * }
 * ```
 */
val Authentication = createApplicationPlugin(
    name = "Authentication",
    createConfiguration = ::AuthenticationConfig
) {
    val extractor = pluginConfig.extractor
        ?: error("AuthenticationExtractor must be configured")

    onCall { call ->
        val userToken = extractor.extract(call)
            ?: UnauthorizedImpl("Authentication required").ex()
        // Store the user token in call attributes for later access
        call.attributes.put(UserTokenKey, userToken)
    }
}

/**
 * Attribute key for storing the authenticated user token in the call.
 */
val UserTokenKey = AttributeKey<UserToken>("UserToken")
