package io.github.smyrgeorge.ktkit.api.error.impl

import io.github.smyrgeorge.ktkit.api.error.ErrorSpec

data class Unauthorized(
    override val message: String,
) : SystemError {
    override val httpStatus: ErrorSpec.HttpStatus = ErrorSpec.HttpStatus.UNAUTHORIZED
}
