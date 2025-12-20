package io.github.smyrgeorge.ktorlib.application

import io.github.smyrgeorge.ktorlib.api.rest.AbstractRestHandler
import io.github.smyrgeorge.ktorlib.api.rest.ExceptionHandler.installExceptionHandling
import io.github.smyrgeorge.ktorlib.api.rest.auth.AuthenticationProvider.Companion.installAuthenticationProvider
import io.github.smyrgeorge.ktorlib.api.rest.auth.PrinciplaExtractor
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

class ApplicationBuilder(
    private val application: Application
) {
    private var authenticationExtractor: PrinciplaExtractor? = null
    private var routes: Array<out AbstractRestHandler> = emptyArray()

    fun withAuthenticationExtractor(extractor: PrinciplaExtractor): ApplicationBuilder {
        authenticationExtractor = extractor
        return this
    }

    fun withRoutes(vararg routes: AbstractRestHandler): ApplicationBuilder {
        this.routes = routes
        return this
    }


    fun build(): Application = application.apply {
        // Install exception handling for structured error responses.
        installExceptionHandling()

        // Install content negotiation for JSON serialization
        install(ContentNegotiation) {
            json(Json {
                isLenient = true
                prettyPrint = false
                ignoreUnknownKeys = true
                explicitNulls = false
            })
        }

        // Install authentication.
        authenticationExtractor?.let { installAuthenticationProvider { extractor = it } }

        // Register routes.
        routing {
            routes.forEach {
                log.info("Registering routes: ${it::class.simpleName}")
                with(it) { routes() }
            }
        }
    }

    companion object {
        fun Application.builder(): ApplicationBuilder = ApplicationBuilder(this)
    }
}
