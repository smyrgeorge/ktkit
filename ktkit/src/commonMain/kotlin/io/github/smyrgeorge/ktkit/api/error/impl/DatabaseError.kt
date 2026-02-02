package io.github.smyrgeorge.ktkit.api.error.impl

import io.github.smyrgeorge.ktkit.api.error.ErrorSpec
import io.github.smyrgeorge.ktkit.api.error.ErrorSpecData
import io.github.smyrgeorge.ktkit.api.error.impl.details.DatabaseErrorData

data class DatabaseError(
    val code: String,
    val cause: Throwable? = null,
    override val message: String,
    override val httpStatus: ErrorSpec.HttpStatus = ErrorSpec.HttpStatus.INTERNAL_SERVER_ERROR,
) : SystemError {
    override fun toErrorSpecData(): ErrorSpecData = DatabaseErrorData(code)
}
