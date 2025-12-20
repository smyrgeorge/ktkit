package io.github.smyrgeorge.ktorlib.error.types

data class BadGatewayImpl(override val message: String) : BadGateway(message)
