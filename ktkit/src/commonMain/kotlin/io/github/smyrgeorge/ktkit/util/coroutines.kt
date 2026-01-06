@file:Suppress("unused")

package io.github.smyrgeorge.ktkit.util

import io.github.smyrgeorge.ktkit.context.ExecContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * A platform-specific implementation of [CoroutineDispatcher] intended for IO-intensive operations.
 *
 * This dispatcher is optimized to handle a large number of concurrent coroutines that involve
 * blocking IO tasks such as file operations or network requests. It allows you to dispatch
 * coroutines onto a background thread pool designed for such tasks, enabling efficient use
 * of system resources and ensuring that the main thread is not blocked.
 *
 * Suitable uses for this dispatcher include:
 * - Reading or writing to files
 * - Making network requests
 * - Interacting with databases
 * - Other high-latency or blocking IO tasks
 *
 * It is an expected property, meaning its actual implementation may vary depending on the
 * platform (e.g., JVM, Native, JS).
 */
expect val Dispatchers.IO_DISPATCHER: CoroutineDispatcher

/**
 * A global singleton implementation of [CoroutineScope] providing a shared coroutine context.
 *
 * This object serves as a convenient and centralized scope for launching coroutines,
 * typically used in scenarios where a shared scope with an empty coroutine context is required.
 *
 * The purpose of `GlobalCoroutineScope` is to provide a universally accessible scope for
 * lightweight, independent coroutine launches that are decoupled from any specific lifecycle.
 * It guarantees that the `coroutineContext` remains empty, ensuring no additional dispatchers
 * or contexts override the behavior of child coroutines launched within this scope.
 *
 * Note that any coroutines launched through this scope will not be cancelled automatically when
 * the creator object goes out of scope or is destroyed. Therefore, it is important to manage
 * the lifecycle of the launched coroutines manually, if necessary.
 */
object GlobalCoroutineScope : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = EmptyCoroutineContext
}

/**
 * Executes a suspending block of code in the IO context.
 *
 * @param f The suspending function to be executed within the CoroutineScope.
 * @return The result of the function executed within the IO context.
 */
suspend inline fun <T> io(crossinline f: suspend CoroutineScope.() -> T): T =
    withContext(Dispatchers.IO_DISPATCHER) { f() }

/**
 * Launches a new coroutine in the context of [GlobalCoroutineScope] with [Dispatchers.IO_DISPATCHER].
 *
 * This function is particularly useful for running a given suspending function block
 * concurrently in a separate coroutine without blocking the current thread. The coroutine
 * is launched using the specified dispatcher which is optimized for handling numerous
 * concurrent tasks efficiently.
 *
 * @param f The suspending function to be executed in the newly launched coroutine.
 * @return A [Job] that represents the coroutine. The job can be used to control the lifecycle
 *         of the coroutine, such as cancelling its execution if needed.
 */
inline fun launch(crossinline f: suspend () -> Unit): Job =
    GlobalCoroutineScope.launch(Dispatchers.IO_DISPATCHER) { f() }

/**
 * Launches a coroutine with the given context and suspending function.
 *
 * This method creates a coroutine within the `EmptyScope` and dispatches
 * it to the `Dispatchers.IO_DISPATCHER` dispatcher. The provided context is used
 * to execute the specified suspending function.
 *
 * @param ctx The custom coroutine context in which the suspending function will run.
 * @param f The suspending function to be executed.
 * @return A `Job` representing the coroutine.
 */
inline fun launch(ctx: ExecContext, crossinline f: suspend () -> Unit): Job =
    GlobalCoroutineScope.launch(Dispatchers.IO_DISPATCHER) { withContext(ctx) { f() } }
