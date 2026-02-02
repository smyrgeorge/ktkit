package io.github.smyrgeorge.ktkit.sqlx4k

import arrow.core.raise.context.Raise
import arrow.core.raise.context.bind
import arrow.core.raise.either
import io.github.smyrgeorge.ktkit.api.error.ErrorSpec
import io.github.smyrgeorge.ktkit.api.error.impl.DatabaseError
import io.github.smyrgeorge.ktkit.api.error.impl.UnknownError
import io.github.smyrgeorge.ktkit.context.ExecContext
import io.github.smyrgeorge.ktkit.service.Service
import io.github.smyrgeorge.ktkit.sqlx4k.DatabaseService.Companion.withTransaction
import io.github.smyrgeorge.ktkit.util.AppResult
import io.github.smyrgeorge.log4k.TracingContext.Companion.span
import io.github.smyrgeorge.log4k.impl.OpenTelemetryAttributes
import io.github.smyrgeorge.sqlx4k.Driver
import io.github.smyrgeorge.sqlx4k.SQLError
import io.github.smyrgeorge.sqlx4k.Transaction
import io.github.smyrgeorge.sqlx4k.arrow.impl.extensions.DbResult

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
         * Executes a database operation within a context that handles error propagation and transformation.
         *
         * This function allows executing a database operation (provided in the `block` parameter)
         * while managing error handling within the provided error context. Any database-specific errors
         * are converted into application-level error representations.
         *
         * @param block A lambda representing the database operation to be executed. It operates within a context
         *              that raises `SQLError` types and returns a result of type `A`.
         * @return The result of the database operation of type `A` after error handling and transformation.
         */
        context(_: Raise<ErrorSpec>)
        inline fun <A> db(block: context(Raise<SQLError>)() -> A): A =
            dbCaching { block() }.toAppResult().bind()

        /**
         * A utility method for caching database operations.
         *
         * This function provides a context-aware mechanism to execute a block of code within
         * a database operation that can handle SQL-related errors. It leverages a `Raise` type
         * for error handling, allowing clean error management in a functional style.
         *
         * @param block The block of code to execute, which operates within the `Raise<SQLError>`
         *              context to handle potential SQL errors during database operations.
         * @return A result of type `DbResult<A>` that encapsulates either the successful execution
         *         or an error.
         */
        context(_: Raise<ErrorSpec>)
        inline fun <A> dbCaching(block: context(Raise<SQLError>)() -> A): DbResult<A> =
            either {
                runCatching { block() }.getOrElse {
                    val error = when (it) {
                        is SQLError -> it
                        else -> SQLError(SQLError.Code.UnknownError, it.message ?: "Unknown error")
                    }
                    raise(error)
                }
            }

        /**
         * Executes a transactional context for a given operation. The operation is wrapped
         * in a database transaction and executed while tracking its duration using OpenTelemetry.
         *
         * @param block The function to be executed within a database transaction. This function
         *          is provided with an implicit [Transaction] context and can return a result of type [R].
         * @return The result of the function execution within the transactional context.
         */
        context(ec: ExecContext)
        suspend inline fun <R> DatabaseService.withTransaction(
            crossinline block: suspend context(Transaction)() -> R
        ): R = db.transaction {
            // Wrap the function execution in a span to track its execution duration.
            val tags = mapOf(OpenTelemetryAttributes.SERVICE_NAME to app.name)
            ec.span("db.transaction", tags) { block() }
        }

        /**
         * Executes a transactional operation within a database context while catching and handling errors,
         * returning a result wrapped in an [AppResult].
         *
         * Combines the transactional execution using [withTransaction] with error handling using the `either` construct.
         * This method provides a convenient way to execute database operations within a transaction and handle
         * potential failures in a unified manner.
         *
         * @param block The suspending function to execute within a database transaction. The function is provided with an
         *          implicit [Transaction] context and is expected to produce a result of type [R].
         * @return An [AppResult] containing the result of the operation if successful, or an error if an exception occurs.
         */
        context(ec: ExecContext)
        suspend inline fun <R> DatabaseService.withTransactionCatching(
            crossinline block: suspend context(Transaction)() -> R
        ): AppResult<R> = either {
            runCatching { withTransaction(block) }.getOrElse {
                val error = when (it) {
                    is ErrorSpec -> it
                    else -> UnknownError(it.message ?: "Unknown error")
                }
                raise(error)
            }
        }
    }
}
