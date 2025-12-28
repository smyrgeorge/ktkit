package io.github.smyrgeorge.ktorlib.service

import arrow.core.Either
import io.github.smyrgeorge.ktorlib.service.AbstractDatabaseService.Companion.withTransaction
import io.github.smyrgeorge.ktorlib.util.EitherThrowable
import io.github.smyrgeorge.sqlx4k.Driver
import io.github.smyrgeorge.sqlx4k.SQLError
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
        suspend inline fun <R> AbstractDatabaseService.withTransaction(
            crossinline f: suspend context(Transaction)() -> R
        ): R = db.transaction {
            when (val result = f()) {
                // Ensure that in case of an error, the transaction is rolled back.
                is Result<*> if result.isFailure -> throw result.exceptionOrNull()!!
                is Either<*, *> if result.isLeft() ->
                    throw (result.leftOrNull() as? Throwable
                        ?: SQLError(SQLError.Code.UknownError, result.leftOrNull().toString()))

                else -> result
            }
        }

        suspend inline fun <R> AbstractDatabaseService.withTransactionCatching(
            noinline f: suspend context(Transaction)() -> R
        ): EitherThrowable<R> = Either.catch { withTransaction(f) }
    }
}
