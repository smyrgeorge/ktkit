package io.github.smyrgeorge.ktorlib.example.test

import arrow.core.Either
import io.github.smyrgeorge.ktorlib.example.generated.TestRepositoryImpl
import io.github.smyrgeorge.ktorlib.util.toEither
import io.github.smyrgeorge.sqlx4k.ContextCrudRepository
import io.github.smyrgeorge.sqlx4k.QueryExecutor
import io.github.smyrgeorge.sqlx4k.SQLError
import io.github.smyrgeorge.sqlx4k.annotation.Query
import io.github.smyrgeorge.sqlx4k.annotation.Repository

@OptIn(ExperimentalContextParameters::class)
@Repository(mapper = Test.RowMapper::class)
interface TestRepository : ContextCrudRepository<Test> {
    @Query("SELECT * FROM test")
    context(context: QueryExecutor)
    suspend fun findAll(): Result<List<Test>>
}

typealias DbResult<T> = Either<SQLError, T>

interface ArrowTestRepository {
    context(context: QueryExecutor)
    suspend fun findAll(): DbResult<List<Test>>
}

@Suppress("OVERRIDE_BY_INLINE")
object ArrowTestRepositoryImpl : ArrowTestRepository {
    context(context: QueryExecutor)
    override suspend inline fun findAll(): DbResult<List<Test>> = TestRepositoryImpl.findAll().toSqlx4kEither()
}

fun <T> Result<T>.toSqlx4kEither(): DbResult<T> =
    toEither().mapLeft {
        when (it) {
            is SQLError -> it
            else -> SQLError(SQLError.Code.Database, it.message)
        }
    }
