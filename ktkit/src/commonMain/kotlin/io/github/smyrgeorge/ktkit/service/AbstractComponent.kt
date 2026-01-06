package io.github.smyrgeorge.ktkit.service

import io.github.smyrgeorge.ktkit.context.ExecutionContext
import kotlinx.coroutines.currentCoroutineContext

/**
 * Represents a foundational abstraction for shared functionality across components.
 *
 * The purpose of this interface is to provide common utilities and streamline
 * context handling for components that implement it. This abstraction enables easier
 * integration with coroutine contexts and ensures consistency in how application-specific
 * contexts, such as [ExecutionContext], are managed.
 *
 * Implementing classes can rely on this abstraction to access and validate the presence of
 * an [ExecutionContext] within the current coroutine context.
 */
interface AbstractComponent {
    /**
     * Retrieves the [io.github.smyrgeorge.ktkit.context.ExecutionContext] from the current CoroutineContext.
     *
     * @return The [io.github.smyrgeorge.ktkit.context.ExecutionContext] if it exists within the current CoroutineContext.
     * @throws IllegalStateException if the [io.github.smyrgeorge.ktkit.context.ExecutionContext] cannot be extracted from the CoroutineContext.
     */
    suspend fun ctx(): ExecutionContext =
        currentCoroutineContext()[ExecutionContext] ?: error("No ExecutionContext found in CoroutineContext")
}