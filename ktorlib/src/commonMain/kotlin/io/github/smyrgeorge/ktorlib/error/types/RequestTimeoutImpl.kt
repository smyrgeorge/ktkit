package io.github.smyrgeorge.ktorlib.error.types

data class RequestTimeoutImpl(override val message: String) : RequestTimeout(message)
