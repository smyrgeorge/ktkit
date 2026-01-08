import io.github.smyrgeorge.ktkit.example.start
import io.github.smyrgeorge.ktkit.sqlx4k.pgmq.Pgmq
import io.github.smyrgeorge.log4k.Logger
import io.github.smyrgeorge.sqlx4k.ConnectionPool
import io.github.smyrgeorge.sqlx4k.postgres.pgmq.PgmqClient
import io.github.smyrgeorge.sqlx4k.postgres.postgreSQL
import kotlinx.coroutines.runBlocking

fun main() {
    val log = Logger.of("MainKt")

    val db = postgreSQL(
        url = "postgresql://localhost:35432/test",
        username = "postgres",
        password = "postgres",
        options = ConnectionPool.Options.builder()
            .maxConnections(10)
            .build()
    )

    val pgmq = Pgmq(db, options = PgmqClient.Options(verifyInstallation = false))

    runBlocking {
        db.migrate(
            path = "src/commonMain/resources/db/migrations",
            afterFileMigration = { m, d ->
                log.info { "Applied migration $m to database (took $d)" }
            }
        ).getOrThrow()

        pgmq.client.installFromPath("src/commonMain/resources/db/pgmq/migrations")
    }

    start(db)
}
