package io.github.smyrgeorge.ktkit.example.test

import io.github.smyrgeorge.ktkit.api.rest.impl.XRealNameRestHandler
import io.github.smyrgeorge.ktkit.api.rest.openapi.OpenApi
import io.github.smyrgeorge.ktkit.api.rest.openapi.OpenApiIgnore
import io.github.smyrgeorge.ktkit.api.rest.openapi.OpenApiInfo
import io.github.smyrgeorge.ktkit.sqlx4k.DatabaseService.Companion.withTransaction
import io.github.smyrgeorge.log4k.context.info
import io.ktor.server.routing.Route

class TestRestHandler(
    private val testService: TestService
) : XRealNameRestHandler() {
    override fun String.uri(): String = "/api/v1/test$this"

    override fun Route.routes() {
        @OpenApiIgnore
        GET("") {
            log.info { "Hello, ${user.username}!" }
            testService.withTransaction {
                testService.test().map { it.toDto() }
            }
        }

        @OpenApi("Creates a test entity and fetches all.")
        GET("/create-and-fetch-all") {
            @OpenApiInfo("Fail on purpose")
            val fail = queryParam("fail").asBooleanOrNull() ?: false
            log.info { "Hello, ${user.username}!" }
            testService.withTransaction {
                testService.createAndFetchAll(fail).map { it.toDto() }
            }
        }

        @OpenApi("Updates a test entity and fetches all.")
        PUT("/update-and-fetch-all/{id}") {
            val id = pathVariable("id").asInt()
            val fail = queryParam("fail").asBooleanOrNull() ?: false
            log.info { "Hello, ${user.username}!" }
            testService.withTransaction {
                testService.updateAndFetchAll(id, fail).map { it.toDto() }
            }
        }
    }
}
