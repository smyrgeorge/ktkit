package io.github.smyrgeorge.ktkit.util

import arrow.core.Either

typealias EitherThrowable<T> = Either<Throwable, T>

fun <T> EitherThrowable<T>.toResult(): Result<T> =
    fold(
        ifLeft = { Result.failure(it) },
        ifRight = { Result.success(it) },
    )
