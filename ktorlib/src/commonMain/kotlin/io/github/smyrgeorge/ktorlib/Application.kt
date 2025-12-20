package io.github.smyrgeorge.ktorlib

import io.github.smyrgeorge.ktorlib.api.rest.AbstractRestHandler
import io.github.smyrgeorge.ktorlib.api.rest.ExceptionHandler.installExceptionHandling
import io.github.smyrgeorge.ktorlib.api.rest.auth.AuthenticationProvider.Companion.installAuthenticationProvider
import io.github.smyrgeorge.ktorlib.api.rest.auth.PrincipalExtractor
import io.github.smyrgeorge.ktorlib.util.applicationLogger
import io.github.smyrgeorge.ktorlib.util.httpEngine
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import io.ktor.server.application.Application as KtorApplication

class Application(
    private val name: String,
    private val host: String = "localhost",
    private val port: Int = 8080,
    private val configure: Configurer.() -> Unit
) {
    private fun makeServer(): EmbeddedServer<ApplicationEngine, ApplicationEngine.Configuration> = embeddedServer(
        factory = httpEngine(),
        environment = applicationEnvironment {
            log = applicationLogger(name)
        },
        configure = {
            connectors.add(
                EngineConnectorBuilder().apply {
                    host = this@Application.host
                    port = this@Application.port
                }
            )
        },
        module = {
            val configurer = Configurer(this)
            configure(configurer)
            configurer.configure()
        }
    )

    fun start(wait: Boolean = true) {
        makeServer().start(wait)
    }

    class Configurer(
        private val application: KtorApplication
    ) {
        private var authenticationExtractor: PrincipalExtractor? = null
        private var routes: MutableList<AbstractRestHandler> = mutableListOf()
        private var other: KtorApplication.() -> Unit = {}

        fun withAuthenticationExtractor(extractor: PrincipalExtractor): Configurer {
            authenticationExtractor = extractor
            return this
        }

        fun withRestHandler(handler: AbstractRestHandler): Configurer {
            routes.add(handler)
            return this
        }

        fun ktor(other: KtorApplication.() -> Unit): Configurer {
            this.other = other
            return this
        }

        internal fun configure(): KtorApplication = application.apply {
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
                    log.info("Registering REST Handler: ${it::class.simpleName}")
                    with(it) { routes() }
                }
            }

            other()
        }
    }
}