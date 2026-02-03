package io.github.smyrgeorge.ktkit.sqlx4k

import io.github.smyrgeorge.ktkit.service.Auditable
import io.github.smyrgeorge.sqlx4k.QueryExecutor

/**
 * Represents a database service that performs operations involving entities implementing the [Auditable] interface.
 * This service provides convenient methods for persisting and managing auditable entities while ensuring proper
 * handling of audit-related fields such as `createdAt`, `createdBy`, `updatedAt`, and `updatedBy`.
 *
 * @param T The type of the entity managed by this service. Must implement the [Auditable] interface.
 */
interface AuditableDatabaseService<T : Auditable<*>> : DatabaseService {
    val repo: AuditableRepository<T>

    context(_: QueryExecutor)
    suspend fun T.save() = repo.save(this)
}
