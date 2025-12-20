package io.github.smyrgeorge.ktorlib.error

import kotlinx.serialization.Serializable

/**
 * Structured error response for API endpoints.
 *
 * This class is used to serialize errors in a consistent format for API responses.
 *
 * @property code Error code (typically the error type)
 * @property requestId Optional request ID for tracing
 * @property details The detailed error information
 */
@Serializable
data class ApiError(
    val code: String,
    val requestId: String? = null,
    val details: Details
) {
    /**
     * Detailed error information for API responses.
     *
     * @property type Error type (class name)
     * @property message Error message
     * @property http HTTP error information
     */
    @Serializable
    data class Details(
        val type: String,
        val message: String,
        val http: Error.HttpError
    )
}
