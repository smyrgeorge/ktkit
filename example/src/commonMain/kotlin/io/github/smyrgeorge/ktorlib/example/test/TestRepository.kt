package io.github.smyrgeorge.ktorlib.example.test

import io.github.smyrgeorge.ktorlib.service.auditable.AuditablRepository
import io.github.smyrgeorge.sqlx4k.QueryExecutor
import io.github.smyrgeorge.sqlx4k.annotation.Query
import io.github.smyrgeorge.sqlx4k.annotation.Repository
import io.github.smyrgeorge.sqlx4k.arrow.impl.extensions.DbResult

@Repository
interface TestRepository : AuditablRepository<Test> {
    @Query("SELECT * FROM test")
    context(context: QueryExecutor)
    suspend fun findAll(): DbResult<List<Test>>
}
