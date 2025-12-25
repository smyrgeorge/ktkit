package io.github.smyrgeorge.ktorlib.service.auditable

import kotlin.time.Instant

interface Auditable {
    var id: Int
    var createdAt: Instant
    var updatedAt: Instant
    var createdBy: String
    var updatedBy: String
}