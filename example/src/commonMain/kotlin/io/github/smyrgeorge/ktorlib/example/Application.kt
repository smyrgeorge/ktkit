package io.github.smyrgeorge.ktorlib.example

import io.github.smyrgeorge.ktorlib.Application
import io.github.smyrgeorge.ktorlib.api.rest.AbstractRestHandler
import io.github.smyrgeorge.ktorlib.api.rest.auth.impl.XRealNamePrincipalExtractor
import io.github.smyrgeorge.ktorlib.api.rest.impl.ApplicationStatusRestHandler
import io.github.smyrgeorge.ktorlib.example.test.ArrowTestRepository
import io.github.smyrgeorge.ktorlib.example.test.ArrowTestRepositoryImpl
import io.github.smyrgeorge.ktorlib.example.test.TestRestHandler
import io.github.smyrgeorge.ktorlib.example.test.TestService
import io.github.smyrgeorge.ktorlib.example.user.UserRepository
import io.github.smyrgeorge.ktorlib.example.user.UserRepositoryImpl
import io.github.smyrgeorge.ktorlib.example.user.UserRestHandler
import io.github.smyrgeorge.ktorlib.example.user.UserService
import io.github.smyrgeorge.ktorlib.example.user.UserServiceImpl
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
            withAuthenticationExtractor(XRealNamePrincipalExtractor())

            tracing {
                appenders.register(SimpleConsoleTracingAppender())
            }

            di {
                single { db }.bind<Driver>()

                singleOf(::UserRestHandler) { bind<AbstractRestHandler>() }
                singleOf(::UserServiceImpl) { bind<UserService>() }
                singleOf(::UserRepositoryImpl) { bind<UserRepository>() }

                singleOf(::TestRestHandler) { bind<AbstractRestHandler>() }
                singleOf(::TestService)
//                single { TestRepositoryImpl }.bind<TestRepository>()
                single { ArrowTestRepositoryImpl }.bind<ArrowTestRepository>()

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
