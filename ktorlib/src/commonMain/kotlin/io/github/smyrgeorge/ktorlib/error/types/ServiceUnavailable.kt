package io.github.smyrgeorge.ktorlib.error.types

import io.github.smyrgeorge.ktorlib.error.Error

abstract class ServiceUnavailable(override val message: String) : Error(message, HttpStatus.SERVICE_UNAVAILABLE)