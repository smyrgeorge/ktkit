package io.github.smyrgeorge.ktkit.api.error.impl

import io.github.smyrgeorge.ktkit.api.error.ErrorSpec
import io.github.smyrgeorge.ktkit.api.error.ErrorSpecData
import io.github.smyrgeorge.ktkit.api.error.impl.details.MissingParameterErrorData

data class MissingParameter(
    val kind: String,
    val name: String,
) : SystemError {
    override val message: String = "Missing required parameter '$name' of type '$kind'"
    override val httpStatus: ErrorSpec.HttpStatus = ErrorSpec.HttpStatus.BAD_REQUEST
    override fun data(): ErrorSpecData = MissingParameterErrorData(kind, name)
}
