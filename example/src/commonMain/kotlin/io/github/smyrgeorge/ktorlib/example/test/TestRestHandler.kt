package io.github.smyrgeorge.ktorlib.example.test

import io.github.smyrgeorge.ktorlib.api.rest.impl.XRealNameRestHandler
import io.github.smyrgeorge.ktorlib.service.AbstractDatabaseService.Companion.withTransaction
import io.ktor.server.routing.Route

class TestRestHandler(
    private val testService: TestService
) : XRealNameRestHandler() {
    override fun String.uri(): String = "/api/v1/test$this"

    override fun Route.routes() {
        GET("") {
            log.info { "Hello, ${user.username}!" }
            testService.withTransaction {
                testService.test()
            }
        }
    }
}
