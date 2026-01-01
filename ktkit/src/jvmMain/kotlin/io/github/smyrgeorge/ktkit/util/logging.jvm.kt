package io.github.smyrgeorge.ktkit.util

import io.ktor.util.logging.Logger

actual fun applicationLogger(name: String): Logger = LoggerJvm(name)
