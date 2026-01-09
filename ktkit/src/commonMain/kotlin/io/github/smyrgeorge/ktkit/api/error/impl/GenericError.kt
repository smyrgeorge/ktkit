package io.github.smyrgeorge.ktkit.api.error.impl

import io.github.smyrgeorge.ktkit.api.error.ErrorSpec
import io.github.smyrgeorge.ktkit.api.error.ErrorSpecData
import io.github.smyrgeorge.ktkit.api.error.impl.details.EmptyErrorData

data class GenericError(
    override val message: String,
    val data: ErrorSpecData = EmptyErrorData,
    override val httpStatus: ErrorSpec.HttpStatus = ErrorSpec.HttpStatus.INTERNAL_SERVER_ERROR,
) : SystemError {
    override fun toErrorSpecData(): ErrorSpecData = data
}
