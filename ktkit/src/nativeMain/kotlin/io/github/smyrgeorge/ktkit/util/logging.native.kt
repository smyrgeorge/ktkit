package io.github.smyrgeorge.ktkit.util

import io.ktor.util.logging.*

actual fun applicationLogger(name: String): Logger = LoggerNative(name)
