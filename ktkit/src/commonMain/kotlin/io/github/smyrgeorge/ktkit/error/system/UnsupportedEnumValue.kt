package io.github.smyrgeorge.ktkit.error.system

import io.github.smyrgeorge.ktkit.error.ErrorSpec
import kotlinx.serialization.Serializable

@Serializable
data class UnsupportedEnumValue(
    val kind: String,
    val value: String,
    override val message: String = "Unsupported enum value '$value' for type '$kind'",
    override val httpStatus: ErrorSpec.HttpStatus = ErrorSpec.HttpStatus.BAD_REQUEST,
) : SystemError
