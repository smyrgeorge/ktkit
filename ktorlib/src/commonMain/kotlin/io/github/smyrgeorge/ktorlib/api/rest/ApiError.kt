package io.github.smyrgeorge.ktorlib.api.rest

import io.github.smyrgeorge.ktorlib.error.ErrorSpec
import kotlinx.serialization.Serializable

/**
 * Structured error response for API endpoints.
 *
 * This class is used to serialize errors in a consistent format for API responses.
 *
 * @property type Error code (typically the error type)
 * @property status HTTP status code associated with the error
 * @property requestId Request ID for tracing
 * @property detail The detailed error information
 *
 * Check here:
 * https://www.rfc-editor.org/rfc/rfc9457.html
 */
@Serializable
data class ApiError(
    val type: String,
    val status: Int,
    val detail: String,
    // Extensions:
    val requestId: String,
    val error: ErrorSpec
)
