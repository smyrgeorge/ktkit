@file:Suppress("unused")

package io.github.smyrgeorge.ktorlib.util.extentions

import io.github.smyrgeorge.ktorlib.domain.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

object EmptyScope : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = EmptyCoroutineContext
}

/**
 * Launches a new coroutine in the context of [EmptyScope] with [Dispatchers.IO_DISPATCHER].
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
    EmptyScope.launch(Dispatchers.IO_DISPATCHER) { f() }

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
inline fun launch(ctx: Context, crossinline f: suspend () -> Unit): Job =
    EmptyScope.launch(Dispatchers.IO_DISPATCHER) { withContext(ctx) { f() } }


@Suppress("UnusedReceiverParameter")
val Dispatchers.IO_DISPATCHER get() = Dispatchers.IO