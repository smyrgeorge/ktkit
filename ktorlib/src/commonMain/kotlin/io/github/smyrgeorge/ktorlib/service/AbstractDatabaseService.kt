package io.github.smyrgeorge.ktorlib.service

import io.github.smyrgeorge.ktorlib.context.ExecutionContext
import io.github.smyrgeorge.ktorlib.service.AbstractDatabaseService.Companion.withTransaction
import io.github.smyrgeorge.ktorlib.util.MyResult
import io.github.smyrgeorge.log4k.TracingContext.Companion.span
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
        context(c: ExecutionContext)
        suspend inline fun <R> AbstractDatabaseService.withTransaction(
            crossinline f: suspend context(Transaction)() -> MyResult<R>
        ): MyResult<R> = db.transaction {
            c.span("db.transaction") { f() }.onLeft { throw it.toThrowable() }
        }
    }
}
