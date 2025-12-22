package io.github.smyrgeorge.ktorlib.example

import io.github.smyrgeorge.sqlx4k.impl.coroutines.TransactionContext
import io.github.smyrgeorge.sqlx4k.postgres.IPostgresSQL

// Implementation using Coroutine Context.
class CTestService(
    private val pg: IPostgresSQL,
    private val ts1: CPTestService1,
    private val ts2: CPTestService2,
) {
    suspend fun test() = TransactionContext.withCurrent(pg) {
        ts1.test1()
        ts2.test2()
    }
}

class CTestService1(
    private val db: IPostgresSQL,
    private val ts2: CPTestService2
) {
    suspend fun test1() = TransactionContext.withCurrent(db) {
        fetchAll("SELECT * FROM users").getOrThrow()
        ts2.test2()
    }
}

class CTestService2 {
    suspend fun test2() = TransactionContext.withCurrent {
        fetchAll("SELECT * FROM users").getOrThrow()
    }
}
