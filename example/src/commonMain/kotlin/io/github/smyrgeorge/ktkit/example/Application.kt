package io.github.smyrgeorge.ktkit.example

import io.github.smyrgeorge.ktkit.Application
import io.github.smyrgeorge.ktkit.api.rest.AbstractRestHandler
import io.github.smyrgeorge.ktkit.example.generated.TestRepositoryImpl
import io.github.smyrgeorge.ktkit.example.test.TestRepository
import io.github.smyrgeorge.ktkit.example.test.TestRestHandler
import io.github.smyrgeorge.ktkit.example.test.TestService
import io.github.smyrgeorge.sqlx4k.Driver
import io.github.smyrgeorge.sqlx4k.postgres.IPostgresSQL
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind

fun start(db: IPostgresSQL) {
    Application(
        name = "io.github.smyrgeorge.ktkit.example.Application",
        conf = Application.Conf(
            host = "localhost",
            port = 8080,
        ),
        configure = {
            tracing {
//                appenders.register(SimpleConsoleTracingAppender())
            }

            di {
                single { db }.bind<Driver>()
                singleOf(::TestRestHandler) { bind<AbstractRestHandler>() }
                singleOf(::TestService)
                single { TestRepositoryImpl }.bind<TestRepository>()
            }
        },
//        postConfigure = {
//            val db = di.get<Driver>()
//            db.migrate(
//                path = "./src/db/migrations",
//                afterFileMigration = { m, d ->
//                    log.info { "Applied migration $m to database (took $d)" }
//                }
//            ).getOrThrow()
//        }
    ).start()
}
