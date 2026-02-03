package io.github.smyrgeorge.ktkit.api.error.impl

import io.github.smyrgeorge.ktkit.api.error.ErrorSpec

data class DatabaseError(
    val code: String,
    val cause: Throwable? = null,
    override val message: String,
) : SystemError {
    override val httpStatus: ErrorSpec.HttpStatus = ErrorSpec.HttpStatus.INTERNAL_SERVER_ERROR
}
