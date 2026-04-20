package io.github.smyrgeorge.ktkit.example.test

import arrow.core.raise.Raise
import io.github.smyrgeorge.ktkit.api.error.ErrorSpec
import io.github.smyrgeorge.ktkit.context.ExecContext
import io.github.smyrgeorge.ktkit.sqlx4k.AuditableDatabaseService
import io.github.smyrgeorge.ktkit.sqlx4k.DatabaseService.Companion.db
import io.github.smyrgeorge.log4k.Logger
import io.github.smyrgeorge.log4k.TracingContext
import io.github.smyrgeorge.log4k.context.info
import io.github.smyrgeorge.sqlx4k.Driver
import io.github.smyrgeorge.sqlx4k.QueryExecutor
import io.github.smyrgeorge.sqlx4k.Transaction

class TestService(
    override val db: Driver,
    override val repo: TestRepository,
) : AuditableDatabaseService<Test> {
    val log = Logger.of(this::class)

    context(_: Raise<ErrorSpec>, _: QueryExecutor)
    private suspend fun findAll(): List<Test> = db { repo.findAll() }

    context(_: TracingContext, _: Raise<ErrorSpec>, _: Transaction)
    suspend fun test(): List<Test> {
        log.info { "Fetching all tests" }
        return findAll().also {
            log.info { "Fetched ${it.size} tests" }
        }
    }

    context(_:TracingContext, _:Raise<ErrorSpec>, _: Transaction)
    suspend fun createAndFetchAll(): List<Test> {
        val row = Test(test = "Test", data = Test.Data())
        db { repo.save(row) }
        return findAll().also {
            log.info { "Fetched ${it.size} tests" }
        }
    }
}
