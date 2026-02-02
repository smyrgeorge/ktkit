package io.github.smyrgeorge.ktkit.api.error.impl

import io.github.smyrgeorge.ktkit.api.error.ErrorSpec
import io.github.smyrgeorge.ktkit.api.error.ErrorSpecData
import io.github.smyrgeorge.ktkit.api.error.impl.details.UnsupportedEnumValueErrorData

data class UnsupportedEnumValue(
    val kind: String,
    val value: String,
) : SystemError {
    override val message: String = "Unsupported enum value '$value' for type '$kind'"
    override val httpStatus: ErrorSpec.HttpStatus = ErrorSpec.HttpStatus.BAD_REQUEST
    override fun data(): ErrorSpecData = UnsupportedEnumValueErrorData(kind, value)
}
