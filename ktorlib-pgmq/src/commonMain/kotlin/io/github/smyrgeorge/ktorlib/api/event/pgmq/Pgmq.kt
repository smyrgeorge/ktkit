package io.github.smyrgeorge.ktorlib.api.event.pgmq

import io.github.smyrgeorge.sqlx4k.postgres.IPostgresSQL
import io.github.smyrgeorge.sqlx4k.postgres.pgmq.PgMqClient
import io.github.smyrgeorge.sqlx4k.postgres.pgmq.impl.PgMqDbAdapterImpl

class Pgmq(pg: IPostgresSQL) {
    private val adapter = PgMqDbAdapterImpl(pg)
    val client = PgMqClient(adapter)
}
