package io.github.smyrgeorge.ktkit.service

import io.github.smyrgeorge.ktkit.context.ExecutionContext
import kotlinx.coroutines.currentCoroutineContext

/**
 * Represents a foundational abstraction used for contextual operations within a coroutine scope.
 *
 * This interface provides utility methods for extracting and working with a [ExecutionContext] object
 * that carries contextual information throughout coroutine-based operations. It serves as a
 * base contract for implementing components that require request-scoped context handling.
 *
 * Typical use cases of this abstraction include:
 * - Extracting user-related or request-related metadata.
 * - Passing contextual information through coroutine scopes.
 * - Enforcing a shared contract for components needing contextual utilities.
 */
interface AbstractComponent {
    /**
     * Retrieves the [io.github.smyrgeorge.ktkit.context.ExecutionContext] from the current CoroutineContext.
     *
     * @return The [io.github.smyrgeorge.ktkit.context.ExecutionContext] if it exists within the current CoroutineContext.
     * @throws IllegalStateException if the [io.github.smyrgeorge.ktkit.context.ExecutionContext] cannot be extracted from the CoroutineContext.
     */
    suspend fun ctx(): ExecutionContext =
        currentCoroutineContext()[ExecutionContext.Companion] ?: error("Sanity check :: could not extract Context.")
}