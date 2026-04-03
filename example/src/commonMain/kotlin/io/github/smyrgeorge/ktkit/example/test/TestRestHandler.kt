package io.github.smyrgeorge.ktkit.example.test

import arrow.core.raise.context.raise
import io.github.smyrgeorge.ktkit.api.rest.impl.XRealNameRestHandler
import io.github.smyrgeorge.ktkit.sqlx4k.DatabaseService.Companion.withTransaction
import io.ktor.server.routing.Route

class TestRestHandler(
    private val testService: TestService,
    private val serviceVariations: ServiceVariations,
    private val transactionalService: TransactionalService
) : XRealNameRestHandler() {
    override fun String.uri(): String = "/api/v1/test$this"

    override fun Route.routes() {
        GET("") {
            log.info { "Hello, ${user.username}!" }
            testService.withTransaction {
                testService.test().map { it.toDto() }
            }
        }

        GET("/result") {
            val fail = queryParam("fail").asBooleanOrNull() ?: false
            log.info { "Hello, ${user.username}!" }
            serviceVariations.withTransaction {
                serviceVariations.findAllResult(fail).map { tests -> tests.map { it.toDto() } }
            }
        }

        GET("/either") {
            val fail = queryParam("fail").asBooleanOrNull() ?: false
            log.info { "Hello, ${user.username}!" }
            serviceVariations.withTransaction {
                serviceVariations.findAllEither(fail).map { tests -> tests.map { it.toDto() } }
            }
        }

        GET("/raise") {
            val fail = queryParam("fail").asBooleanOrNull() ?: false
            log.info { "Hello, ${user.username}!" }
            serviceVariations.withTransaction {
                serviceVariations.findAllRaise(fail).map { it.toDto() }
            }
        }

        PUT("/raise/update/{id}") {
            val id = pathVariable("id").asInt()
            val fail = queryParam("fail").asBooleanOrNull() ?: false
            log.info { "User ${user.username} tries to update test $id multiple times." }
            transactionalService.withTransaction {
                transactionalService.updateMultiple( id).toDto()
                if(fail) raise(TestFail("Failed updating test $id!"))
                transactionalService.updateMultiple( id).toDto()
            }
        }

        POST("/raise/create") {
            log.info { "User ${user.username} tries to create a test in a transaction." }
            val fail = queryParam("fail").asBooleanOrNull() ?: false
            transactionalService.withTransaction {
                transactionalService.create().toDto()
                if(fail) raise(TestFail("Failed creating test."))
            }
        }
    }
}
