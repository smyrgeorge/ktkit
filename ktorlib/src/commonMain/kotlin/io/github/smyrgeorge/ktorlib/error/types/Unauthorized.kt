package io.github.smyrgeorge.ktorlib.error.types

import io.github.smyrgeorge.ktorlib.error.Error

/**
 * Abstract base class for 401 Unauthorized errors.
 */
abstract class Unauthorized(message: String) : Error(message, HttpStatus.UNAUTHORIZED)

/**
 * Concrete implementation of an Unauthorized error.
 */
class UnauthorizedImpl(message: String) : Unauthorized(message)
