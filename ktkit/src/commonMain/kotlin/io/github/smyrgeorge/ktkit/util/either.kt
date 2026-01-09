package io.github.smyrgeorge.ktkit.util

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.smyrgeorge.ktkit.api.error.ErrorSpec

typealias AppResult<T> = Either<ErrorSpec, T>
typealias EitherThrowable<T> = Either<Throwable, T>

fun <T> EitherThrowable<T>.toResult(): Result<T> = fold(
    ifLeft = { Result.failure(it) },
    ifRight = { Result.success(it) }
)
