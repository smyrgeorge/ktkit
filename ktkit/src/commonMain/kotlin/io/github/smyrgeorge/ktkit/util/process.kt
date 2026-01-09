package io.github.smyrgeorge.ktkit.util

expect fun registerShutdownHook()
expect fun vmMemoryMetrics(): Map<String, Long>
expect fun vmProcessorsMetrics(): Map<String, Int>
expect fun getEnv(name: String): String?
