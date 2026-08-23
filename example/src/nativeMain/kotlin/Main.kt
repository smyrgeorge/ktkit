import io.github.smyrgeorge.ktkit.api.props.ConfigPropertiesToml
import io.github.smyrgeorge.ktkit.example.ExampleApplication
import io.github.smyrgeorge.ktkit.example.config.Props
import io.github.smyrgeorge.ktkit.example.start
import io.github.smyrgeorge.ktkit.example.test.Test
import io.github.smyrgeorge.ktkit.sqlx4k.JsonSupport
import io.github.smyrgeorge.ktkit.sqlx4k.pgmq.Pgmq
import io.github.smyrgeorge.log4k.Logger
import io.github.smyrgeorge.log4k.context.classic
import io.github.smyrgeorge.log4k.context.info
import io.github.smyrgeorge.sqlx4k.ConnectionPool
import io.github.smyrgeorge.sqlx4k.postgres.pgmq.PgmqClient
import io.github.smyrgeorge.sqlx4k.postgres.postgreSQL
import kotlinx.coroutines.runBlocking

fun main() {
    val log = Logger.of(ExampleApplication::class)

    val props = ConfigPropertiesToml.loadFromFileSystem<Props>("src/commonMain/resources/application.toml")

    val db = postgreSQL(
        url = props.database.url,
        username = props.database.username,
        password = props.database.password,
        options = ConnectionPool.Options.builder()
            .maxConnections(props.database.maxConnections)
            .build(),
        encoders = JsonSupport.encoders(
            types = setOf(
                Test.Data::class
            )
        )
    )

    runBlocking {
        db.migrate(
            path = "src/commonMain/resources/db/migrations",
            afterFileMigration = { m, d ->
                log.classic.info { "Applied migration $m to database (took $d)" }
            }
        ).getOrThrow()

        val pgmq = Pgmq(db, options = PgmqClient.Options(verifyInstallation = false))
        pgmq.client.installFromPath("src/commonMain/resources/db/pgmq/migrations")
    }

    val pgmq = Pgmq(db)

    start(props, db, pgmq)
}
