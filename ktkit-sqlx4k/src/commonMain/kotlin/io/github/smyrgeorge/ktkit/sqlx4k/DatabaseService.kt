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
            mapLeft { DatabaseError(it.code.name, it.message ?: "Unknown error") }

        context(_: Raise<ErrorSpec>)
        inline fun <A> db(block: context(Raise<SQLError>)() -> A): A =
            dbCaching { block() }.toAppResult().bind()

        context(_: Raise<ErrorSpec>)
        inline fun <A> dbCaching(block: context(Raise<SQLError>)() -> A): DbResult<A> =
            either { block() }

        context(ec: ExecContext)
        suspend inline fun <R> DatabaseService.withTransaction(
            crossinline f: suspend context(Transaction)() -> R
        ): R = db.transaction {
            // Wrap the function execution in a span to track its execution duration.
            val tags = mapOf(OpenTelemetryAttributes.SERVICE_NAME to app.name)
            ec.span("db.transaction", tags) { f() }
        }

        context(ec: ExecContext)
        suspend inline fun <R> DatabaseService.withTransactionCatching(
            crossinline f: suspend context(Transaction)() -> R
        ): AppResult<R> = either { withTransaction(f) }
    }
}
