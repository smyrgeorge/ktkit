package io.github.smyrgeorge.ktkit.service

import io.github.smyrgeorge.ktkit.Application
import io.github.smyrgeorge.ktkit.context.ExecContext
import kotlinx.coroutines.currentCoroutineContext
import org.koin.mp.KoinPlatformTools

/**
 * Represents a foundational abstraction for shared functionality across components.
 *
 * The purpose of this interface is to provide common utilities and streamline
 * context handling for components that implement it. This abstraction enables easier
 * integration with coroutine contexts and ensures consistency in how application-specific
 * contexts, such as [ExecContext], are managed.
 *
 * Implementing classes can rely on this abstraction to access and validate the presence of
 * an [ExecContext] within the current coroutine context.
 */
interface AbstractComponent {
    /**
     * Provides access to the default application instance managed by the Koin dependency injection framework.
     *
     * This property retrieves the [Application] instance defined within the current Koin context.
     * It serves as a convenient way to access the application-level dependency graph.
     *
     * Note that this is a lazily-evaluated property and assumes that a valid Koin context has already
     * been initialized. Accessing this property without proper initialization of Koin might result in
     * an exception.
     *
     * This is typically used within shared components or application-wide abstractions where access
     * to the application's primary context is required.
     */
    val app: Application get() = KoinPlatformTools.defaultContext().get().get()

    /**
     * Retrieves the [io.github.smyrgeorge.ktkit.context.ExecContext] from the current CoroutineContext.
     *
     * @return The [io.github.smyrgeorge.ktkit.context.ExecContext] if it exists within the current CoroutineContext.
     * @throws IllegalStateException if the [io.github.smyrgeorge.ktkit.context.ExecContext] cannot be extracted from the CoroutineContext.
     */
    suspend fun ctx(): ExecContext =
        currentCoroutineContext()[ExecContext] ?: error("No ExecutionContext found in CoroutineContext")
}
