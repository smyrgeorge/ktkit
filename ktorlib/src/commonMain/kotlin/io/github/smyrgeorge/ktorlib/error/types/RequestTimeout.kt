package io.github.smyrgeorge.ktorlib.error.types

import io.github.smyrgeorge.ktorlib.error.Error

abstract class RequestTimeout(override val message: String) : Error(message, HttpStatus.REQUEST_TIMEOUT)
