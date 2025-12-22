package io.github.smyrgeorge.ktorlib.util

import io.github.smyrgeorge.sqlx4k.Driver
import io.github.smyrgeorge.sqlx4k.Transaction

/**
 * Represents an abstract service within the application.
 *
 * This interface extends the capabilities of [AbstractComponent], allowing
 * implementing classes to inherit common utilities including logging, I/O operations,
 * retry mechanisms, and context extraction from the coroutines context.
 *
 * Use this interface to define services that conform to a unified structure with
 * shared functionality at the component level.
 */
interface AbstractService : AbstractComponent {
    val db: Driver

    suspend fun <R> withTransaction(f: suspend Transaction.() -> R): R =
        db.transaction { f() }

    suspend fun <A, R> with(a: A, f: suspend context(A)() -> R): R = f(a)
    suspend fun <A, B, R> with(a: A, b: B, f: suspend context(A, B)() -> R): R = f(a, b)
    suspend fun <A, B, C, R> with(a: A, b: B, c: C, f: suspend context(A, B, C)() -> R): R = f(a, b, c)
}
