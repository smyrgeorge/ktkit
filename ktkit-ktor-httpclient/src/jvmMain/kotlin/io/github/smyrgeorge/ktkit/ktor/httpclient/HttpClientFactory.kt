package io.github.smyrgeorge.ktkit.ktor.httpclient

import io.github.smyrgeorge.ktkit.util.default
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.headers
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object HttpClientFactory {
    /**
     * Creates a configured instance of an [HttpClient] with specified connection settings, timeouts,
     * and JSON processing using kotlinx.serialization.
     *
     * @param json the JSON instance for encoding and decoding JSON data. If not provided, a default instance is used.
     * @param connectionTimeout the timeout duration for establishing connections, default is 2 seconds.
     * @param requestTimeout the timeout duration for the entire request, default is 10 seconds.
     * @param socketTimeout the timeout duration for reading data from sockets, default is 10 seconds.
     * @param maxConnections the maximum number of connections allowed in the connection pool, default is 128.
     * @param enableLogging whether to enable HTTP request/response logging, default is false.
     * @param logLevel the logging level for HTTP requests, default is LogLevel.INFO.
     * @return a configured instance of [HttpClient].
     */
    fun create(
        json: Json = Json { default() },
        connectionTimeout: Duration = 2.seconds,
        requestTimeout: Duration = 10.seconds,
        socketTimeout: Duration = 10.seconds,
        maxConnections: Int = 128,
        enableLogging: Boolean = false,
        logLevel: LogLevel = LogLevel.INFO,
    ): HttpClient = HttpClient(CIO) {
        // Engine configuration
        engine {
            maxConnectionsCount = maxConnections
        }

        // Install content negotiation for JSON with kotlinx.serialization
        install(ContentNegotiation) {
            json(json)
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
    }
}
