package io.github.smyrgeorge.ktkit.api.error.impl

import io.github.smyrgeorge.ktkit.api.error.ErrorSpec
import io.github.smyrgeorge.ktkit.api.error.ErrorSpecData
import io.github.smyrgeorge.ktkit.api.error.impl.details.EmptyErrorData

data class MalformedRequestBody(
    val cause: Throwable,
    override val message: String = "Could not parse request body: ${cause.message}",
    override val httpStatus: ErrorSpec.HttpStatus = ErrorSpec.HttpStatus.BAD_REQUEST,
) : SystemError {
    override fun toErrorSpecData(): ErrorSpecData = EmptyErrorData
}
