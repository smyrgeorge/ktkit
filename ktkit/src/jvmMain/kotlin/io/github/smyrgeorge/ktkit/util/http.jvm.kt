package io.github.smyrgeorge.ktkit.util

import io.ktor.server.engine.*
import io.ktor.server.netty.*

@Suppress("UNCHECKED_CAST")
actual fun httpEngine(): ApplicationEngineFactory<ApplicationEngine, ApplicationEngine.Configuration> =
    Netty as ApplicationEngineFactory<ApplicationEngine, ApplicationEngine.Configuration>
