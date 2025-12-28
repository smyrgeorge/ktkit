package io.github.smyrgeorge.ktorlib

import io.github.smyrgeorge.ktorlib.api.rest.AbstractRestHandler
import io.github.smyrgeorge.ktorlib.api.rest.ExceptionHandler.installExceptionHandling
import io.github.smyrgeorge.ktorlib.api.rest.auth.AuthenticationProvider.Companion.installAuthenticationProvider
import io.github.smyrgeorge.ktorlib.api.rest.auth.PrincipalExtractor
import io.github.smyrgeorge.ktorlib.context.UserToken
import io.github.smyrgeorge.ktorlib.util.ANONYMOUS_USER
import io.github.smyrgeorge.ktorlib.util.SYSTEM_USER
import io.github.smyrgeorge.ktorlib.util.applicationLogger
import io.github.smyrgeorge.ktorlib.util.getAll
import io.github.smyrgeorge.ktorlib.util.httpEngine
import io.github.smyrgeorge.ktorlib.util.pwd
import io.github.smyrgeorge.ktorlib.util.registerShutdownHook
import io.github.smyrgeorge.log4k.Logger
import io.github.smyrgeorge.log4k.RootLogger
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.authenticate
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.EngineConnectorBuilder
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import io.ktor.server.application.Application as KtorApplication

@Suppress("unused")
class Application(
    private val name: String,
    private val host: String = "localhost",
    private val port: Int = 8080,
    private val configure: Configurer.() -> Unit = {},
    private val postConfigure: suspend Application.() -> Unit = {}
) {
    val log: Logger = Logger.of(name)

    private var _di: KoinApplication? = null
    private var _ktor: KtorApplication? = null
    private var _server: EmbeddedServer<ApplicationEngine, ApplicationEngine.Configuration>? = null

    val di: KoinApplication
        get() = _di ?: error("Koin Application not initialized. Run start() first.")
    val ktor: KtorApplication
        get() = _ktor ?: error("Ktor Application not initialized. Run start() first.")
    val server: EmbeddedServer<ApplicationEngine, ApplicationEngine.Configuration>
        get() = _server ?: error("Ktor Server not initialized. Run start() first.")

    private fun makeServer() = embeddedServer(
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
            _ktor = this
            Configurer(this@Application, this).apply(configure).configure()
            postConfigure()
        }
    )

    fun start(wait: Boolean = true) {
        log.info("PWD: ${pwd()}")
        log.info("Starting $name...")
        makeServer().apply {
            _server = this
            INSTANCE = this@Application
            registerShutdownHook()
        }.start(wait)
    }

    fun shutdown(gracePeriod: Duration = 1.seconds, timeout: Duration = 5.seconds) {
        log.info("Shutting down...")
        di.close()
        server.stop(gracePeriod.inWholeMilliseconds, timeout.inWholeMilliseconds)
    }

    class Configurer(
        private val app: Application,
        private val ktor: KtorApplication
    ) {
        private var module: Module = Module()
        private var json: Json = Json { default() }
        private var authenticationExtractor: PrincipalExtractor? = null
        private var routes: MutableList<AbstractRestHandler> = mutableListOf()
        private var other: KtorApplication.() -> Unit = {}

        fun di(config: Module.() -> Unit) {
            module.config()
        }


        fun withSystemUser(user: UserToken) {
            SYSTEM_USER = user
        }

        fun withAnonymousUser(user: UserToken) {
            ANONYMOUS_USER = user
        }

        fun withAuthenticationExtractor(extractor: PrincipalExtractor) {
            authenticationExtractor = extractor
        }

        internal fun <T : AbstractRestHandler> withRestHandler(handler: T) {
            routes.add(handler)
        }

        internal fun withRestHandlers(vararg handlers: AbstractRestHandler) {
            routes.addAll(handlers)
        }

        internal fun withRestHandlers(handlers: List<AbstractRestHandler>) {
            routes.addAll(handlers)
        }

        fun logging(config: RootLogger.Logging.() -> Unit) {
            RootLogger.Logging.config()
        }

        fun tracing(config: RootLogger.Tracing.() -> Unit) {
            RootLogger.Tracing.config()
        }

        fun metering(config: RootLogger.Metering.() -> Unit) {
            RootLogger.Metering.config()
        }

        private fun JsonBuilder.default() {
            isLenient = true
            prettyPrint = false
            ignoreUnknownKeys = true
            explicitNulls = false
        }

        fun json(config: JsonBuilder.() -> Unit) {
            json = Json {
                default()
                config()
            }
        }

        fun ktor(config: KtorApplication.() -> Unit) {
            this.other = config
        }

        internal fun configure() {
            // Start Koin.
            startKoin { modules(module) }.apply {
                app._di = this
                // Auto register discovered REST handlers.
                withRestHandlers(getAll<AbstractRestHandler>())
            }

            ktor.apply {
                // Install exception handling for structured error responses.
                installExceptionHandling()

                // Install content negotiation for JSON serialization
                install(ContentNegotiation) {
                    json(json)
                }

                // Install authentication.
                authenticationExtractor?.let { installAuthenticationProvider { extractor = it } }

                // Register routes.
                routing {
                    val routes: Route.() -> Unit = {
                        this@Configurer.routes.forEach {
                            log.info("Registering REST Handler: ${it::class.simpleName}")
                            with(it) { routes() }
                        }
                    }

                    // If authentication is configured, wrap routes in authenticate block
                    if (authenticationExtractor != null) {
                        authenticate(authenticationExtractor!!.name(), optional = false, build = routes)
                    } else {
                        routes()
                    }
                }

                // Other configuration for Ktor.
                other()
            }
        }
    }

    companion object {
        lateinit var INSTANCE: Application
    }
}
