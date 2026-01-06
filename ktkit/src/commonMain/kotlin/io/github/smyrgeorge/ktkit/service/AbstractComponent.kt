package io.github.smyrgeorge.ktkit.service

import io.github.smyrgeorge.ktkit.Application
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
     * Provides access to the singleton instance of the [Application].
     *
     * This property is a global accessor to the application's main instance ([Application.INSTANCE_OR_NULL]).
     * It is used to interact with or retrieve the configuration, state, or other resources
     * managed by the [Application] class. By design, it ensures that only one instance of the
     * application exists during the runtime.
     *
     * This is particularly useful in scenarios where shared access to the [Application] is
     * required across multiple components such as services or handlers that implement or extend
     * [AbstractComponent] or similar abstractions.
     *
     * Note: Ensure to initialize the application properly before accessing this property to avoid
     * runtime errors when the underlying instance is unavailable.
     */
    val app: Application get() = Application.INSTANCE

    /**
     * Retrieves the [io.github.smyrgeorge.ktkit.context.ExecutionContext] from the current CoroutineContext.
     *
     * @return The [io.github.smyrgeorge.ktkit.context.ExecutionContext] if it exists within the current CoroutineContext.
     * @throws IllegalStateException if the [io.github.smyrgeorge.ktkit.context.ExecutionContext] cannot be extracted from the CoroutineContext.
     */
    suspend fun ctx(): ExecutionContext =
        currentCoroutineContext()[ExecutionContext] ?: error("No ExecutionContext found in CoroutineContext")
}
