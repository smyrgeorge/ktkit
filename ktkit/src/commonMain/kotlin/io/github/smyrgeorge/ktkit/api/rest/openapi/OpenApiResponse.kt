package io.github.smyrgeorge.ktkit.api.rest.openapi

/**
 * Documentation of one response, used inside [OpenApi.responses] — adds the response to the
 * operation (with the standard `ApiError` schema for error codes) or overrides the description
 * of an inferred one.
 *
 * @property code The HTTP status code.
 * @property description The response description. Empty = the standard phrase of the code.
 */
@Retention(AnnotationRetention.SOURCE)
annotation class OpenApiResponse(
    val code: Int,
    val description: String = "",
)
