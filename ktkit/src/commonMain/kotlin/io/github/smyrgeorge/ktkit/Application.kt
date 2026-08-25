package io.github.smyrgeorge.ktkit

import io.github.smyrgeorge.ktkit.api.auth.impl.UserToken
import io.github.smyrgeorge.ktkit.api.error.impl.NotFound
import io.github.smyrgeorge.ktkit.api.rest.AbstractRestHandler
import io.github.smyrgeorge.ktkit.api.rest.ApiError
import io.github.smyrgeorge.ktkit.api.rest.impl.ApplicationStatusRestHandler
import io.github.smyrgeorge.ktkit.api.rest.openapi.OpenApiRestHandler
import io.github.smyrgeorge.ktkit.context.Principal
import io.github.smyrgeorge.ktkit.util.applicationLogger
import io.github.smyrgeorge.ktkit.util.defaultSerializersModule
import io.github.smyrgeorge.ktkit.util.defaultWithErrors
import io.github.smyrgeorge.ktkit.util.getAll
import io.github.smyrgeorge.ktkit.util.httpEngine
import io.github.smyrgeorge.ktkit.util.registerShutdownHook
import io.github.smyrgeorge.log4k.Logger
import io.github.smyrgeorge.log4k.RootLogger
import io.github.smyrgeorge.log4k.classic.info
import io.github.smyrgeorge.log4k.impl.appenders.simple.SimpleMeteringCollectorAppender
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.EngineConnectorBuilder
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.modules.plus
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import io.ktor.server.application.Application as KtorApplication

@Suppress("unused")
class Application(
    val name: String,
    val description: String? = null,
    val conf: Conf,
    private val configure: Configurer.() -> Unit = {},
    private val postConfigure: suspend Application.() -> Unit = {}
) {
    val log: Logger = Logger.of(name)

    private var _status: Status = Status.DOWN
    private var _startedAt: Instant? = null
    private var _json: Json? = null
    private var _di: KoinApplication? = null
    private var _ktor: KtorApplication? = null
    private var _server: EmbeddedServer<ApplicationEngine, ApplicationEngine.Configuration>? = null

    internal val shutdownHooks = mutableListOf<suspend () -> Unit>()
    internal val metrics = SimpleMeteringCollectorAppender()

    val status: Status get() = _status
    val startedAt: Instant get() = _startedAt ?: error("Application not started yet. Run start() first.")
    val json: Json
        get() = _json ?: error("JSON Serializer not initialized. Run start() first.")
    val di: KoinApplication
        get() = _di ?: error("Koin Application not initialized. Run start() first.")
    val ktor: KtorApplication
        get() = _ktor ?: error("Ktor Application not initialized. Run start() first.")
    val server: EmbeddedServer<ApplicationEngine, ApplicationEngine.Configuration>
        get() = _server ?: error("Ktor Server not initialized. Run start() first.")

    init {
        INSTANCE_OR_NULL = this
    }

    /**
     * Starts the application by initializing the server, configuring the environment,
     * and registering a shutdown hook for graceful termination.
     *
     * This method logs the startup sequence and ensures that the server is set up
     * with the application-specific configuration and modules provided during initialization.
     * Once started, the server will begin listening for incoming requests and will block
     * until explicitly stopped.
     *
     * The startup process includes:
     * - Creating an embedded server instance with the configured HTTP engine and application environment.
     * - Applying custom application configurations and modules, such as JSON serialization, routing, and dependency injection.
     * - Registering a shutdown hook to handle cleanup tasks upon server termination.
     * - Starting the server and blocking the current thread until shutdown.
     */
    fun start() {
        log.info { "Starting $name..." }
        makeServer().apply {
            _server = this
            registerShutdownHook()
            _status = Status.UP
            _startedAt = Clock.System.now()
        }.start(wait = true)
    }

    /**
     * Shuts down the application by performing necessary cleanup tasks, invoking registered shutdown hooks,
     * closing the dependency injection context, and stopping the server gracefully.
     *
     * @param gracePeriod Duration to wait for ongoing requests to complete before forcefully stopping the server.
     *                    Default is 1 second.
     * @param timeout Duration to wait for the server to shut down completely after the grace period.
     *                Default is 5 seconds.
     */
    fun shutdown(gracePeriod: Duration = 1.seconds, timeout: Duration = 5.seconds) {
        log.info { "Shutting down..." }
        _status = Status.DOWN
        _startedAt = null
        shutdownHooks.forEach { runBlocking { it() } }
        di.close()
        server.stop(gracePeriod.inWholeMilliseconds, timeout.inWholeMilliseconds)
    }

    /**
     * Registers a shutdown hook that will be invoked when the application is shutting down.
     *
     * @param hook A lambda function to be executed during the shutdown process.
     */
    fun onShutdown(hook: suspend () -> Unit) {
        shutdownHooks.add(hook)
    }

    private fun makeServer() = embeddedServer(
        factory = httpEngine(),
        environment = applicationEnvironment {
            log = applicationLogger(name)
        },
        configure = {
            connectors.add(
                EngineConnectorBuilder().apply {
                    host = conf.host
                    port = conf.port
                }
            )
        },
        module = {
            _ktor = this
            Configurer(this@Application, this).apply(configure).configure()
            postConfigure()
        }
    )

    /**
     * Represents the operational status of the application.
     *
     * The `Status` enumeration defines two possible states for the application:
     * - `UP`: Indicates that the application is operational and running.
     * - `DOWN`: Indicates that the application is non-operational or has been shut down.
     *
     * This enum can be used to monitor and communicate the current state of the application,
     * especially in health checks and logging.
     */
    enum class Status {
        UP,
        DOWN
    }

    /**
     * Represents the configuration for an application, including settings for the host, port,
     * and error type host URL.
     *
     * @property host The hostname or IP address the application binds to. Defaults to "localhost".
     * @property port The port number the application listens on. Must be between 1 and 65535.
     *                Defaults to 8080.
     * @property includeTypePropertyInApiError Whether to include the type property in ApiError responses.
     * @property errorTypeHost The base URL used for errors, adhering to RFC 9457.
     *                         It must start with "http://" or "https://" and end with "/errors".
     * @property openApi Configuration of the OpenAPI documentation endpoints (see [OpenApi]).
     *
     * @constructor Ensures that:
     * - The `port` is a valid number within the acceptable range (1 to 65535).
     * - The `errorTypeHost` starts with a valid HTTP(S) protocol.
     * - The `errorTypeHost` ends with the required path "/errors".
     *
     * Throws [IllegalArgumentException] if any of the validation requirements are not met.
     */
    @Suppress("HttpUrlsUsage")
    data class Conf(
        val host: String = "localhost",
        val port: Int = 8080,
        val includeTypePropertyInApiError: Boolean = true,
        val errorTypeHost: String = "http://$host:$port/errors", // RFC 9457
        val openApi: OpenApi = OpenApi(),
    ) {
        init {
            require(port in 1..65535) { "Port must be between 1 and 65535" }
            require(errorTypeHost.startsWith("http://") || errorTypeHost.startsWith("https://")) {
                "Error Type Host must start with http:// or https://"
            }
            require(errorTypeHost.endsWith("/errors")) { "Error Type Host must end with /errors" }
        }

        /**
         * Configuration for generating and serving OpenAPI documentation.
         *
         * @property enabled Determines if the OpenAPI documentation is enabled. Defaults to `true`.
         * @property basePath The base path where the documentation will be served.
         *                    Must start with `/` and must not end with `/`. Defaults to `/api/docs`.
         * @property title The title of the API documentation. Optional. Must not be blank if specified.
         * @property version The version of the API. Defaults to `0.0.1`. Must not be blank.
         * @property description An optional description of the API documentation. Must not be blank if specified.
         * @property servers A list of server URLs associated with the API. Each URL must start with `http://` or `https://`.
         * @property theme The color theme of the documentation page. Defaults to [Theme.AUTO].
         * @property ui The interactive documentation UI served at the base path. Defaults to [Ui.Swagger].
         * @property security A list of authentication schemes advertised in the specification. Scheme names must be unique.
         */
        data class OpenApi(
            val enabled: Boolean = true,
            val basePath: String = "/api/docs",
            val title: String? = null,
            val version: String = "0.0.1",
            val description: String? = null,
            val servers: List<String> = emptyList(),
            val theme: Theme = Theme.AUTO,
            val ui: Ui = Ui.Swagger(),
            val security: List<SecurityScheme> = emptyList(),
        ) {
            init {
                require(basePath.startsWith("/") && basePath.length > 1 && !basePath.endsWith("/")) {
                    "OpenApi basePath must start with '/' and must not end with '/'"
                }
                require(title == null || title.isNotBlank()) { "OpenApi title must not be blank" }
                require(version.isNotBlank()) { "OpenApi version must not be blank" }
                require(description == null || description.isNotBlank()) { "OpenApi description must not be blank" }
                servers.forEach { server ->
                    require(server.startsWith("http://") || server.startsWith("https://")) {
                        "OpenApi server '$server' must start with http:// or https://"
                    }
                }
                require(security.map { it.name }.toSet().size == security.size) {
                    "OpenApi security scheme names must be unique"
                }
            }

            /**
             * Represents visual themes that can be applied to the user interface.
             *
             * Used to define UI appearance preferences such as light, dark, or automatic
             * theme switching based on system settings.
             */
            enum class Theme {
                /** Follow the browser/OS preference (`prefers-color-scheme`). */
                AUTO,
                LIGHT,
                DARK,
            }

            /**
             * Represents the different types of user interface (UI) integrations that can be used
             * for rendering OpenAPI specifications.
             */
            sealed interface Ui {
                /**
                 * [Swagger UI](https://swagger.io/tools/swagger-ui/).
                 *
                 * @property css The URL of the Swagger UI stylesheet. Defaults to the unpkg CDN —
                 *               point it at a self-hosted copy for air-gapped environments.
                 * @property js The URL of the Swagger UI bundle script. Defaults to the unpkg CDN —
                 *              point it at a self-hosted copy for air-gapped environments.
                 */
                data class Swagger(
                    val css: String = "https://unpkg.com/swagger-ui-dist@5/swagger-ui.css",
                    val js: String = "https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js",
                ) : Ui {
                    init {
                        requireAssetUrl(css, "Ui.Swagger css")
                        requireAssetUrl(js, "Ui.Swagger js")
                    }
                }

                /**
                 * [Scalar API Reference](https://scalar.com/products/api-references).
                 *
                 * @property js The URL of the Scalar API Reference script. Defaults to the jsDelivr
                 *              CDN — point it at a self-hosted copy for air-gapped environments.
                 */
                data class Scalar(
                    val js: String = "https://cdn.jsdelivr.net/npm/@scalar/api-reference",
                ) : Ui {
                    init {
                        requireAssetUrl(js, "Ui.Scalar js")
                    }
                }

                companion object {
                    /** UI assets are either absolute http(s) URLs or absolute paths served by the app itself. */
                    private fun requireAssetUrl(url: String, name: String) {
                        require(url.startsWith("http://") || url.startsWith("https://") || url.startsWith("/")) {
                            "OpenApi $name must start with http://, https:// or / (an absolute path)"
                        }
                    }
                }
            }

            /**
             * Represents a security scheme in an OpenAPI specification. A security scheme defines a method
             * of authentication or authorization for accessing the API's resources.
             */
            sealed interface SecurityScheme {
                /**
                 * The name of the scheme — its key under `components.securitySchemes`. Must be
                 * unique across the configured schemes and match `[a-zA-Z0-9._-]+`.
                 */
                val name: String

                /** An optional human-readable description of the scheme. */
                val description: String?

                /**
                 * An API key sent in a header or query parameter (`type: apiKey`).
                 *
                 * For example, the `x-real-name` header of [XRealNamePrincipalExtractor][io.github.smyrgeorge.ktkit.api.auth.impl.XRealNamePrincipalExtractor]:
                 * `ApiKey(paramName = "x-real-name")`.
                 *
                 * @property paramName The name of the header or query parameter carrying the key.
                 * @property location Where the key is sent: [Location.HEADER] (default) or [Location.QUERY].
                 */
                data class ApiKey(
                    val paramName: String,
                    val location: Location = Location.HEADER,
                    override val name: String = "apiKey",
                    override val description: String? = null,
                ) : SecurityScheme {
                    init {
                        requireNameAndDescription(name, description)
                        require(paramName.isNotBlank()) { "OpenApi security scheme '$name' paramName must not be blank" }
                    }

                    /** Where the API key is sent. */
                    enum class Location {
                        HEADER,
                        QUERY,
                    }
                }

                /** HTTP Basic authentication (`type: http`, `scheme: basic`). */
                data class HttpBasic(
                    override val name: String = "basicAuth",
                    override val description: String? = null,
                ) : SecurityScheme {
                    init {
                        requireNameAndDescription(name, description)
                    }
                }

                /**
                 * HTTP Bearer-token authentication (`type: http`, `scheme: bearer`).
                 *
                 * @property bearerFormat An optional hint at how the token is formatted (e.g. "JWT").
                 */
                data class HttpBearer(
                    val bearerFormat: String? = null,
                    override val name: String = "bearerAuth",
                    override val description: String? = null,
                ) : SecurityScheme {
                    init {
                        requireNameAndDescription(name, description)
                        require(bearerFormat == null || bearerFormat.isNotBlank()) {
                            "OpenApi security scheme '$name' bearerFormat must not be blank"
                        }
                    }
                }

                companion object {
                    private val NAME_REGEX = Regex("[a-zA-Z0-9._-]+")

                    private fun requireNameAndDescription(name: String, description: String?) {
                        require(name.matches(NAME_REGEX)) {
                            "OpenApi security scheme name '$name' must match [a-zA-Z0-9._-]+"
                        }
                        require(description == null || description.isNotBlank()) {
                            "OpenApi security scheme '$name' description must not be blank"
                        }
                    }
                }
            }
        }
    }

    /**
     * Configurer is responsible for setting up and configuring various parts of an application,
     * including dependency injection, JSON serialization, logging, tracing, and REST API routes.
     * It provides a fluent builder-style interface for defining application configurations.
     *
     * @property app The main application instance for which this Configurer is responsible.
     * @property ktor The Ktor application instance.
     */
    class Configurer(
        val app: Application,
        val ktor: KtorApplication,
    ) {
        private var module: Module = Module()
        private var json: Json = Json { defaultWithErrors() }
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

        fun logging(config: RootLogger.Logging.() -> Unit) {
            RootLogger.Logging.config()
        }

        fun tracing(config: RootLogger.Tracing.() -> Unit) {
            RootLogger.Tracing.config()
        }

        fun metering(config: RootLogger.Metering.() -> Unit) {
            RootLogger.Metering.config()
        }

        fun json(config: JsonBuilder.() -> Unit) {
            json = Json {
                // Apply default configuration.
                defaultWithErrors()
                // Apply custom configuration.
                config()
                serializersModule += defaultSerializersModule
            }
        }

        fun ktor(config: KtorApplication.() -> Unit) {
            this.other = config
        }

        internal fun configure() {
            app._json = json

            metering {
                appenders.register(app.metrics)
            }

            module.apply {
                // Register the application instance as a singleton.
                single { app }.bind<Application>()
                single { app.json }.bind<Json>()
                singleOf(::ApplicationStatusRestHandler) { bind<AbstractRestHandler>() }
                // Register the OpenAPI documentation endpoints (if enabled).
                if (app.conf.openApi.enabled) {
                    singleOf(::OpenApiRestHandler) { bind<AbstractRestHandler>() }
                }
            }

            // Start Koin.
            val di = startKoin { modules(module) }.apply {
                app._di = this
            }

            ktor.apply {
                // Install content negotiation for JSON serialization
                install(ContentNegotiation) {
                    json(json)
                }

                // Install status pages for default handler
                install(StatusPages) {
                    unhandled { call ->
                        val title: String = NotFound.TITLE
                        val res = ApiError(
                            type = ApiError.errorType(
                                includeTypePropertyInApiError = app.conf.includeTypePropertyInApiError,
                                errorTypeHost = app.conf.errorTypeHost,
                                title = title
                            ),
                            title = title,
                            status = HttpStatusCode.NotFound.value,
                            requestId = null,
                            detail = "The requested resource was not found.",
                            data = null
                        )
                        call.respond(HttpStatusCode.NotFound, res)
                    }
                }

                // Register routes.
                routing {
                    // Auto register discovered REST handlers.
                    di.getAll<AbstractRestHandler>().forEach {
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
        /**
         * A nullable instance holder for the [Application] class, which is used to track the currently
         * running application instance, if any.
         *
         * This variable is set when the application is started and cleared when the application shuts down.
         *
         * It is primarily used for accessing the `Application` instance to perform actions like shutdown
         * or manage lifecycle events. If no `Application` instance is running, this variable will be `null`.
         */
        var INSTANCE_OR_NULL: Application? = null

        /**
         * Provides a singleton instance of the `Application` class.
         *
         * This property is used to retrieve the globally accessible `Application` instance.
         * It ensures that the application is properly initialized before it can be accessed.
         * If the application has not been initialized, an exception is thrown.
         *
         * @throws IllegalStateException when the application has not been initialized.
         * To initialize the application, the `start()` method must be invoked first.
         */
        val INSTANCE: Application get() = INSTANCE_OR_NULL ?: error("Application not initialized. Run start() first.")

        /**
         * Provides the instance of the application's Dependency Injection container, powered by Koin.
         * This is used to manage application-wide dependencies, including services and components.
         *
         * The `di` property is initialized within the application's setup process and is used to
         * retrieve or configure dependencies for use throughout the application.
         *
         * @return The KoinApplication instance representing the Dependency Injection container.
         */
        val di: KoinApplication get() = INSTANCE.di

        /**
         * Represents a system-level user in the application, typically used for operations
         * or processes that are executed without a specific authenticated user context.
         *
         * This variable holds a default implementation of the `Principal` interface,
         * initialized with a system-defined user identity. It can be reassigned to represent
         * a different system user if required, such as for testing purposes or specific configurations.
         */
        var SYSTEM_USER: Principal = UserToken.DEFAULT_SYSTEM_USER

        /**
         * A globally accessible variable representing the default anonymous user context within the application.
         *
         * This variable holds a `Principal` instance intended for scenarios where user authentication
         * is not required or an anonymous context is enough. It is used as the default user
         * in various configurations, such as in REST handlers that operate without requiring user
         * authentication.
         *
         * By default, it is initialized with `UserToken.DEFAULT_ANONYMOUS_USER`.
         * However, it can be reassigned to a custom `Principal` instance to adapt the anonymous user behavior
         * according to specific application requirements.
         */
        var ANONYMOUS_USER: Principal = UserToken.DEFAULT_ANONYMOUS_USER
    }
}
