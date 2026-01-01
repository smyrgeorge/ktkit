package io.github.smyrgeorge.ktkit.service

import io.github.smyrgeorge.ktkit.context.ExecutionContext
import io.github.smyrgeorge.ktkit.error.system.DatabaseError
import io.github.smyrgeorge.ktkit.service.AbstractDatabaseService.Companion.withTransaction
import io.github.smyrgeorge.ktkit.util.DomainResult
import io.github.smyrgeorge.log4k.TracingContext.Companion.span
import io.github.smyrgeorge.sqlx4k.Driver
import io.github.smyrgeorge.sqlx4k.Transaction
import io.github.smyrgeorge.sqlx4k.arrow.impl.extensions.DbResult

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
        fun <T> DbResult<T>.toDomainResult(): DomainResult<T> =
            mapLeft { DatabaseError(it.code.name, it.message ?: "Unknown error") }

        /**
         * Executes the provided function within the context of a database transaction and returns the result.
         * The transaction is managed by the `db` driver instance of the implementing service.
         * If the function execution results in an error, the error is converted to a throwable and rethrown.
         *
         * @param f A suspendable function to be executed within the transaction scope.
         *          The function receives a [Transaction] context implicitly and must return a [DomainResult] of type [R].
         * @return A [DomainResult] of type [R] containing the result of the transaction execution.
         *         If the operation fails, the error is handled and transformed into a throwable.
         */
        context(c: ExecutionContext)
        suspend inline fun <R> AbstractDatabaseService.withTransaction(
            crossinline f: suspend context(Transaction)() -> DomainResult<R>
        ): DomainResult<R> = db.transaction {
            c.span("db.transaction") { f().onLeft { throw it.toThrowable() } }
        }
    }
}
