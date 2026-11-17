package io.github.smyrgeorge.ktkit.ktor.httpclient

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.CIOEngineConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.contentnegotiation.ContentNegotiationConfig
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object HttpClientFactory {
    /**
     * Creates a configured instance of an [HttpClient] with specified connection settings, timeouts,
     * and content negotiation.
     *
     * @param connectionTimeout the timeout duration for establishing connections, default is 2 seconds.
     * @param requestTimeout the timeout duration for the entire request, default is 10 seconds.
     * @param socketTimeout the timeout duration for reading data from sockets, default is 10 seconds.
     * @param maxConnections the maximum number of connections allowed in the connection pool, default is 128.
     * @param enableLogging whether to enable HTTP request/response logging, default is false.
     * @param logLevel the logging level for HTTP requests, the default is LogLevel.INFO.
     * @param negotiation registers the client's content converters and is the only codec installed; the default is
     *   kotlinx.serialization JSON with Ktor's default `Json`. Pass `{ json(myJson) }` for another `Json`, or another
     *   converter (e.g. `{ jackson() }`).
     * @param configure runs last on the client's configuration, to install further plugins or extend the ones above.
     * @return a configured instance of [HttpClient].
     */
    fun create(
        connectionTimeout: Duration = 2.seconds,
        requestTimeout: Duration = 10.seconds,
        socketTimeout: Duration = 10.seconds,
        maxConnections: Int = 128,
        enableLogging: Boolean = false,
        logLevel: LogLevel = LogLevel.INFO,
        negotiation: ContentNegotiationConfig.() -> Unit = { json() },
        configure: HttpClientConfig<CIOEngineConfig>.() -> Unit = {},
    ): HttpClient = HttpClient(CIO) {
        // Engine configuration
        engine {
            maxConnectionsCount = maxConnections
        }

        // Install content negotiation with the given codec
        install(ContentNegotiation) {
            negotiation()
        }

        // Configure timeouts
        install(HttpTimeout) {
            this.requestTimeoutMillis = requestTimeout.inWholeMilliseconds
            this.connectTimeoutMillis = connectionTimeout.inWholeMilliseconds
            this.socketTimeoutMillis = socketTimeout.inWholeMilliseconds
        }

        // Optional logging
        if (enableLogging) {
            install(Logging) {
                level = logLevel
            }
        }

        // Default request configuration
        defaultRequest {
            headers {
                append(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }
        }

        // Configure expected success status codes
        expectSuccess = false

        configure()
    }
}
