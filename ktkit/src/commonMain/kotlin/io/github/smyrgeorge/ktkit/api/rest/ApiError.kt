package io.github.smyrgeorge.ktkit.api.rest

import io.github.smyrgeorge.ktkit.error.ErrorSpecData
import kotlinx.serialization.Serializable

/**
 * Structured error response for API endpoints.
 *
 * This class is used to serialize errors in a consistent format for API responses.
 *
 * @property title Error code (typically the error type)
 * @property status HTTP status code associated with the error
 * @property detail The detailed error information
 * @property requestId Request ID for tracing
 * @property data Additional error deta
 *
 * Check here:
 * https://www.rfc-editor.org/rfc/rfc9457.html
 */
@Serializable
data class ApiError(
    val type: String?,
    val title: String,
    val status: Int,
    val detail: String,
    // Extensions:
    val requestId: String,
    val data: ErrorSpecData?
)
