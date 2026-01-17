package io.github.smyrgeorge.ktkit.sqlx4k

import arrow.core.raise.context.Raise
import arrow.core.raise.context.bind
import arrow.core.raise.either
import io.github.smyrgeorge.ktkit.api.error.ErrorSpec
import io.github.smyrgeorge.ktkit.api.error.impl.DatabaseError
import io.github.smyrgeorge.ktkit.context.ExecContext
import io.github.smyrgeorge.ktkit.service.Service
import io.github.smyrgeorge.ktkit.sqlx4k.DatabaseService.Companion.db
import io.github.smyrgeorge.ktkit.sqlx4k.DatabaseService.Companion.withTransaction
import io.github.smyrgeorge.ktkit.util.AppResult
import io.github.smyrgeorge.log4k.TracingContext.Companion.span
import io.github.smyrgeorge.log4k.impl.OpenTelemetryAttributes
import io.github.smyrgeorge.sqlx4k.Driver
import io.github.smyrgeorge.sqlx4k.SQLError
import io.github.smyrgeorge.sqlx4k.Transaction
import io.github.smyrgeorge.sqlx4k.arrow.impl.extensions.DbResult

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
        /**
         * Transforms a [DbResult] instance into an [AppResult] by mapping any left-side error to a [DatabaseError].
         * This conversion ensures that database-level errors are encapsulated in a standardized error format.
         *
         * @receiver A [DbResult] containing either a success value of type `T` or an error.
         * @return An [AppResult] where the left-side errors of [DbResult] are mapped to [DatabaseError] instances.
         */
        fun <T> DbResult<T>.toAppResult(): AppResult<T> =
            mapLeft { DatabaseError(it.code.name, it.message ?: "Unknown error") }

        /**
         * Executes a database operation within the context of error handling.
         *
         * This method allows invoking a block of code that performs a database operation.
         * It uses the provided error handling context to manage errors, returning the
         * result of the operation or propagating the error if one occurs.
         *
         * @param block A lambda representing the database operation to be executed.
         *              The block has access to a context for raising SQL-related errors.
         * @return The result of the executed database operation or an error wrapped in
         *         the appropriate application result type.
         */
        context(_: Raise<ErrorSpec>)
        inline fun <A> db(block: context(Raise<SQLError>) () -> A): A =
            either { block() }.toAppResult().bind()

        /**
         * Executes a block of code within the context of a database transaction. If the executed block fails,
         * an error will be thrown, converting the failure to a throwable. The execution duration is tracked
         * with a span for tracing purposes.
         *
         * @param f The block of code to be executed within the transaction. It receives an implicit [Transaction]
         *          instance and produces an [AppResult] containing either a success result of type [R] or an error.
         * @return An [AppResult] containing the success result of type [R], or an error if the transaction failed.
         */
        context(c: ExecContext)
        suspend inline fun <R> DatabaseService.withTransaction(
            crossinline f: suspend Transaction.() -> AppResult<R>
        ): AppResult<R> = db.transaction {
            // Wrap the function execution in a span to track its execution duration.
            val tags = mapOf(OpenTelemetryAttributes.SERVICE_NAME to app.name)
            c.span("db.transaction", tags) { f().onLeft { throw it.toThrowable() } }
        }

        context(c: ExecContext)
        suspend inline fun <R> DatabaseService.withTransaction2(
            crossinline f: suspend Transaction.() -> R
        ): R = db.transaction {
            // Wrap the function execution in a span to track its execution duration.
            val tags = mapOf(OpenTelemetryAttributes.SERVICE_NAME to app.name)
            c.span("db.transaction", tags) {
                either<ErrorSpec, R> { f() }.bind()
            }
        }
    }
}
