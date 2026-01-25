package io.github.smyrgeorge.ktkit.example

import io.github.smyrgeorge.ktkit.example.test.Test
import io.github.smyrgeorge.ktkit.sqlx4k.JsonSupport
import io.github.smyrgeorge.ktkit.sqlx4k.pgmq.Pgmq
import io.github.smyrgeorge.log4k.Logger
import io.github.smyrgeorge.sqlx4k.ConnectionPool
import io.github.smyrgeorge.sqlx4k.impl.migrate.utils.readMigrationFilesFromResources
import io.github.smyrgeorge.sqlx4k.impl.migrate.utils.readSqlFilesFromResources
import io.github.smyrgeorge.sqlx4k.postgres.pgmq.PgmqClient
import io.github.smyrgeorge.sqlx4k.postgres.postgreSQL
import kotlinx.coroutines.runBlocking

fun main() {
    val log = Logger.of(ExampleApplication::class)

    val db = postgreSQL(
        url = "postgresql://localhost:35432/test",
        username = "postgres",
        password = "postgres",
        options = ConnectionPool.Options.builder()
            .maxConnections(10)
            .build(),
        encoders = JsonSupport.encoders(
            types = setOf(
                Test.Data::class
            )
        )
    )

    runBlocking {
        db.migrate(
            supplier = { readMigrationFilesFromResources("db/migrations") },
            afterFileMigration = { m, d ->
                log.info { "Applied migration $m to database (took $d)" }
            }
        ).getOrThrow()

        val pgmq = Pgmq(db, options = PgmqClient.Options(verifyInstallation = false))
        val pgmqFiles = readSqlFilesFromResources("db/pgmq/migrations")
        pgmq.client.installFromSqlFiles(pgmqFiles)
    }

    val pgmq = Pgmq(db)

    start(db, pgmq)
}
