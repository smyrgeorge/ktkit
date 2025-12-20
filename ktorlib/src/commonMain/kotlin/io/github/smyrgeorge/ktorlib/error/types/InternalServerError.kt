package io.github.smyrgeorge.ktorlib.error.types

import io.github.smyrgeorge.ktorlib.error.Error

abstract class InternalServerError(message: String) : Error(message, HttpStatus.INTERNAL_SERVER_ERROR)
