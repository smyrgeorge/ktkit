package io.github.smyrgeorge.ktorlib.util

import io.github.smyrgeorge.ktorlib.context.Context
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

    companion object {
        suspend inline fun <R> AbstractService.withTransaction(
            crossinline f: suspend context(Context, Transaction)() -> R
        ): R = db.transaction { with(ctx(), this) { f() } }

        suspend inline fun <R> AbstractService.withExecutionContext(
            crossinline f: suspend Context.() -> R
        ): R = ctx().f()

        suspend inline fun <A, R> with(a: A, f: suspend context(A)() -> R): R = f(a)
        suspend inline fun <A, B, R> with(a: A, b: B, f: suspend context(A, B)() -> R): R = f(a, b)
        suspend inline fun <A, B, C, R> with(a: A, b: B, c: C, f: suspend context(A, B, C)() -> R): R = f(a, b, c)
    }
}
