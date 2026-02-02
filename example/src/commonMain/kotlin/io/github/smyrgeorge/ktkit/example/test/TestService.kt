package io.github.smyrgeorge.ktkit.example.test

import io.github.smyrgeorge.ktkit.context.ExecContext
import io.github.smyrgeorge.ktkit.sqlx4k.AuditableDatabaseService
import io.github.smyrgeorge.ktkit.sqlx4k.DatabaseService.Companion.db
import io.github.smyrgeorge.log4k.Logger
import io.github.smyrgeorge.sqlx4k.Driver
import io.github.smyrgeorge.sqlx4k.QueryExecutor
import io.github.smyrgeorge.sqlx4k.Transaction

class TestService(
    override val db: Driver,
    override val repo: TestRepository,
) : AuditableDatabaseService<Test> {
    val log = Logger.of(this::class)

    context(_: ExecContext, _: QueryExecutor)
    private suspend fun findAll(): List<Test> = db { repo.findAll() }

    context(_: ExecContext, _: Transaction)
    suspend fun test(): List<Test> {
        log.info { "Fetching all tests" }

        return findAll().also {
            log.info { "Fetched ${it.size} tests" }
        }
    }
}
