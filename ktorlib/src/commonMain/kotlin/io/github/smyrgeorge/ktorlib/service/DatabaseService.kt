package io.github.smyrgeorge.ktorlib.service

import arrow.core.Either
import io.github.smyrgeorge.ktorlib.context.Context
import io.github.smyrgeorge.ktorlib.service.DatabaseService.Companion.withTransaction
import io.github.smyrgeorge.ktorlib.service.Service.Companion.with
import io.github.smyrgeorge.sqlx4k.Driver
import io.github.smyrgeorge.sqlx4k.Transaction

/**
 * Represents an abstract interface for database services, extending [Service]
 * and defining a contract for handling database transactions through a [Driver] instance.
 *
 * This interface provides a foundation for database-related service abstractions. It includes:
 * - A [db] property representing the database driver instance.
 * - A utility companion object function [withTransaction], enabling scoped execution of
 *   operations within a database transaction.
 */
interface DatabaseService : Service {
    val db: Driver

    companion object {
        suspend fun <R> DatabaseService.withTransaction(f: suspend context(Context, Transaction)() -> R): R {
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