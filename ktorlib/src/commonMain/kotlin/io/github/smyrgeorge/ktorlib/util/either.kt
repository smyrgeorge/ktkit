package io.github.smyrgeorge.ktorlib.util

import arrow.core.Either
import io.github.smyrgeorge.ktorlib.error.ErrorSpec

typealias MyResult<T> = Either<ErrorSpec, T>
typealias EitherThrowable<T> = Either<Throwable, T>
