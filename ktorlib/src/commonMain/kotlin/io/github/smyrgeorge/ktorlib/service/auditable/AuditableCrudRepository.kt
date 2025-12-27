package io.github.smyrgeorge.ktorlib.service.auditable

import io.github.smyrgeorge.ktorlib.context.Context
import io.github.smyrgeorge.sqlx4k.arrow.ArrowContextCrudRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalContextParameters::class, ExperimentalUuidApi::class)
interface AuditableCrudRepository<T : Auditable<*>> : ArrowContextCrudRepository<T> {
    override suspend fun preInsertHook(entity: T): T {
        val user = ctx().user
        entity.createdAt = Clock.System.now()
        entity.createdBy = user.uuid
        entity.updatedAt = entity.createdAt
        entity.updatedBy = entity.createdBy
        return entity
    }

    override suspend fun preUpdateHook(entity: T): T {
        entity.updatedAt = Clock.System.now()
        entity.updatedBy = ctx().user.uuid
        return entity
    }


    /**
     * Extracts the `Context` from the current coroutine context.
     *
     * This method retrieves the `Context` element stored within the coroutine context
     * and ensures that it is present. If the `Context` is missing or not properly initialized,
     * an error is thrown to signify an unexpected state.
     *
     * @return The `Context` instance associated with the current coroutine context.
     * @throws IllegalStateException If the `Context` is not present in the current coroutine context.
     */
    private suspend fun ctx(): Context =
        currentCoroutineContext()[Context.Companion] ?: error("Sanity check :: could not extract Context.")
}