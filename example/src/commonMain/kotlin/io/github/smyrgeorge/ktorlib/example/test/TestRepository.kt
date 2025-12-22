package io.github.smyrgeorge.ktorlib.example.test

import io.github.smyrgeorge.sqlx4k.ContextCrudRepository
import io.github.smyrgeorge.sqlx4k.QueryExecutor
import io.github.smyrgeorge.sqlx4k.annotation.Query
import io.github.smyrgeorge.sqlx4k.annotation.Repository

@OptIn(ExperimentalContextParameters::class)
@Repository(mapper = Test.RowMapper::class)
interface TestRepository : ContextCrudRepository<Test> {
    @Query("SELECT * FROM test")
    context(context: QueryExecutor)
    suspend fun findAll(): Result<List<Test>>
}
