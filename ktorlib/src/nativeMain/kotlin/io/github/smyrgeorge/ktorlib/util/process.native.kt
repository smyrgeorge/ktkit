package io.github.smyrgeorge.ktorlib.util

import io.github.smyrgeorge.ktorlib.Application
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
            Application.INSTANCE.shutdown()
        }
    })
    signal(SIGINT, staticCFunction<Int, Unit> {
        runBlocking {
            Application.INSTANCE.shutdown()
            println("Triggered shutdown hook.")
        }
    })
}
