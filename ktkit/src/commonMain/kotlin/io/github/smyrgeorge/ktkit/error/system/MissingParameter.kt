package io.github.smyrgeorge.ktkit.error.system

import io.github.smyrgeorge.ktkit.error.ErrorSpec
import kotlinx.serialization.Serializable

@Serializable
data class MissingParameter(
    val kind: String,
    val name: String,
    override val message: String = "Missing required parameter '$name' of type '$kind'",
    override val httpStatus: ErrorSpec.HttpStatus = ErrorSpec.HttpStatus.BAD_REQUEST,
) : SystemError
