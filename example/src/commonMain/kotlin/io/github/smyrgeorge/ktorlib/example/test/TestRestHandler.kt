package io.github.smyrgeorge.ktorlib.example.test

import io.github.smyrgeorge.ktorlib.api.rest.impl.AnonymousRestHandler
import io.github.smyrgeorge.log4k.Logger
import io.github.smyrgeorge.log4k.Tracer
import io.ktor.server.routing.Route

class TestRestHandler(
    private val testService: TestService
) : AnonymousRestHandler() {

    override val log = Logger.of(this::class)
    val tracer = Tracer.of(this::class)

    override fun String.uri(): String = "/api/v1/test$this"

    override fun Route.routes() {
        GET("") {
            tracer.span("test") {
                log.info("Hello, ${user.username}!")
                testService.withTransaction {
                    testService.findAll()
                }
            }
        }
    }
}
