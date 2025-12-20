package io.github.smyrgeorge.ktorlib.error.types

import io.github.smyrgeorge.ktorlib.error.Error

/**
 * Abstract base class for 404 Not Found errors.
 */
abstract class NotFound(message: String) : Error(message, HttpStatus.NOT_FOUND)

/**
 * Concrete implementation of a Not Found error.
 */
class NotFoundImpl(message: String) : NotFound(message)
