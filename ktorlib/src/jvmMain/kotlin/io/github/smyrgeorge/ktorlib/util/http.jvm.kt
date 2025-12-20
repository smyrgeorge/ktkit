package io.github.smyrgeorge.ktorlib.util

import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.ApplicationEngineFactory

@Suppress("UNCHECKED_CAST")
actual fun httpEngine(): ApplicationEngineFactory<ApplicationEngine, ApplicationEngine.Configuration> =
    CIO as ApplicationEngineFactory<ApplicationEngine, ApplicationEngine.Configuration>