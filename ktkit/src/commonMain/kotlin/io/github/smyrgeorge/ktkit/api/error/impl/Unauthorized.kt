package io.github.smyrgeorge.ktkit.api.error.impl

import io.github.smyrgeorge.ktkit.api.error.ErrorSpec
import io.github.smyrgeorge.ktkit.api.error.ErrorSpecData
import io.github.smyrgeorge.ktkit.api.error.impl.details.EmptyErrorData

data class Unauthorized(
    override val message: String,
    override val httpStatus: ErrorSpec.HttpStatus = ErrorSpec.HttpStatus.UNAUTHORIZED,
) : SystemError {
    override fun toErrorSpecData(): ErrorSpecData = EmptyErrorData
}
