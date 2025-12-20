package io.github.smyrgeorge.ktorlib.util

import io.github.smyrgeorge.ktorlib.util.LoggerJvm
import io.ktor.util.logging.Logger

actual fun applicationLogger(name: String): Logger = LoggerJvm(name)