package io.github.smyrgeorge.ktkit.service.auditable

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
interface Auditable<ID> {
    val id: ID
    var createdAt: Instant
    var createdBy: Uuid
    var updatedAt: Instant
    var updatedBy: Uuid
}