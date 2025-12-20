package io.github.smyrgeorge.ktorlib.api.rest.auth

import io.github.smyrgeorge.ktorlib.domain.UserToken
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.AuthenticationContext
import io.ktor.server.auth.AuthenticationProvider as KtorAuthenticationProvider

/**
 * AuthenticationProvider is a Ktor authentication provider designed to integrate
 * flexible and customizable authentication mechanisms within a Ktor application.
 *
 * This implementation relies on an [PrincipalExtractor] to parse authentication
 * information from incoming HTTP requests, providing an authenticated principal if successful.
 *
 * @constructor Instantiates the AuthenticationProvider with the specified configuration.
 * @param config Configuration containing the required [PrincipalExtractor].
 */
class AuthenticationProvider(
    private val config: Config
) : KtorAuthenticationProvider(config) {
    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val call = context.call
        val userToken: UserToken? = config.extractor.extract(call)

        if (userToken != null) {
            context.principal(userToken)
        }
    }

    class Config(
        val extractor: PrincipalExtractor
    ) : KtorAuthenticationProvider.Config(extractor::class.simpleName ?: "extractor-${extractor.hashCode()}") {
        class Template {
            var extractor: PrincipalExtractor? = null
        }
    }

    companion object {
        /**
         * Extension function to easily install an authentication provider.
         *
         * Usage:
         * ```
         * fun Application.module() {
         *     installAuthenticationProvider {
         *         extractor = XRealNameAuthenticationExtractor()
         *     }
         *     // ... other configuration
         * }
         *
         */
        fun Application.installAuthenticationProvider(configure: Config.Template.() -> Unit) {
            install(Authentication) {
                val config = Config.Template().apply(configure)
                val extractor = config.extractor
                    ?: error("AuthenticationExtractor must be configured in ktorlib authentication provider")

                val provider = AuthenticationProvider(Config(extractor))
                register(provider)
            }
        }
    }
}
