package io.github.smyrgeorge.ktkit.example.test

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
            log.info { "Hello, ${user.username}!" }
            testService.withTransaction {
                testService.createAndFetchAll().map { it.toDto() }
            }
        }

        GET("/result") {
            log.info { "Hello, ${user.username}!" }
            val fail = queryParam("fail").asBooleanOrNull() ?: false
            serviceVariations.withTransaction {
                serviceVariations.findAllResult(fail).map { tests -> tests.map { it.toDto() } }
            }
        }

        GET("/either") {
            log.info { "Hello, ${user.username}!" }
            val fail = queryParam("fail").asBooleanOrNull() ?: false
            serviceVariations.withTransaction {
                serviceVariations.findAllEither(fail).map { tests -> tests.map { it.toDto() } }
            }
        }

        GET("/raise") {
            log.info { "Hello, ${user.username}!" }
            val fail = queryParam("fail").asBooleanOrNull() ?: false
            serviceVariations.withTransaction {
                serviceVariations.findTestRaise(fail).map { it.toDto() }
            }
        }
    }
}
