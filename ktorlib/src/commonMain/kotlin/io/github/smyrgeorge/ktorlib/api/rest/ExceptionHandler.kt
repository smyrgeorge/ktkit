package io.github.smyrgeorge.ktorlib.api.rest

import io.github.smyrgeorge.ktorlib.error.ApiError
import io.github.smyrgeorge.ktorlib.error.InternalError
import io.github.smyrgeorge.ktorlib.error.types.UnknownError
import io.github.smyrgeorge.log4k.Logger
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

/**
 * Exception handler for converting structured errors to HTTP responses.
 *
 * This handler catches [InternalError] exceptions and converts them to structured
 * [ApiError] responses with appropriate HTTP status codes.
 */
object ExceptionHandler {
    private val log = Logger.of(this::class)

    /**
     * Configures exception handling for the provided [StatusPagesConfig] instance.
     * Sets up handlers for specific exception types and ensures structured error responses.
     *
     * @param config The [StatusPagesConfig] instance to configure with exception handlers.
     */
    private fun configure(config: StatusPagesConfig) {
        with(config) {
            // Handle InternalError exceptions
            exception<InternalError> { call, cause -> handleInternalError(call, cause) }
            // Handle all other exceptions
            exception<Throwable> { call, cause -> handleUnknownError(call, cause) }
        }
    }

    /**
     * Handles internal server errors by logging the error and responding with a structured API error response.
     *
     * @param call The current application call, used to extract headers and send a response.
     * @param cause The internal error that triggered this handler, containing structured error information.
     */
    private suspend fun handleInternalError(call: ApplicationCall, cause: InternalError) {
        val error = cause.error
        val requestId = call.request.headers["X-Request-ID"]

        // Only log server errors (5xx)
        if (error.http.code >= 500) {
            log.error("InternalError: ${error.type} - ${error.message}", cause)
        }

        val apiError = ApiError(
            code = error.type,
            requestId = requestId,
            details = ApiError.Details(
                type = error.type,
                message = error.message,
                http = error.http
            )
        )

        val httpStatusCode = HttpStatusCode.fromValue(error.http.code)
        call.respond(httpStatusCode, apiError)
    }

    /**
     * Handles unknown errors by logging the error, creating a structured API error response,
     * and sending an HTTP 500 Internal Server Error to the client.
     *
     * @param call The [ApplicationCall] instance representing the HTTP request.
     * @param cause The [Throwable] representing the unexpected error that occurred.
     */
    private suspend fun handleUnknownError(call: ApplicationCall, cause: Throwable) {
        val requestId = call.request.headers["X-Request-ID"]

        log.error("Unknown error occurred", cause)

        val error = UnknownError(cause.message ?: "An unknown error occurred")
        val apiError = ApiError(
            code = error.type,
            requestId = requestId,
            details = ApiError.Details(
                type = error.type,
                message = error.message,
                http = error.http
            )
        )

        call.respond(HttpStatusCode.InternalServerError, apiError)
    }

    /**
     * Extension function to easily install exception handling.
     *
     * Usage:
     * ```
     * fun Application.module() {
     *     installExceptionHandling()
     *     // ... other configuration
     * }
     * ```
     */
    fun Application.installExceptionHandling() {
        install(StatusPages) {
            configure(this)
        }
    }
}
