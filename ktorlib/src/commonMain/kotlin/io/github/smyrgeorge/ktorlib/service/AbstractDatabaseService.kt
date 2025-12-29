package io.github.smyrgeorge.ktorlib.service

import arrow.core.Either
import io.github.smyrgeorge.ktorlib.error.Error
import io.github.smyrgeorge.ktorlib.error.InternalError
import io.github.smyrgeorge.ktorlib.service.AbstractDatabaseService.Companion.withTransaction
import io.github.smyrgeorge.log4k.TracingContext
import io.github.smyrgeorge.sqlx4k.Driver
import io.github.smyrgeorge.sqlx4k.Transaction

/**
 * Represents an abstract interface for database services, extending [AbstractService]
 * and defining a contract for handling database transactions through a [Driver] instance.
 *
 * This interface provides a foundation for database-related service abstractions. It includes:
 * - A [db] property representing the database driver instance.
 * - A utility companion object function [withTransaction], enabling scoped execution of
 *   operations within a database transaction.
 */
interface AbstractDatabaseService : AbstractService {
    val db: Driver

    companion object {
        context(tc: TracingContext)
        suspend inline fun <R> AbstractDatabaseService.withTransaction(
            crossinline f: suspend context(TracingContext, Transaction)() -> R
        ): R = db.transaction {
            when (val result = f()) {
                // Ensure that in case of an error, the transaction is rolled back.
                is Result<*> if result.isFailure -> throw result.exceptionOrNull()!!
                is Either<*, *> if result.isLeft() -> {
                    when (val error = result.leftOrNull() ?: throw IllegalStateException("Unexpected null error")) {
                        is Error -> throw error.toThrowable()
                        is InternalError -> throw error
                        else -> throw IllegalStateException("Unexpected error type: $error")
                    }
                }

                else -> result
            }
        }
    }
}
