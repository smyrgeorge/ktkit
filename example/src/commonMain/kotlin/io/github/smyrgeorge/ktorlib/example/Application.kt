package io.github.smyrgeorge.ktorlib.example

import io.github.smyrgeorge.ktorlib.Application
import io.github.smyrgeorge.ktorlib.api.rest.AbstractRestHandler
import io.github.smyrgeorge.ktorlib.api.rest.impl.ApplicationStatusRestHandler
import io.github.smyrgeorge.ktorlib.example.generated.TestRepositoryImpl
import io.github.smyrgeorge.ktorlib.example.test.TestRepository
import io.github.smyrgeorge.ktorlib.example.test.TestRestHandler
import io.github.smyrgeorge.ktorlib.example.test.TestService
import io.github.smyrgeorge.ktorlib.util.get
import io.github.smyrgeorge.log4k.impl.appenders.simple.SimpleConsoleTracingAppender
import io.github.smyrgeorge.sqlx4k.ConnectionPool
import io.github.smyrgeorge.sqlx4k.Driver
import io.github.smyrgeorge.sqlx4k.postgres.postgreSQL
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind

fun start() {
    val db = postgreSQL(
        url = "postgresql://localhost:35432/test",
        username = "postgres",
        password = "postgres",
        options = ConnectionPool.Options.builder()
            .maxConnections(10)
            .build()
    )

    Application(
        name = "io.github.smyrgeorge.ktorlib.example.Application",
        host = "localhost",
        port = 8080,
        configure = {
            tracing {
                appenders.register(SimpleConsoleTracingAppender())
            }

            di {
                single { db }.bind<Driver>()

                singleOf(::TestRestHandler) { bind<AbstractRestHandler>() }
                singleOf(::TestService)
                single { TestRepositoryImpl }.bind<TestRepository>()

                singleOf(::ApplicationStatusRestHandler) { bind<AbstractRestHandler>() }
            }
        },
        postConfigure = {
            val db = di.get<Driver>()
            db.migrate(
                path = "./src/db/migrations",
                afterFileMigration = { m, d ->
                    log.info { "Applied migration $m to database (took $d)" }
                }
            ).getOrThrow()
        }
    ).start(wait = true)
}
