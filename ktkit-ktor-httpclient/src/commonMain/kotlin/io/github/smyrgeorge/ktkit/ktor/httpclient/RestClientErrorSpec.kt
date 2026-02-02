package io.github.smyrgeorge.ktkit.ktor.httpclient

import io.github.smyrgeorge.ktkit.api.error.ErrorSpec

interface RestClientErrorSpec : ErrorSpec {
    data class TransportError(
        override val message: String,
        override val httpStatus: ErrorSpec.HttpStatus = ErrorSpec.HttpStatus.INTERNAL_SERVER_ERROR,
    ) : RestClientErrorSpec

    data class DeserializationError(
        override val message: String,
        override val httpStatus: ErrorSpec.HttpStatus = ErrorSpec.HttpStatus.INTERNAL_SERVER_ERROR,
    ) : RestClientErrorSpec
}
