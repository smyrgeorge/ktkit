package io.github.smyrgeorge.ktkit.error.system

import io.github.smyrgeorge.ktkit.error.ErrorSpec

/**
 * Abstract base class for all errors in the system.
 *
 * Provides a structured way to handle errors with HTTP status codes and type information.
 *
 * @property message Error message describing what went wrong
 * @property httpStatus HTTP status associated with this error
 */
sealed interface SystemError : ErrorSpec {
    override val message: String
    override val httpStatus: ErrorSpec.HttpStatus
}
