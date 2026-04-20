package io.github.smyrgeorge.ktkit.example.test

import arrow.core.raise.Raise
import arrow.core.raise.context.raise
import io.github.smyrgeorge.ktkit.api.error.ErrorSpec
import io.github.smyrgeorge.ktkit.api.error.impl.NotFound
import io.github.smyrgeorge.ktkit.api.error.impl.UnknownError
import io.github.smyrgeorge.ktkit.context.ExecContext
import io.github.smyrgeorge.ktkit.sqlx4k.AuditableDatabaseService
import io.github.smyrgeorge.ktkit.sqlx4k.DatabaseService.Companion.db
import io.github.smyrgeorge.log4k.Logger
import io.github.smyrgeorge.log4k.TracingContext
import io.github.smyrgeorge.log4k.context.info
import io.github.smyrgeorge.sqlx4k.Driver
import io.github.smyrgeorge.sqlx4k.QueryExecutor
import io.github.smyrgeorge.sqlx4k.Transaction
import kotlin.time.Clock
import kotlin.time.Instant

class TestService(
    override val db: Driver,
    override val repo: TestRepository,
) : AuditableDatabaseService<Test> {
    val log = Logger.of(this::class)

    context(_: Raise<ErrorSpec>, _: QueryExecutor)
    private suspend fun findAll(): List<Test> = db { repo.findAll() }

    context(_: Raise<ErrorSpec>, _: QueryExecutor)
    private suspend fun findOneById(id: Int): Test? = db { repo.findOneById(id) }

    context(_: ExecContext, _: Raise<ErrorSpec>, _: Transaction)
    suspend fun test(): List<Test> {
        log.info { "Fetching all tests" }
        return findAll().also {
            log.info { "Fetched ${it.size} tests" }
        }
    }

    context(_: ExecContext, _:Raise<ErrorSpec>, _: Transaction)
    suspend fun createAndFetchAll(fail: Boolean = false): List<Test> {
        val row = Test(test = "Test", data = Test.Data())
        db { repo.save(row) }

        // Fail on purpose
        if(fail) raise(UnknownError("Failed on purpose via flag to test transaction"))
        return findAll().also {
            log.info { "Fetched ${it.size} tests" }
        }
    }

    context(_: ExecContext, _: Raise<ErrorSpec>, _: Transaction)
    suspend fun updateAndFetchAll(id: Int, fail: Boolean = false): List<Test> {
        val existing = findOneById(id) ?: raise(NotFound("Test with id $id not found"))
        val updated = existing.copy(test = "Updated ${Clock.System.now()}")
        db { repo.save(updated)}

        // Fail on purpose
        if(fail) raise(UnknownError("Failed on purpose via flag to test transaction"))
        return findAll().also {
            log.info { "Updated ${it.size} tests" }
        }
    }
}
