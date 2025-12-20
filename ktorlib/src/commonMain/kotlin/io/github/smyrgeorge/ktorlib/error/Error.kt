package io.github.smyrgeorge.ktorlib.error

import kotlinx.serialization.Serializable

/**
 * Abstract base class for all errors in the system.
 *
 * Provides a structured way to handle errors with HTTP status codes and type information.
 *
 * @property message Error message describing what went wrong
 * @property httpStatus HTTP status associated with this error
 */
abstract class Error(
    open val message: String,
    httpStatus: HttpStatus,
) {
    val type: String = this::class.simpleName ?: error("Sanity check failed.")
    val http: HttpError = HttpError(httpStatus.name, httpStatus.code)

    /**
     * Throws this error as an [InternalError] exception.
     *
     * @param throwable Optional cause of the error
     * @throws InternalError Always throws
     */
    fun ex(throwable: Throwable? = null): Nothing =
        throw InternalError(this, message, throwable)

    /**
     * Converts this error to an [InternalError] throwable without throwing it.
     *
     * @param throwable Optional cause of the error
     * @return InternalError wrapping this error
     */
    fun toThrowable(throwable: Throwable? = null): InternalError =
        InternalError(this, message, throwable)

    /**
     * Converts this error to a failed [Result].
     *
     * @return Failed Result containing the error
     */
    inline fun <reified T> toResult(): Result<T> =
        Result.failure(toThrowable())

    /**
     * Represents the HTTP error information.
     *
     * @property status HTTP status name (e.g., "BAD_REQUEST")
     * @property code HTTP status code (e.g., 400)
     */
    @Serializable
    data class HttpError(val status: String, val code: Int)

    /**
     * HTTP status codes supported by the error system.
     *
     * @property code HTTP status code number
     * @property reasonPhrase Human-readable description of the status
     */
    enum class HttpStatus(
        val code: Int,
        @Suppress("unused")
        private val reasonPhrase: String
    ) {
        // --- 4xx Client Error ---
        BAD_REQUEST(400, "Bad Request"),
        UNAUTHORIZED(401, "Unauthorized"),
        FORBIDDEN(403, "Forbidden"),
        NOT_FOUND(404, "Not Found"),
        REQUEST_TIMEOUT(408, "Request timeout"),
        CONFLICT(409, "Conflict"),
        GONE(410, "Gone"),

        // --- 5xx Server Error ---
        INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
        BAD_GATEWAY(502, "Bad gateway"),
        SERVICE_UNAVAILABLE(503, "Service Unavailable"),
    }
}
