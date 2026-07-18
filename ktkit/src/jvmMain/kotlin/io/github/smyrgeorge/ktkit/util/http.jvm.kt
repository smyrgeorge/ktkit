package io.github.smyrgeorge.ktkit.util

import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.ApplicationEngineFactory
import io.ktor.server.netty.Netty

@Suppress("UNCHECKED_CAST")
actual fun httpEngine(): ApplicationEngineFactory<ApplicationEngine, ApplicationEngine.Configuration> =
    Netty as ApplicationEngineFactory<ApplicationEngine, ApplicationEngine.Configuration>
