package io.github.smyrgeorge.ktkit.example

import io.github.smyrgeorge.ktkit.Application
import io.github.smyrgeorge.ktkit.api.rest.AbstractRestHandler
import io.github.smyrgeorge.ktkit.example.generated.TestRepositoryImpl
import io.github.smyrgeorge.ktkit.example.test.TestRepository
import io.github.smyrgeorge.ktkit.example.test.TestRestHandler
import io.github.smyrgeorge.ktkit.example.test.TestService
import io.github.smyrgeorge.ktkit.util.get
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
        name = "io.github.smyrgeorge.ktkit.example.Application",
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
    ).start()
}
