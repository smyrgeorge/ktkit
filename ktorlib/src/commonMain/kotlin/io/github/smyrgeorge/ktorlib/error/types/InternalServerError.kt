package io.github.smyrgeorge.ktorlib.error.types

import io.github.smyrgeorge.ktorlib.error.Error

/**
 * Abstract base class for 500 Internal Server Error errors.
 */
abstract class InternalServerError(message: String) : Error(message, HttpStatus.INTERNAL_SERVER_ERROR)

/**
 * Concrete implementation of an Internal Server Error.
 */
class InternalServerErrorImpl(message: String) : InternalServerError(message)

/**
 * Error thrown when an unknown error occurs.
 */
class UnknownError(message: String) : InternalServerError(message)
