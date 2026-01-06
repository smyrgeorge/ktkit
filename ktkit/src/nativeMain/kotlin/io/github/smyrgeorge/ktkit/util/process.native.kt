package io.github.smyrgeorge.ktkit.util

import io.github.smyrgeorge.ktkit.Application
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction
import kotlinx.coroutines.runBlocking
import platform.posix.SIGINT
import platform.posix.atexit
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
