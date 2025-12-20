package io.github.smyrgeorge.ktorlib.api.rest

import io.github.smyrgeorge.ktorlib.error.ApiError
import io.github.smyrgeorge.ktorlib.error.ErrorDetails
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
     * Configures the StatusPages plugin to handle exceptions.
     *
     * Usage in your Application.module():
     * ```
     * fun Application.module() {
     *     install(StatusPages) {
     *         ExceptionHandler.configure(this)
     *     }
     * }
     * ```
     */
    fun configure(config: StatusPagesConfig) {
        with(config) {
            // Handle InternalError exceptions
            exception<InternalError> { call, cause ->
                handleInternalError(call, cause)
            }

            // Handle all other exceptions
            exception<Throwable> { call, cause ->
                handleUnknownError(call, cause)
            }
        }
    }

    /**
     * Handles InternalError exceptions by converting them to structured API errors.
     */
    private suspend fun handleInternalError(call: ApplicationCall, cause: InternalError) {
        val error = cause.error
        val requestId = call.request.headers["X-Request-ID"]

        log.error("InternalError: ${error.type} - ${error.message}", cause)

        val apiError = ApiError(
            code = error.type,
            requestId = requestId,
            details = ErrorDetails(
                type = error.type,
                message = error.message,
                http = error.http
            )
        )

        val httpStatusCode = HttpStatusCode.fromValue(error.http.code)
        call.respond(httpStatusCode, apiError)
    }

    /**
     * Handles unknown exceptions by wrapping them in a generic error.
     */
    private suspend fun handleUnknownError(call: ApplicationCall, cause: Throwable) {
        val requestId = call.request.headers["X-Request-ID"]

        log.error("Unknown error occurred", cause)

        val error = UnknownError(cause.message ?: "An unknown error occurred")
        val apiError = ApiError(
            code = error.type,
            requestId = requestId,
            details = ErrorDetails(
                type = error.type,
                message = error.message,
                http = error.http
            )
        )

        call.respond(HttpStatusCode.InternalServerError, apiError)
    }
}
