package io.github.smyrgeorge.ktkit

import io.github.smyrgeorge.ktkit.api.auth.impl.UserToken
import io.github.smyrgeorge.ktkit.api.rest.AbstractRestHandler
import io.github.smyrgeorge.ktkit.context.Principal
import io.github.smyrgeorge.ktkit.error.ErrorSpec
import io.github.smyrgeorge.ktkit.error.system.BadRequest
import io.github.smyrgeorge.ktkit.error.system.DatabaseError
import io.github.smyrgeorge.ktkit.error.system.Forbidden
import io.github.smyrgeorge.ktkit.error.system.InternalServerError
import io.github.smyrgeorge.ktkit.error.system.MissingParameter
import io.github.smyrgeorge.ktkit.error.system.NotFound
import io.github.smyrgeorge.ktkit.error.system.Unauthorized
import io.github.smyrgeorge.ktkit.error.system.UnknownError
import io.github.smyrgeorge.ktkit.error.system.UnsupportedEnumValue
import io.github.smyrgeorge.ktkit.util.applicationLogger
import io.github.smyrgeorge.ktkit.util.getAll
import io.github.smyrgeorge.ktkit.util.httpEngine
import io.github.smyrgeorge.ktkit.util.registerShutdownHook
import io.github.smyrgeorge.log4k.Logger
import io.github.smyrgeorge.log4k.RootLogger
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.EngineConnectorBuilder
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
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

    private val shutdownHooks = mutableListOf<() -> Unit>()

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
        log.info { "Starting $name..." }
        makeServer().apply {
            _server = this
            INSTANCE_OR_NULL = this@Application
            registerShutdownHook()
        }.start(wait)
    }

    fun shutdown(gracePeriod: Duration = 1.seconds, timeout: Duration = 5.seconds) {
        log.info { "Shutting down..." }
        shutdownHooks.forEach { it() }
        di.close()
        server.stop(gracePeriod.inWholeMilliseconds, timeout.inWholeMilliseconds)
    }

    fun onShutdown(hook: () -> Unit) {
        shutdownHooks.add(hook)
    }

    class Configurer(
        private val app: Application,
        private val ktor: KtorApplication
    ) {
        private var module: Module = Module()
        private var json: Json = Json { default() }
        private var routes: MutableList<AbstractRestHandler> = mutableListOf()
        private var other: KtorApplication.() -> Unit = {}

        fun di(config: Module.() -> Unit) {
            module.config()
        }

        fun withSystemUser(user: Principal) {
            SYSTEM_USER = user
        }

        fun withAnonymousUser(user: Principal) {
            ANONYMOUS_USER = user
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
            classDiscriminator = "@type"
            serializersModule = defaultSerializersModule
        }

        fun json(config: JsonBuilder.() -> Unit) {
            json = Json {
                // Apply default configuration.
                default()
                // Apply custom configuration.
                config()
                // Override default configuration.
                classDiscriminator = "@type"
                serializersModule += defaultSerializersModule
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
                // Install content negotiation for JSON serialization
                install(ContentNegotiation) {
                    json(json)
                }

                // Register routes.
                routing {
                    routes.forEach {
                        log.info("Registering REST Handler: ${it::class.simpleName}")
                        with(it) { routes() }
                    }
                }

                // Other configuration for Ktor.
                other()
            }
        }
    }

    companion object {
        var INSTANCE_OR_NULL: Application? = null
        val INSTANCE: Application get() = INSTANCE_OR_NULL ?: error("Application not initialized. Run start() first.")
        val di: KoinApplication get() = INSTANCE.di
        private val defaultSerializersModule = SerializersModule {
            polymorphic(ErrorSpec::class) {
                subclass(BadRequest::class)
                subclass(DatabaseError::class)
                subclass(Forbidden::class)
                subclass(InternalServerError::class)
                subclass(MissingParameter::class)
                subclass(NotFound::class)
                subclass(Unauthorized::class)
                subclass(UnknownError::class)
                subclass(UnsupportedEnumValue::class)
            }
        }

        var SYSTEM_USER: Principal = UserToken.DEFAULT_SYSTEM_USER
        var ANONYMOUS_USER: Principal = UserToken.DEFAULT_ANONYMOUS_USER
    }
}
