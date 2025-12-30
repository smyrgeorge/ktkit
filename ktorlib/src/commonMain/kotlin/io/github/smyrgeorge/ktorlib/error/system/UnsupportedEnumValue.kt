package io.github.smyrgeorge.ktorlib.error.system

import io.github.smyrgeorge.ktorlib.error.ErrorSpec
import kotlinx.serialization.Serializable

@Serializable
data class UnsupportedEnumValue(
    val type: String,
    val value: String,
    override val message: String = "Unsupported enum value '$value' for type '$type'",
    override val httpStatus: ErrorSpec.HttpStatus = ErrorSpec.HttpStatus.BAD_REQUEST,
) : SystemError
