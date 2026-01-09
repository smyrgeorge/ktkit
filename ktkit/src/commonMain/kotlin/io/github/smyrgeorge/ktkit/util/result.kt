package io.github.smyrgeorge.ktkit.util

import arrow.core.left
import arrow.core.right

inline fun <T> Result<T>.mapError(transform: (exception: Throwable) -> Throwable): Result<T> {
    return when (val exception = exceptionOrNull()) {
        null -> this
        else -> Result.failure(transform(exception))
    }
}

fun <T> Result<T>.toEither(): EitherThrowable<T> = fold(
    onSuccess = { it.right() },
    onFailure = { it.left() }
)
