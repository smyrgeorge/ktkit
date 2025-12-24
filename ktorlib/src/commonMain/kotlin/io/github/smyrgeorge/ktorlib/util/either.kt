package io.github.smyrgeorge.ktorlib.util

import arrow.core.Either
import arrow.core.left
import arrow.core.right

/**
 * Converts a [Result] to an [Either].
 * - Success values become [Either.Right]
 * - Failure exceptions become [Either.Left]
 */
fun <T> Result<T>.toEither(): Either<Throwable, T> =
    fold(
        onSuccess = { it.right() },
        onFailure = { it.left() }
    )

/**
 * Converts a [Result] to an [Either] with custom error mapping.
 * - Success values become [Either.Right]
 * - Failure exceptions are mapped to [Either.Left] using the provided mapper
 */
fun <T, E> Result<T>.toEither(mapError: (Throwable) -> E): Either<E, T> =
    fold(
        onSuccess = { it.right() },
        onFailure = { mapError(it).left() }
    )

/**
 * Converts an [Either] to a [Result].
 * - [Either.Right] values become [Result.success]
 * - [Either.Left] values become [Result.failure] (requires left type to be Throwable)
 */
fun <T> Either<Throwable, T>.toResult(): Result<T> =
    fold(
        ifLeft = { Result.failure(it) },
        ifRight = { Result.success(it) }
    )
