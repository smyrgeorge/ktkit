package io.github.smyrgeorge.ktorlib.service.auditable

import arrow.core.Either
import io.github.smyrgeorge.ktorlib.context.ExecutionContext
import io.github.smyrgeorge.ktorlib.error.system.DatabaseError
import io.github.smyrgeorge.log4k.TracingContext.Companion.span
import io.github.smyrgeorge.log4k.impl.OpenTelemetryAttributes
import io.github.smyrgeorge.sqlx4k.SQLError
import io.github.smyrgeorge.sqlx4k.Statement
import io.github.smyrgeorge.sqlx4k.arrow.ArrowContextCrudRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalContextParameters::class, ExperimentalUuidApi::class)
interface AuditableRepository<T : Auditable<*>> : ArrowContextCrudRepository<T> {
    override suspend fun preInsertHook(entity: T): T {
        val user = ctx().user
        entity.createdAt = Clock.System.now()
        entity.createdBy = user.uuid
        entity.updatedAt = entity.createdAt
        entity.updatedBy = entity.createdBy
        return entity
    }

    override suspend fun preUpdateHook(entity: T): T {
        entity.updatedAt = Clock.System.now()
        entity.updatedBy = ctx().user.uuid
        return entity
    }

    override suspend fun <R> aroundQuery(method: String, statement: Statement, block: suspend () -> R): R {
        val operation = "db.${this::class.simpleName}.$method"
        return ctx().tracingContext.span(
            name = operation,
            tags = mapOf(
                OpenTelemetryAttributes.DB_STATEMENT to statement.toString(),
                OpenTelemetryAttributes.DB_OPERATION to operation,
                OpenTelemetryAttributes.DB_DRIVER_NAME to "sqlx4k"
            )
        ) {
            when (val res = block()) {
                // Normally, we don't expect Result<*> to be returned from the repository (ArrowContextCrudRepository).
                // However, we do it just in case this will change in the future.
                is Result<*> if res.isFailure -> {
                    // Early close the span if the query failed.
                    exception(res.exceptionOrNull()!!, true)
                    end(res.exceptionOrNull()!!)
                    res
                }

                // Normally, we expect DbResult<*> to be returned from the repository (ArrowContextCrudRepository).
                is Either<*, *> if res.isLeft() -> {
                    val e = when (val e = res.leftOrNull()!!) {
                        is SQLError -> e
                        is Throwable -> e
                        else -> DatabaseError(code = "UnknownError", message = "Unknown error occurred: $e")
                    } as Throwable

                    // Early close the span if the query failed.
                    exception(e, true)
                    end(e)
                    res
                }

                else -> res
            }
        }
    }

    private suspend fun ctx(): ExecutionContext =
        currentCoroutineContext()[ExecutionContext.Companion] ?: error("Sanity check :: could not extract Context.")
}