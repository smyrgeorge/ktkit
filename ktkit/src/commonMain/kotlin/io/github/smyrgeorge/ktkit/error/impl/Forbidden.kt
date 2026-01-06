package io.github.smyrgeorge.ktkit.error.impl

import io.github.smyrgeorge.ktkit.error.ErrorSpec
import io.github.smyrgeorge.ktkit.error.ErrorSpecData
import io.github.smyrgeorge.ktkit.error.impl.details.EmptyErrorData

data class Forbidden(
    override val message: String,
    override val httpStatus: ErrorSpec.HttpStatus = ErrorSpec.HttpStatus.FORBIDDEN,
) : SystemError {
    override fun toErrorSpecData(): ErrorSpecData = EmptyErrorData
}

