package io.github.smyrgeorge.ktkit.api.error.impl

import io.github.smyrgeorge.ktkit.api.error.ErrorSpec

data class MalformedRequestBody(
    val cause: Throwable,
) : SystemError {
    override val message: String = "Could not parse request body: ${cause.message}"
    override val httpStatus: ErrorSpec.HttpStatus = ErrorSpec.HttpStatus.BAD_REQUEST
}
