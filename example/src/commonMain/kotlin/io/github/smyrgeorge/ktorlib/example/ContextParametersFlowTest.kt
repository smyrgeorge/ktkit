package io.github.smyrgeorge.ktorlib.example

import io.github.smyrgeorge.sqlx4k.Transaction
import io.github.smyrgeorge.sqlx4k.postgres.IPostgresSQL

// Implementation using Context Parameters.
class CPTestService(
    private val pg: IPostgresSQL,
    private val ts1: CPTestService1,
    private val ts2: CPTestService2,
) {
    suspend fun test(): Unit = pg.transaction { test() }

    context(db: Transaction)
    suspend fun test() {
        ts1.test1()
        ts2.test2()
    }
}

class CPTestService1(
    private val ts2: CPTestService2
) {
    context(db: Transaction)
    suspend fun test1() {
        db.fetchAll("SELECT * FROM users").getOrThrow()
        ts2.test2()
    }
}

class CPTestService2(
) {
    context(db: Transaction)
    suspend fun test2() {
        db.fetchAll("SELECT * FROM users").getOrThrow()
    }
}
