package io.github.smyrgeorge.ktkit.api.error

/**
 * Exception wrapper for [io.github.smyrgeorge.ktkit.api.error.impl.SystemError] instances.
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
