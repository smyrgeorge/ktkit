package io.github.smyrgeorge.ktorlib

import io.github.smyrgeorge.ktorlib.util.applicationLogger
import io.github.smyrgeorge.ktorlib.util.httpEngine
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.application.Application as KtorApplication

class Application(
    private val name: String,
    private val host: String = "localhost",
    private val port: Int = 8080,
    private val cofigure: KtorApplication.() -> Unit
) {
    private fun makeEnvironment(): ApplicationEnvironment = applicationEnvironment {
        log = applicationLogger(name)
    }

    private fun makeServer(
        env: ApplicationEnvironment,
    ): EmbeddedServer<ApplicationEngine, ApplicationEngine.Configuration> = embeddedServer(
        factory = httpEngine(),
        environment = env,
        configure = {
            connectors.add(
                EngineConnectorBuilder().apply {
                    host = this@Application.host
                    port = this@Application.port
                }
            )
        },
        module = cofigure
    )

    fun start(
        wait: Boolean = true
    ) {
        val env = makeEnvironment()
        makeServer(env).start(wait)
    }
}