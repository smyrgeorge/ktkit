package io.github.smyrgeorge.ktkit.ktor.httpclient

import arrow.core.raise.RaiseDSL
import arrow.core.raise.context.Raise
import io.github.smyrgeorge.ktkit.api.error.ErrorSpec

interface RestClientErrorSpec : ErrorSpec {

    data class RequestError(
        val cause: Throwable,
        override val message: String = "Could not fulfill the request (${cause::class.simpleName}): ${cause.message}",
        override val httpStatus: ErrorSpec.HttpStatus = ErrorSpec.HttpStatus.INTERNAL_SERVER_ERROR,
    ) : RestClientErrorSpec

    data class DeserializationError(
        val cause: Throwable,
        override val message: String = "Could not extract response body (${cause::class.simpleName}): ${cause.message}",
        override val httpStatus: ErrorSpec.HttpStatus = ErrorSpec.HttpStatus.INTERNAL_SERVER_ERROR,
    ) : RestClientErrorSpec

    @Suppress("NOTHING_TO_INLINE")
    companion object {
        @RaiseDSL
        context(rc: Raise<RestClientErrorSpec>)
        inline fun RestClientErrorSpec.raise(): Nothing = rc.raise(this)
    }
}
