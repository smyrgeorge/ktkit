package io.github.smyrgeorge.ktorlib.error.types

import io.github.smyrgeorge.ktorlib.error.Error

abstract class Forbidden(message: String) : Error(message, HttpStatus.FORBIDDEN)
