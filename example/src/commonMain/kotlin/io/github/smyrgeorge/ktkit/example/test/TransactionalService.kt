package io.github.smyrgeorge.ktkit.example.test

import arrow.core.raise.Raise
import arrow.core.raise.context.raise
import io.github.smyrgeorge.ktkit.api.error.ErrorSpec
import io.github.smyrgeorge.ktkit.api.error.impl.NotFound
import io.github.smyrgeorge.ktkit.sqlx4k.AuditableDatabaseService
import io.github.smyrgeorge.ktkit.sqlx4k.DatabaseService.Companion.db
import io.github.smyrgeorge.sqlx4k.Driver
import io.github.smyrgeorge.sqlx4k.Transaction
import kotlinx.atomicfu.atomic


data class TestFail(
    override val message: String,
    override val httpStatus: ErrorSpec.HttpStatus = ErrorSpec.HttpStatus.INTERNAL_SERVER_ERROR
) : ErrorSpec

class TransactionalService(
    override val db: Driver,
    override val repo: TestRepository,
): AuditableDatabaseService<Test> {
    private val updateSequence = atomic(0)
    private val createSequence = atomic(0)


    context(_: Transaction, _: Raise<ErrorSpec>)
    suspend fun create(): Test {
        val sequence = createSequence.incrementAndGet()
        val newName = "created_tx_$sequence"
        val newTest = Test(
            id = 0,
            test = newName,
            data = Test.Data(test = newName)
        )
        return db { repo.save(newTest) }
    }

    context(_: Transaction, _: Raise<ErrorSpec>)
    suspend fun update(id: Int): Test {
        val test = db { repo.findOneById(id) } ?: raise(NotFound("Test $id not found"))

        val sequence = updateSequence.incrementAndGet()
        val txIndex = test.test.lastIndexOf("_tx_")
        val baseName = if (txIndex >= 0 && test.test.substring(txIndex + 4).toIntOrNull() != null) {
            test.test.substring(0, txIndex)
        } else {
            test.test
        }
        val newName = "${baseName}_tx_$sequence"

        val updated = test.copy(test = newName, data = Test.Data(test = newName))
        return db { repo.save(updated) }
    }

    context(_: Transaction, _: Raise<ErrorSpec>)
    suspend fun updateMultiple(id: Int): Test {
        update(id)
        update(id)
        return update(id)
    }
}
