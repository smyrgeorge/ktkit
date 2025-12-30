package io.github.smyrgeorge.ktorlib.error.system

import io.github.smyrgeorge.ktorlib.error.ErrorSpec
import kotlinx.serialization.Serializable

@Serializable
data class UnknownError(
    override val message: String,
    override val httpStatus: ErrorSpec.HttpStatus = ErrorSpec.HttpStatus.INTERNAL_SERVER_ERROR,
) : SystemError