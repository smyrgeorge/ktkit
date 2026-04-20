package io.github.smyrgeorge.ktkit.example.test

import arrow.core.raise.context.raise
import io.github.smyrgeorge.ktkit.api.rest.impl.XRealNameRestHandler
import io.github.smyrgeorge.ktkit.sqlx4k.DatabaseService.Companion.withTransaction
import io.github.smyrgeorge.log4k.context.info
import io.ktor.server.routing.Route

class TestRestHandler(
    private val testService: TestService,
    private val serviceVariations: ServiceVariations,
) : XRealNameRestHandler() {
    override fun String.uri(): String = "/api/v1/test$this"

    override fun Route.routes() {
        GET("") {
            log.info { "Hello, ${user.username}!" }
            testService.withTransaction {
                testService.test().map { it.toDto() }
            }
        }
        GET("/create-and-fetch-all") {
            val fail = queryParam("fail").asBooleanOrNull() ?: false
            log.info { "Hello, ${user.username}!" }

            testService.withTransaction {
                testService.createAndFetchAll(fail).map { it.toDto() }
            }
        }

        PUT("/update-and-fetch-all/{id}") {
            val id = pathVariable("id").asInt()
            val fail = queryParam("fail").asBooleanOrNull() ?: false
            log.info { "Hello, ${user.username}!" }
            testService.withTransaction {
                testService.updateAndFetchAll(id, fail).map {it.toDto()}
            }
        }
    }
}
