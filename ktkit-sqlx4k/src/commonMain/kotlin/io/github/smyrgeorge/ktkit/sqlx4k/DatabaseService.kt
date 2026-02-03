package io.github.smyrgeorge.ktkit.sqlx4k

import arrow.core.Either
import arrow.core.raise.context.Raise
import arrow.core.raise.context.bind
import io.github.smyrgeorge.ktkit.api.error.ErrorSpec
import io.github.smyrgeorge.ktkit.api.error.impl.DatabaseError
import io.github.smyrgeorge.ktkit.context.ExecContext
import io.github.smyrgeorge.ktkit.service.Service
import io.github.smyrgeorge.ktkit.util.KtKitDSL
import io.github.smyrgeorge.log4k.TracingContext.Companion.span
import io.github.smyrgeorge.log4k.impl.OpenTelemetryAttributes
import io.github.smyrgeorge.sqlx4k.Driver
import io.github.smyrgeorge.sqlx4k.Transaction
import io.github.smyrgeorge.sqlx4k.arrow.impl.extensions.DbResult

/**
 * Provides database-related services and utilities for working with database operations.
 * This interface extends [Service], incorporating common service-level functionality
 * and additional database-specific methods.
 */
interface DatabaseService : Service {
    val db: Driver

    @Suppress("NOTHING_TO_INLINE")
    companion object {
        /**
         * Maps the error component of a `DbResult` to a `DatabaseError` object, while preserving the success value.
         *
         * This method converts the left (`Error`) side of the `DbResult` into a more specific `DatabaseError`,
         * encapsulating the underlying error details such as the error code, cause, and message.
         *
         * @return An `Either` object where the left side is a `DatabaseError` constructed from the current error,
         *         and the right side is the success value of the original `DbResult`.
         */
        @PublishedApi
        internal inline fun <T> DbResult<T>.mapError(): Either<DatabaseError, T> =
            mapLeft { DatabaseError(it.code.name, it, it.message ?: "No message provided.") }

        /**
         * Executes a database operation within a safe context, raising errors as specified by the [ErrorSpec] interface.
         *
         * This function wraps a database operation block and processes potential errors using `dbCatching`.
         * The result is unwrapped and returned to the caller, assuming the operation succeeds.
         * If the operation fails, an error is raised within the context of the [ErrorSpec].
         *
         * @param block The lambda expression representing the database operation to be executed.
         * @return The result of the database operation, of generic type [T], if the operation is successful.
         * @throws DatabaseError if the database operation fails and an error is raised in the context of [ErrorSpec].
         */
        @KtKitDSL
        context(_: Raise<ErrorSpec>)
        inline fun <T> db(block: () -> DbResult<T>): T = dbCatching(block).bind()

        /**
         * Executes a given database operation block, catching any potential errors and mapping them to a consistent
         * error format. This method ensures robust error handling and encapsulates the result of the database operation
         * in an `Either` type.
         *
         * @param block The database operation to execute, returning a `DbResult` containing either a success value
         *              of type `T` or a failure.
         * @return An `Either` instance where the right side is the successful result of the database operation (`T`)
         *         and the left side is a mapped `DatabaseError` for any encountered errors.
         */
        @KtKitDSL
        context(_: Raise<ErrorSpec>)
        inline fun <T> dbCatching(block: () -> DbResult<T>): Either<DatabaseError, T> = block().mapError()

        /**
         * Executes a transactional context for a given operation. The operation is wrapped
         * in a database transaction and executed while tracking its duration using OpenTelemetry.
         *
         * @param block The function to be executed within a database transaction. This function
         *          is provided with an implicit [Transaction] context and can return a result of type [R].
         * @return The result of the function execution within the transactional context.
         */
        @KtKitDSL
        context(ec: ExecContext)
        suspend inline fun <R> DatabaseService.withTransaction(
            crossinline block: suspend context(Transaction)() -> R
        ): R = db.transaction {
            // Wrap the function execution in a span to track its execution duration.
            val tags = mapOf(OpenTelemetryAttributes.SERVICE_NAME to app.name)
            ec.span("db.transaction", tags) { block() }
        }
    }
}
