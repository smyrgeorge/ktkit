package io.github.smyrgeorge.ktkit.error.system

import io.github.smyrgeorge.ktkit.error.ErrorSpec
import io.github.smyrgeorge.ktkit.error.ErrorSpecData
import io.github.smyrgeorge.ktkit.error.system.details.EmptyErrorData

data class UnknownError(
    override val message: String,
    override val httpStatus: ErrorSpec.HttpStatus = ErrorSpec.HttpStatus.INTERNAL_SERVER_ERROR,
) : SystemError {
    override fun toErrorSpecData(): ErrorSpecData = EmptyErrorData
}
