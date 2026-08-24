package io.github.smyrgeorge.ktkit.api.error.impl

import io.github.smyrgeorge.ktkit.api.error.ErrorSpec
import io.github.smyrgeorge.ktkit.api.error.ErrorSpecData
import io.github.smyrgeorge.ktkit.api.error.impl.details.MalformedParameterErrorData

data class MalformedParameter(
    val kind: String,
    val name: String,
    val expected: String,
    val value: String,
) : SystemError {
    override val message: String = "Malformed parameter '$name' of type '$kind': '$value' is not a valid $expected"
    override val httpStatus: ErrorSpec.HttpStatus = ErrorSpec.HttpStatus.BAD_REQUEST
    override fun data(): ErrorSpecData = MalformedParameterErrorData(kind, name, expected, value)
}
