package io.github.smyrgeorge.ktorlib.service

import arrow.core.Either
import io.github.smyrgeorge.ktorlib.context.Context
import io.github.smyrgeorge.ktorlib.service.AbstractService.Companion.with
import io.github.smyrgeorge.ktorlib.util.AbstractComponent
import io.github.smyrgeorge.sqlx4k.Driver
import io.github.smyrgeorge.sqlx4k.Transaction

/**
 * Represents an abstract service within the application.
 *
 * This interface extends the capabilities of [io.github.smyrgeorge.ktorlib.util.AbstractComponent], allowing
 * implementing classes to inherit common utilities including logging, I/O operations,
 * retry mechanisms, and context extraction from the coroutines context.
 *
 * Use this interface to define services that conform to a unified structure with
 * shared functionality at the component level.
 */
interface AbstractDbService : AbstractComponent {
    val db: Driver

    companion object {
        suspend fun <R> AbstractDbService.withTransaction(f: suspend context(Context, Transaction)() -> R): R {
            return db.transaction {
                val result = with(ctx(), this) { f() }
                @Suppress("UNCHECKED_CAST")
                when (result) {
                    is Result<*> -> result.getOrThrow() as R
                    is Either<*, *> -> result.fold(
                        ifLeft = { throw (it as? Throwable ?: IllegalStateException(it.toString())) },
                        ifRight = { value -> value as R }
                    )

                    else -> result
                }
            }
        }
    }
}