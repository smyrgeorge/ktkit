package io.github.smyrgeorge.ktkit.service

import kotlin.time.Instant
import kotlin.uuid.Uuid

interface Auditable<ID> {
    val id: ID
    var createdAt: Instant
    var createdBy: Uuid
    var updatedAt: Instant
    var updatedBy: Uuid
}