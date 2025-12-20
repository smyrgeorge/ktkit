package io.github.smyrgeorge.ktorlib.error.types

import io.github.smyrgeorge.ktorlib.error.Error

abstract class Unauthorized(message: String) : Error(message, HttpStatus.UNAUTHORIZED)
