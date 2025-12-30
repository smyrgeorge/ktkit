package io.github.smyrgeorge.ktorlib.api.mq.pgmq

import io.github.smyrgeorge.sqlx4k.postgres.IPostgresSQL
import io.github.smyrgeorge.sqlx4k.postgres.pgmq.PgMqClient
import io.github.smyrgeorge.sqlx4k.postgres.pgmq.impl.PgMqDbAdapterImpl

class PgMq(pg: IPostgresSQL) {
    private val adapter = PgMqDbAdapterImpl(pg)
    val client = PgMqClient(adapter)
}
