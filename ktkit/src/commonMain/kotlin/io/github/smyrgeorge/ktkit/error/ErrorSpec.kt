package io.github.smyrgeorge.ktkit.error

import io.github.smyrgeorge.ktkit.error.system.UnknownError
import kotlinx.serialization.Serializable

/**
 * Represents a specification for errors used within the system.
 *
 * This interface provides a structured format to define error-related details,
 * including categorization (`kind`), human-readable message (`message`),
 * and associated HTTP status (`httpStatus`). It also offers utility methods
 * to handle errors as throwable exceptions.
 *
 * @property message A human-readable description of the error.
 * @property httpStatus The HTTP status code associated with this error.
 */
interface ErrorSpec {
    val message: String
    val httpStatus: HttpStatus

    /**
     * Throws this error as an [io.github.smyrgeorge.ktkit.error.RuntimeError] exception.
     *
     * @param throwable Optional cause of the error
     * @throws io.github.smyrgeorge.ktkit.error.RuntimeError Always throws
     */
    fun ex(throwable: Throwable? = null): Nothing =
        throw RuntimeError(this, message, throwable)

    /**
     * Converts this error to an [RuntimeError] throwable without throwing it.
     *
     * @param throwable Optional cause of the error
     * @return InternalError wrapping this error
     */
    fun toThrowable(throwable: Throwable? = null): RuntimeError =
        RuntimeError(this, message, throwable)

    /**
     * HTTP status codes supported by the error system.
     *
     * @property code HTTP status code number
     * @property phrase Human-readable description of the status
     */
    @Serializable
    enum class HttpStatus(
        val code: Int,
        val phrase: String
    ) {
        // --- 4xx Client Error ---
        BAD_REQUEST(400, "Bad Request"),
        UNAUTHORIZED(401, "Unauthorized"),
        FORBIDDEN(403, "Forbidden"),
        NOT_FOUND(404, "Not Found"),

        // --- 5xx Server Error ---
        INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    }

    companion object {
        fun fromThrowable(throwable: Throwable): ErrorSpec =
            UnknownError(throwable.message ?: "An unknown error occurred")
    }
}
