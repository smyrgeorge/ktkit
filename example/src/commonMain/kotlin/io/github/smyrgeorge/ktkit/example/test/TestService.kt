package io.github.smyrgeorge.ktkit.example.test

import arrow.core.raise.context.bind
import io.github.smyrgeorge.ktkit.context.ExecutionContext
import io.github.smyrgeorge.ktkit.service.AbstractDatabaseService
import io.github.smyrgeorge.ktkit.service.AbstractDatabaseService.Companion.toDomainResult
import io.github.smyrgeorge.ktkit.util.DomainResult
import io.github.smyrgeorge.log4k.Logger
import io.github.smyrgeorge.sqlx4k.Driver
import io.github.smyrgeorge.sqlx4k.QueryExecutor
import io.github.smyrgeorge.sqlx4k.Transaction

class TestService(
    override val db: Driver,
    private val testRepository: TestRepository
) : AbstractDatabaseService {
    val log = Logger.of(this::class)

    context(_: ExecutionContext, _: QueryExecutor)
    private suspend fun findAll(): DomainResult<List<Test>> =
        testRepository.findAll().toDomainResult()

    context(_: ExecutionContext, _: Transaction)
    suspend fun test(): DomainResult<List<Test>> {
        log.info { "Fetching all tests" }
        return findAll().also {
            log.info { "Fetched ${it.bind().size} tests" }
        }
    }
}

