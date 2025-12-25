package io.github.smyrgeorge.ktorlib.example.test

import arrow.core.Either
import io.github.smyrgeorge.ktorlib.context.Context
import io.github.smyrgeorge.ktorlib.util.AbstractService
import io.github.smyrgeorge.log4k.Logger
import io.github.smyrgeorge.sqlx4k.Driver
import io.github.smyrgeorge.sqlx4k.Transaction

typealias FlowResult<T> = Either<Throwable, T>

class TestService(
    override val db: Driver,
    private val testRepository: TestRepository
) : AbstractService {
    override val log = Logger.of(this::class)

    context(_: Context, tx: Transaction)
    suspend fun findAll(): FlowResult<List<Test>> {
        log.info { "Fetching all tests" }
        return testRepository.findAll()
    }
}

