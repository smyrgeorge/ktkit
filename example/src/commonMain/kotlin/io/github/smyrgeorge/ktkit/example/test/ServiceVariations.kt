package io.github.smyrgeorge.ktkit.example.test

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.context.raise
import arrow.core.raise.either
import io.github.smyrgeorge.ktkit.api.error.ErrorSpec
import io.github.smyrgeorge.ktkit.api.error.impl.NotFound
import io.github.smyrgeorge.ktkit.sqlx4k.AuditableDatabaseService
import io.github.smyrgeorge.ktkit.sqlx4k.DatabaseService.Companion.db
import io.github.smyrgeorge.sqlx4k.Driver
import io.github.smyrgeorge.sqlx4k.Transaction

class ServiceVariations(
    override val db: Driver,
    override val repo: TestRepository,
) : AuditableDatabaseService<Test> {
    // NOTICE FOR MR REVIEW:
    // In sqlx4k transaction(), rollback is triggered when:
    // 1) an exception escapes the transaction block, or
    // 2) the returned value is Result.Failure (transaction() explicitly checks this case).
    // It does NOT rollback automatically for a plain Either.Left return value.
    //
    // The three methods below intentionally demonstrate those behaviors.
    context(_: Transaction)
    suspend fun findAllResult(fail: Boolean = false): Result<List<Test>> =
        runCatching {
            if (fail) NotFound("No tests found (result variation).").throwRuntimeError()
            repo.findAll().fold(
                ifLeft = { error -> throw RuntimeException("Database query failed: $error") },
                ifRight = { tests -> tests },
            )
        }

    // NOTE:
    // Returning Either.Left by itself DOES NOT mark transaction rollback in sqlx4k (for now).
    // Rollback would require converting Left to a thrown error at the transaction boundary.
    context(_: Transaction)
    suspend fun findAllEither(fail: Boolean = false): Either<ErrorSpec, List<Test>> =
        either {
            if (fail) raise(NotFound("No tests found (either variation)."))
            db { repo.findAll() }
        }

    // NOTE:
    // raise(...) short-circuits with exceptional control flow, so sqlx4k transaction() rolls back.
    context(_: Raise<ErrorSpec>, _: Transaction)
    suspend fun findAllRaise(fail: Boolean = false): List<Test> {
        if (fail) raise(NotFound("No tests found (raise variation)."))
        return db { repo.findAll() }
    }
}
