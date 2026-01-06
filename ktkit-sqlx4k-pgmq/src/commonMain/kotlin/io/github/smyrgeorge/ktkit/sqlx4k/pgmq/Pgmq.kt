package io.github.smyrgeorge.ktkit.sqlx4k.pgmq

import io.github.smyrgeorge.sqlx4k.postgres.IPostgresSQL
import io.github.smyrgeorge.sqlx4k.postgres.pgmq.PgMqClient
import io.github.smyrgeorge.sqlx4k.postgres.pgmq.impl.PgMqDbAdapterImpl

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
    options: PgMqClient.Options = PgMqClient.Options()
) {
    private val adapter = PgMqDbAdapterImpl(pg)
    val client: PgMqClient = PgMqClient(adapter, options)
}
