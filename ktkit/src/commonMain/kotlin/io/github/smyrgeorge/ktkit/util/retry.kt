package io.github.smyrgeorge.ktkit.util

import io.github.smyrgeorge.log4k.Logger
import kotlinx.coroutines.delay

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
 * Executes a suspending block of code with retry logic, wrapping the result in a [Result] object.
 *
 * @param times The maximum number of retry attempts. Default is 3.
 * @param initialDelay The initial delay in milliseconds before retrying. Default is 100ms.
 * @param maxDelay The maximum delay in milliseconds between retry attempts. Default is 1000ms.
 * @param factor The backoff factor by which the delay increases after each retry. Default is 2.0.
 * @param log The logger used to log retry attempts and exceptions.
 * @param block The suspending block of code to be executed with retries.
 * @return A [Result] object containing the successful result of the block or the exception if all retries fail.
 */
suspend fun <T> retryCatching(
    times: Int = 3,
    initialDelay: Long = 100, // 0.1 second
    maxDelay: Long = 1000,    // 1 second
    factor: Double = 2.0,
    log: Logger = io.github.smyrgeorge.ktkit.util.log,
    block: suspend () -> T
): Result<T> = runCatching { retry(times, initialDelay, maxDelay, factor, log, block) }
