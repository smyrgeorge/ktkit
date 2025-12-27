package io.github.smyrgeorge.ktorlib.example.test

import io.github.smyrgeorge.ktorlib.service.auditable.Auditable
import io.github.smyrgeorge.ktorlib.util.SYSTEM_USER
import io.github.smyrgeorge.sqlx4k.ResultSet
import io.github.smyrgeorge.sqlx4k.annotation.Id
import io.github.smyrgeorge.sqlx4k.annotation.Table
import io.github.smyrgeorge.sqlx4k.impl.extensions.asInstant
import io.github.smyrgeorge.sqlx4k.impl.extensions.asInt
import io.github.smyrgeorge.sqlx4k.impl.extensions.asUuid
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid
import io.github.smyrgeorge.sqlx4k.RowMapper as IRowMapper

@Table("test")
data class Test(
    @Id(insert = true)
    override val id: Int,
    override var createdAt: Instant = Clock.System.now(),
    override var createdBy: Uuid = SYSTEM_USER.uuid,
    override var updatedAt: Instant = createdAt,
    override var updatedBy: Uuid = createdBy,
    val test: String,
) : Auditable<Int> {
    object RowMapper : IRowMapper<Test> {
        override fun map(row: ResultSet.Row): Test {
            return Test(
                id = row.get("id").asInt(),
                createdAt = row.get("created_at").asInstant(),
                createdBy = row.get("created_by").asUuid(),
                updatedAt = row.get("updated_at").asInstant(),
                updatedBy = row.get("updated_by").asUuid(),
                test = row.get("test").asString(),
            )
        }
    }
}
