package io.github.smyrgeorge.ktorlib.error.types

import io.github.smyrgeorge.ktorlib.error.Error

abstract class NotFound(message: String) : Error(message, HttpStatus.NOT_FOUND)
