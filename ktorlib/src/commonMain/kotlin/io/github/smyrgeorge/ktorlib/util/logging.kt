package io.github.smyrgeorge.ktorlib.util

import io.ktor.util.logging.Logger

expect fun applicationLogger(name: String): Logger
