package io.github.smyrgeorge.ktorlib.error

/**
 * Exception wrapper for [io.github.smyrgeorge.ktorlib.error.system.SystemError] instances.
 *
 * Used to convert structured errors into throwable exceptions.
 *
 * @property error The structured error being wrapped
 */
class RuntimeError(
    val error: ErrorSpec,
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
