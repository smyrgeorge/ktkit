package io.github.smyrgeorge.ktorlib.example.test

import io.github.smyrgeorge.ktorlib.util.AbstractService
import io.github.smyrgeorge.log4k.Logger
import io.github.smyrgeorge.log4k.TracingEvent
import io.github.smyrgeorge.sqlx4k.Driver
import io.github.smyrgeorge.sqlx4k.QueryExecutor

class TestService(
    override val db: Driver,
    private val testRepository: TestRepository
) : AbstractService {
    override val log = Logger.of(this::class)

    context(span: TracingEvent.Span, db: QueryExecutor)
    suspend fun findAll(): List<Test> {
        log.info(span) { "Fetching all tests" }
        return testRepository.findAll().getOrThrow()
    }
}
