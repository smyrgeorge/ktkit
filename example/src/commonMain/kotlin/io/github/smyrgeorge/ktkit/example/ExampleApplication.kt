package io.github.smyrgeorge.ktkit.example

import io.github.smyrgeorge.ktkit.Application
import io.github.smyrgeorge.ktkit.api.rest.AbstractRestHandler
import io.github.smyrgeorge.ktkit.example.generated.TestRepositoryImpl
import io.github.smyrgeorge.ktkit.example.test.TestRepository
import io.github.smyrgeorge.ktkit.example.test.TestRestHandler
import io.github.smyrgeorge.ktkit.example.test.TestService
import io.github.smyrgeorge.ktkit.sqlx4k.pgmq.Pgmq
import io.github.smyrgeorge.log4k.Level
import io.github.smyrgeorge.sqlx4k.Driver
import io.github.smyrgeorge.sqlx4k.postgres.IPostgresSQL
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind

class ExampleApplication

fun start(db: IPostgresSQL, pgmq: Pgmq) {
    Application(
        name = ExampleApplication::class.simpleName!!,
        description = "This is an example application",
        conf = Application.Conf(
            host = "localhost",
            port = 8080,
        ),
        configure = {
            logging {
                // Configure logging.
                level = Level.INFO
                // Log in JSON format:
                // SimpleJsonConsoleLoggingAppender.install()
            }
            tracing {
                // Configure tracing.
            }
            metering {
                // Configure metering.
            }
            json {
                // Configure JSON serialization.
            }
            ktor {
                // Additional Ktor configuration.
            }
            di {
                single { db }.bind<Driver>()
                single { pgmq }.bind<Pgmq>()
                singleOf(::TestRestHandler) { bind<AbstractRestHandler>() }
                singleOf(::TestService)
                single { TestRepositoryImpl }.bind<TestRepository>()
            }
        },
        postConfigure = {
            // After configuration, perform any necessary post-configuration tasks.
        }
    ).start()
}
