package io.github.smyrgeorge.ktkit.example.test

import arrow.core.left
import arrow.core.raise.context.Raise
import arrow.core.raise.context.bind
import io.github.smyrgeorge.ktkit.api.error.impl.UnknownError
import io.github.smyrgeorge.ktkit.context.ExecContext
import io.github.smyrgeorge.ktkit.sqlx4k.AuditableDatabaseService
import io.github.smyrgeorge.ktkit.sqlx4k.DatabaseService.Companion.db
import io.github.smyrgeorge.ktkit.sqlx4k.DatabaseService.Companion.toAppResult
import io.github.smyrgeorge.log4k.Logger
import io.github.smyrgeorge.sqlx4k.Driver
import io.github.smyrgeorge.sqlx4k.QueryExecutor
import io.github.smyrgeorge.sqlx4k.SQLError
import io.github.smyrgeorge.sqlx4k.Transaction

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

//        val test = db { sqlx4k() }
//        UnknownError("Boom!").left().bind()
//        error("Boom!")

        return findAll().also {
            log.info { "Fetched ${it.size} tests" }
        }
    }

    // Asume the sqlx4k change it's signatures to the following:
    context(_: Raise<SQLError>, _: QueryExecutor)
    fun sqlx4k(): Int = 0
}

