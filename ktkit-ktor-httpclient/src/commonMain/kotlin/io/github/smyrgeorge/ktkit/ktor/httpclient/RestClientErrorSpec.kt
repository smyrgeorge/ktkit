package io.github.smyrgeorge.ktkit.ktor.httpclient

import arrow.core.raise.RaiseDSL
import arrow.core.raise.context.Raise
import io.github.smyrgeorge.ktkit.api.error.ErrorSpec
import io.github.smyrgeorge.ktkit.api.error.ErrorSpecData
import io.github.smyrgeorge.ktkit.api.error.impl.details.EmptyErrorData
import io.github.smyrgeorge.ktkit.api.rest.ApiError

interface RestClientErrorSpec : ErrorSpec {
    data class RestClientReceiveError(
        val cause: ApiError,
    ) : RestClientErrorSpec {
        override val message: String = "Could not fulfill the request (${cause::class.simpleName}): ${cause.detail}"
        override val httpStatus: ErrorSpec.HttpStatus = ErrorSpec.HttpStatus.fromCode(cause.status)
        override fun data(): ErrorSpecData = cause.data ?: EmptyErrorData
    }

    data class RestClientRequestError(
        val cause: Throwable,
    ) : RestClientErrorSpec {
        override val message: String = "Could not fulfill the request (${cause::class.simpleName}): ${cause.message}"
        override val httpStatus: ErrorSpec.HttpStatus = ErrorSpec.HttpStatus.INTERNAL_SERVER_ERROR
    }

    data class RestClientDeserializationError(
        val cause: Throwable,
    ) : RestClientErrorSpec {
        override val message: String = "Could not extract response body (${cause::class.simpleName}): ${cause.message}"
        override val httpStatus: ErrorSpec.HttpStatus = ErrorSpec.HttpStatus.INTERNAL_SERVER_ERROR
    }

    @Suppress("NOTHING_TO_INLINE")
    companion object {
        @RaiseDSL
        context(rc: Raise<RestClientErrorSpec>)
        inline fun RestClientErrorSpec.raise(): Nothing = rc.raise(this)
    }
}
