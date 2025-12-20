package io.github.smyrgeorge.ktorlib.util

import io.github.smyrgeorge.ktorlib.util.LoggerNative
import io.ktor.util.logging.*

actual fun applicationLogger(name: String): Logger = LoggerNative(name)