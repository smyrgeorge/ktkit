package io.github.smyrgeorge.ktorlib.util

import io.ktor.server.cio.*
import io.ktor.server.engine.*

@Suppress("UNCHECKED_CAST")
actual fun httpEngine(): ApplicationEngineFactory<ApplicationEngine, ApplicationEngine.Configuration> =
    CIO as ApplicationEngineFactory<ApplicationEngine, ApplicationEngine.Configuration>