package io.github.smyrgeorge.ktorlib.error.types

import io.github.smyrgeorge.ktorlib.error.Error

/**
 * Abstract base class for 403 Forbidden errors.
 */
abstract class Forbidden(message: String) : Error(message, HttpStatus.FORBIDDEN)

/**
 * Concrete implementation of a Forbidden error.
 */
class ForbiddenImpl(message: String) : Forbidden(message)
