package io.github.smyrgeorge.ktkit.error.system

import io.github.smyrgeorge.ktkit.error.ErrorSpec
import kotlinx.serialization.Serializable

@Serializable
data class UnknownError(
    override val message: String,
    override val httpStatus: ErrorSpec.HttpStatus = ErrorSpec.HttpStatus.INTERNAL_SERVER_ERROR,
) : SystemError