package io.github.smyrgeorge.ktkit.api.error.impl

import io.github.smyrgeorge.ktkit.api.error.ErrorSpec

data class NotFound(
    override val message: String,
) : SystemError {
    override val httpStatus: ErrorSpec.HttpStatus = ErrorSpec.HttpStatus.NOT_FOUND

    internal companion object {
        internal const val TITLE = "NotFound"
    }
}

