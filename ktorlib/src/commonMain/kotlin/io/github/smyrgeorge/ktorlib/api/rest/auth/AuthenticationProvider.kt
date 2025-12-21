package io.github.smyrgeorge.ktorlib.api.rest.auth

import io.github.smyrgeorge.ktorlib.context.UserToken
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.AuthenticationContext
import io.ktor.util.logging.Logger
import io.ktor.server.auth.AuthenticationProvider as KtorAuthenticationProvider

/**
 * Represents an authentication provider for Ktor applications.
 *
 * This class is responsible for managing authentication mechanisms based on user tokens
 * extracted from incoming HTTP requests. It integrates with Ktor's authentication system
 * and provides functionality for validating and assigning authenticated principals.
 *
 * @constructor Creates an instance of the authentication provider using the given configuration.
 * @param config A configuration object containing the necessary components such as logger and principal extractor.
 */
class AuthenticationProvider(
    private val config: Config
) : KtorAuthenticationProvider(config) {
    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val userToken: Result<UserToken?> = config.extractor.extract(context.call)
        userToken
            .onSuccess { it?.let { context.principal(config.name, it) } }
            .onFailure { config.log.debug("Failed to extract user token from request: ${it.message}") }
    }

    class Config(
        val log: Logger,
        val extractor: PrincipalExtractor
    ) : KtorAuthenticationProvider.Config(extractor::class.simpleName ?: "extractor-${extractor.hashCode()}") {
        class Builder {
            var log: Logger? = null
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
        fun Application.installAuthenticationProvider(configure: Config.Builder.() -> Unit) {
            install(Authentication) {
                val config = Config.Builder().apply(configure)
                val log = config.log ?: error("Logger must be configured in ktorlib authentication provider")
                val extractor = config.extractor ?: error("AuthenticationExtractor must be configured in ktorlib authentication provider")
                val provider = AuthenticationProvider(Config(log, extractor))
                register(provider)
            }
        }
    }
}
