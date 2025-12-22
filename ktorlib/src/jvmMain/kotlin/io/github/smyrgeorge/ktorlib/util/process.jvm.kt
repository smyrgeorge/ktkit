package io.github.smyrgeorge.ktorlib.util

import io.github.smyrgeorge.ktorlib.Application
import kotlin.concurrent.thread

actual fun registerShutdownHook() {
    Runtime.getRuntime().addShutdownHook(
        thread(name = "shutdown", start = false) {
            Application.INSTANCE.shutdown()
        }
    )
}

actual fun pwd(): String = System.getProperty("user.dir")
