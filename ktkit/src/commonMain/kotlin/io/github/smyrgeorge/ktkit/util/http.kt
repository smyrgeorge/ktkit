package io.github.smyrgeorge.ktkit.util

import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.ApplicationEngineFactory

expect fun httpEngine(): ApplicationEngineFactory<ApplicationEngine, ApplicationEngine.Configuration>
