package io.github.smyrgeorge.ktkit.sqlx4k

import arrow.core.raise.context.Raise
import arrow.core.raise.context.bind
import arrow.core.raise.either
import io.github.smyrgeorge.ktkit.api.error.ErrorSpec
import io.github.smyrgeorge.ktkit.api.error.impl.DatabaseError
import io.github.smyrgeorge.ktkit.context.ExecContext
import io.github.smyrgeorge.ktkit.service.Service
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
            mapLeft { DatabaseError(it.code.name, it, it.message ?: "No message provided.") }

        /**
         * Executes a database operation within a caching context, handling SQL-related errors and binding
         * the resulting application-level outcome.
         *
         * This function takes a block of code as a lambda, executes it within the context of a database
         * caching operation, and converts the result into an application-level result. The execution
         * context includes error handling for SQL-related errors.
         *
         * @param block A lambda block to be executed in the context of a database operation. The block may
         *              raise SQL-related errors and is expected to return a value of type `A`.
         * @return The result of the executed block after being transformed into an application-level format
         *         and bound to the overall computation.
         */
        context(_: Raise<ErrorSpec>)
        inline fun <A> db(block: context(Raise<SQLError>)() -> A): A =
            dbCaching { block() }.toAppResult().bind()

        /**
         * Executes a block of code within a database caching context, handling any SQL-related errors
         * and returning a result wrapped in a `DbResult`.
         *
         * @param block A lambda block to be executed within the given context. The block may raise SQL-related
         *              errors and is designed to return a value of type `A`.
         * @return A `DbResult` instance that wraps the result of the executed block or captures any errors
         *         encountered during execution.
         */
        context(rc: Raise<ErrorSpec>)
        inline fun <A> dbCaching(block: context(Raise<SQLError>)() -> A): DbResult<A> =
            either { block() }

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
    }
}
