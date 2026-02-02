package io.github.smyrgeorge.ktkit.example.test

import arrow.core.raise.context.Raise
import arrow.core.raise.context.bind
import arrow.core.raise.context.raise
import io.github.smyrgeorge.ktkit.context.ExecContext
import io.github.smyrgeorge.ktkit.sqlx4k.AuditableDatabaseService
import io.github.smyrgeorge.ktkit.sqlx4k.DatabaseService.Companion.db
import io.github.smyrgeorge.ktkit.sqlx4k.DatabaseService.Companion.dbCaching
import io.github.smyrgeorge.ktkit.sqlx4k.DatabaseService.Companion.toAppResult
import io.github.smyrgeorge.log4k.Logger
import io.github.smyrgeorge.sqlx4k.Driver
import io.github.smyrgeorge.sqlx4k.QueryExecutor
import io.github.smyrgeorge.sqlx4k.SQLError
import io.github.smyrgeorge.sqlx4k.Transaction
import io.github.smyrgeorge.sqlx4k.arrow.impl.extensions.DbResult

class TestService(
    override val db: Driver,
    override val repo: TestRepository,
) : AuditableDatabaseService<Test> {
    val log = Logger.of(this::class)

    context(_: ExecContext, _: QueryExecutor)
    private suspend fun findAll(): List<Test> =
        repo.findAll().toAppResult().bind()

    context(_: ExecContext, _: Transaction)
    suspend fun test(): List<Test> {
        log.info { "Fetching all tests" }

        val test: Int = db { sqlx4kError() }
        log.info { "Fetched $test tests" }

        val handled: DbResult<Int> = dbCaching { sqlx4kError() }
        log.info { "Fetched $handled tests" }

        return findAll().also {
            log.info { "Fetched ${it.size} tests" }
        }
    }

    // Assume the sqlx4k change it's signatures to the following:
    context(_: Raise<SQLError>, _: QueryExecutor)
    fun sqlx4k(): Int = 5

    context(_: Raise<SQLError>, _: QueryExecutor)
    fun sqlx4kError(): Int = raise(SQLError(SQLError.Code.UnknownError, "Unknown error"))
}
