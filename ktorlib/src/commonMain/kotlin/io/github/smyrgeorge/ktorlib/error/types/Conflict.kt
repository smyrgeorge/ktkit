package io.github.smyrgeorge.ktorlib.error.types

import io.github.smyrgeorge.ktorlib.error.Error

abstract class Conflict(override val message: String) : Error(message, HttpStatus.CONFLICT)