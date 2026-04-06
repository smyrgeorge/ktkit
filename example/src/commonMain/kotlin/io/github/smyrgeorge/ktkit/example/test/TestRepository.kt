package io.github.smyrgeorge.ktkit.example.test

import io.github.smyrgeorge.ktkit.sqlx4k.AuditableRepository
import io.github.smyrgeorge.sqlx4k.QueryExecutor
import io.github.smyrgeorge.sqlx4k.annotation.Query
import io.github.smyrgeorge.sqlx4k.annotation.Repository
import io.github.smyrgeorge.sqlx4k.arrow.impl.extensions.DbResult

@Repository
interface TestRepository : AuditableRepository<Test> {
    @Query("SELECT * FROM test")
    context(context: QueryExecutor)
    suspend fun findAll(): DbResult<List<Test>>


    @Query("SELECT id, created_at, created_by, updated_at, updated_by, test, data FROM test WHERE id = :id")
    context(context: QueryExecutor)
    suspend fun findOneById(id: Int): DbResult<Test?>
}
