package io.github.smyrgeorge.ktkit.util

import io.github.smyrgeorge.ktkit.Application
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString
import kotlinx.coroutines.runBlocking
import platform.posix.SIGINT
import platform.posix.atexit
import platform.posix.getenv
import platform.posix.signal

@OptIn(ExperimentalForeignApi::class)
actual fun registerShutdownHook() {
    atexit(staticCFunction<Unit> {
        runBlocking {
            Application.INSTANCE_OR_NULL?.shutdown()
        }
    })
    signal(SIGINT, staticCFunction<Int, Unit> {
        runBlocking {
            Application.INSTANCE_OR_NULL?.shutdown()
            println("Triggered shutdown hook.")
        }
    })
}

actual fun vmMemoryMetrics(): Map<String, Long> = emptyMap()
actual fun vmProcessorsMetrics(): Map<String, Int> = emptyMap()

@OptIn(ExperimentalForeignApi::class)
actual fun getEnv(name: String): String? = getenv(name)?.toKString()
