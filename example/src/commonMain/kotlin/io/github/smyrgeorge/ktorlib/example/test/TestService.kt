package io.github.smyrgeorge.ktorlib.example.test

import arrow.core.Either
import arrow.core.raise.context.bind
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

    context(_: ExecutionContext, _: TracingContext, _: Transaction)
    suspend fun findAll(): EitherThrowable<List<Test>> {
        log.info { "Fetching all tests" }
        val test = test().bind()
        return testRepository.findAll()
    }

    fun test(): EitherThrowable<Unit> {
        return Either.Left(RuntimeException("Test exception"))
    }
}

