package io.github.smyrgeorge.ktkit.sqlx4k

import io.github.smyrgeorge.ktkit.service.Auditable
import io.github.smyrgeorge.sqlx4k.QueryExecutor

interface AuditableDatabaseService<T : Auditable<*>> : DatabaseService {
    val repo: AuditableRepository<T>

    context(_: QueryExecutor)
    suspend fun T.save() = repo.save(this)
}
