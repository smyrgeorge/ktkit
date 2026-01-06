package io.github.smyrgeorge.ktkit.example.test

import io.github.smyrgeorge.ktkit.Application.Companion.SYSTEM_USER
import io.github.smyrgeorge.ktkit.service.auditable.Auditable
import io.github.smyrgeorge.sqlx4k.annotation.Id
import io.github.smyrgeorge.sqlx4k.annotation.Table
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Table("test")
data class Test(
    @Id
    override val id: Int,
    override var createdAt: Instant = Clock.System.now(),
    override var createdBy: Uuid = SYSTEM_USER.id,
    override var updatedAt: Instant = createdAt,
    override var updatedBy: Uuid = createdBy,
    val test: String,
) : Auditable<Int>
