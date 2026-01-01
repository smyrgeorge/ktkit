package io.github.smyrgeorge.ktkit.util

import io.ktor.util.logging.Logger

expect fun applicationLogger(name: String): Logger
