package io.github.smyrgeorge.ktorlib.error.types

data class ConflictImpl(override val message: String) : Conflict(message)