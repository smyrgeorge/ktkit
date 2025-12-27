package io.github.smyrgeorge.ktorlib.example.test

import io.github.smyrgeorge.ktorlib.service.auditable.AuditableCrudRepository
import io.github.smyrgeorge.sqlx4k.QueryExecutor
import io.github.smyrgeorge.sqlx4k.annotation.Query
import io.github.smyrgeorge.sqlx4k.annotation.Repository
import io.github.smyrgeorge.sqlx4k.arrow.impl.extensions.DbResult

@OptIn(ExperimentalContextParameters::class)
// language=SQL
@Repository(mapper = Test.RowMapper::class)
interface TestRepository : AuditableCrudRepository<Test> {
    @Query("SELECT * FROM test")
    context(context: QueryExecutor)
    suspend fun findAll(): DbResult<List<Test>>
}
