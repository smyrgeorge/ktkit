package io.github.smyrgeorge.ktorlib.example.test

import io.github.smyrgeorge.ktorlib.context.ExecutionContext
import io.github.smyrgeorge.ktorlib.service.AbstractDatabaseService
import io.github.smyrgeorge.ktorlib.util.EitherThrowable
import io.github.smyrgeorge.log4k.Logger
import io.github.smyrgeorge.log4k.TracingContext
import io.github.smyrgeorge.sqlx4k.Driver
import io.github.smyrgeorge.sqlx4k.Transaction

class TestService(
    override val db: Driver,
    private val testRepository: TestRepository
) : AbstractDatabaseService {
    val log = Logger.of(this::class)

    context(_: ExecutionContext, tc: TracingContext, t: Transaction)
    suspend fun findAll(): EitherThrowable<List<Test>> {
        log.info { "Fetching all tests" }
        return testRepository.findAll()
    }
}

