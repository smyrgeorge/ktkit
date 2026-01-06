package io.github.smyrgeorge.ktkit.error.system

import io.github.smyrgeorge.ktkit.error.ErrorSpec
import io.github.smyrgeorge.ktkit.error.ErrorSpecData
import io.github.smyrgeorge.ktkit.error.system.details.MissingParameterErrorData

data class MissingParameter(
    val kind: String,
    val name: String,
    override val message: String = "Missing required parameter '$name' of type '$kind'",
    override val httpStatus: ErrorSpec.HttpStatus = ErrorSpec.HttpStatus.BAD_REQUEST,
) : SystemError {
    override fun toErrorSpecData(): ErrorSpecData = MissingParameterErrorData(kind, name)
}
