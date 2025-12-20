package io.github.smyrgeorge.ktorlib.error

/**
 * Exception wrapper for [Error] instances.
 *
 * Used to convert structured errors into throwable exceptions.
 *
 * @property error The structured error being wrapped
 */
class InternalError(
    val error: Error,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)