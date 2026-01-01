package io.github.smyrgeorge.ktkit.util

import io.github.smyrgeorge.log4k.Logger
import kotlinx.coroutines.delay
import kotlin.reflect.KClass

private object RetryUtil
private val log = Logger.of(RetryUtil::class)

/**
 * Repeatedly executes a suspending block of code with a specified number of attempts and delay policy until
 * the block succeeds or the attempts are exhausted.
 *
 * @param times The number of times to attempt the execution of the block. Default is 3.
 * @param initialDelay The initial delay in milliseconds before retrying. Default is 100ms.
 * @param maxDelay The maximum delay in milliseconds between attempts. Default is 1000ms.
 * @param factor The backoff factor to multiply the delay by each attempt. Default is 2.0.
 * @param log The logger to use for logging retries
 * @param block The suspending block of code to be executed.
 * @return The result of the block if execution is successful within the given attempts.
 * @throws Exception The last exception thrown by the block if all attempts fail.
 */
suspend fun <T> retry(
    times: Int = 3,
    initialDelay: Long = 100, // 0.1 second
    maxDelay: Long = 1000,    // 1 second
    factor: Double = 2.0,
    log: Logger = io.github.smyrgeorge.ktkit.util.log,
    block: suspend () -> T
): T = io {
    var currentDelay = initialDelay
    repeat(times - 1) {
        try {
            return@io block()
        } catch (e: Exception) {
            log.warn("[${it + 1}/$times] ${e.message}", e)
        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }
    return@io block() // last attempt
}

/**
 * Executes a suspending block of code with a retry mechanism for specified exceptions. The block will be retried
 * up to a specified number of times, with an exponential backoff for delays between attempts.
 *
 * @param times The number of times to attempt the execution of the block. Default is 3.
 * @param initialDelay The initial delay in milliseconds before retrying. Default is 100ms.
 * @param maxDelay The maximum delay in milliseconds between attempts. Default is 1000ms.
 * @param factor The backoff factor to multiply the delay by for each attempt. Default is 2.0.
 * @param log The logger to use for logging retries.
 * @param exceptions The types of exceptions that should trigger a retry. The block will be retried only when
 *                   an exception of one of these types is thrown.
 * @param block The suspending block of code to be executed.
 * @return The result of the block if execution is successful within the given attempts.
 * @throws Exception The last exception thrown by the block if all attempts fail and none of the retries are successful.
 */
suspend fun <T> retryFor(
    times: Int = 3,
    initialDelay: Long = 100, // 0.1 second
    maxDelay: Long = 1000,    // 1 second
    factor: Double = 2.0,
    log: Logger = io.github.smyrgeorge.ktkit.util.log,
    vararg exceptions: KClass<out Exception>,
    block: suspend () -> T
): T = io {
    var currentDelay = initialDelay
    repeat(times - 1) {
        try {
            return@io block()
        } catch (e: Exception) {
            // Check if exception is one of the specified types
            val shouldRetry = exceptions.any { exClass ->
                exClass.isInstance(e)
            }
            if (!shouldRetry) throw e
            log.warn("[${it + 1}/$times] ${e.message}", e)
        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }
    return@io block() // last attempt
}