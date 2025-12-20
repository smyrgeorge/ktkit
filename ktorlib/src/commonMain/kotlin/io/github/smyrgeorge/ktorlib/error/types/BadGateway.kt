package io.github.smyrgeorge.ktorlib.error.types

import io.github.smyrgeorge.ktorlib.error.Error

abstract class BadGateway(override val message: String) : Error(message, HttpStatus.BAD_GATEWAY)
