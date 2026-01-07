package io.github.smyrgeorge.ktkit.util

import io.github.smyrgeorge.ktkit.Application
import kotlin.concurrent.thread

actual fun registerShutdownHook() {
    Runtime.getRuntime().addShutdownHook(
        thread(name = "shutdown", start = false) {
            Application.INSTANCE_OR_NULL?.shutdown()
        }
    )
}

actual fun vmMemoryMetrics(): Map<String, Long> {
    val runtime = Runtime.getRuntime()
    return mapOf(
        "vm.memory.free" to runtime.freeMemory(),
        "vm.memory.total" to runtime.totalMemory(),
        "vm.memory.max" to runtime.maxMemory(),
    )
}

actual fun vmProcessorsMetrics(): Map<String, Int> {
    val runtime = Runtime.getRuntime()
    return mapOf("vm.processors.available" to runtime.availableProcessors())
}
