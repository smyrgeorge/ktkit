package io.github.smyrgeorge.ktorlib.example.test

import io.github.smyrgeorge.ktorlib.context.Context
import io.github.smyrgeorge.ktorlib.util.AbstractService
import io.github.smyrgeorge.log4k.Logger
import io.github.smyrgeorge.sqlx4k.Driver
import io.github.smyrgeorge.sqlx4k.Transaction

class TestService(
    override val db: Driver,
    private val testRepository: TestRepository
) : AbstractService {
    override val log = Logger.of(this::class)

    context(tx: Transaction)
    suspend fun findAll(): List<Test> {
        log.info { "Fetching all tests" }
        return testRepository.findAll().getOrThrow()
    }

    context(ctx: Context)
    suspend fun findAll(): List<Test> {
        log.info { "Fetching all tests" }
        return emptyList()
    }
}

