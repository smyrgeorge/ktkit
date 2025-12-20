package io.github.smyrgeorge.ktorlib.error.types

data class ServiceUnavailableImpl(override val message: String) : ServiceUnavailable(message)