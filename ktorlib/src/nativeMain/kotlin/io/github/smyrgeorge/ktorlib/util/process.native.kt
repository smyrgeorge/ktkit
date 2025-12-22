package io.github.smyrgeorge.ktorlib.util

import io.github.smyrgeorge.ktorlib.Application
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.runBlocking
import platform.posix.SIGINT
import platform.posix.atexit
import platform.posix.getcwd
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

@OptIn(ExperimentalForeignApi::class)
actual fun pwd(): String = ByteArray(1024).usePinned { getcwd(it.addressOf(0), 1024u) }!!.toKString()
