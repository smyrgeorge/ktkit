package io.github.smyrgeorge.ktkit.sqlx4k.pgmq

import io.github.smyrgeorge.sqlx4k.postgres.IPostgresSQL
import io.github.smyrgeorge.sqlx4k.postgres.pgmq.PgmqClient
import io.github.smyrgeorge.sqlx4k.postgres.pgmq.impl.PgmqDbAdapterImpl

/**
 * A wrapper class that provides access to PostgreSQL message queue (PGMQ) functionalities.
 *
 * This class initializes the necessary parts to interact with a PostgreSQL-backed
 * message queue system. It leverages a database adapter and a PGMQ client to enable
 * message publishing and consumption.
 *
 * @constructor Creates a new instance of the `Pgmq` class.
 * @param pg An implementation of the `IPostgresSQL` interface used for database operations.
 *
 * Properties:
 * - `client`: The primary interface for interacting with the PostgreSQL message queue.
 */
class Pgmq(
    pg: IPostgresSQL,
    options: PgmqClient.Options = PgmqClient.Options()
) {
    private val adapter = PgmqDbAdapterImpl(pg)
    val client: PgmqClient = PgmqClient(adapter, options)
}
