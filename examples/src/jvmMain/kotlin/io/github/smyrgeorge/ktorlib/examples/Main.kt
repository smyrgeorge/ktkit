package io.github.smyrgeorge.ktorlib.examples

import io.github.smyrgeorge.ktorlib.api.rest.auth.impl.XRealNamePrincipalExtractor
import io.github.smyrgeorge.ktorlib.application.ApplicationBuilder.Companion.builder
import io.github.smyrgeorge.ktorlib.util.applicationLogger
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*

fun Application.cofigure() {
    builder()
        .withAuthenticationExtractor(XRealNamePrincipalExtractor())
        .withRoutes(ExampleHandler())
        .build()
}

fun main() {
    val env = applicationEnvironment {
        log = applicationLogger("io.github.smyrgeorge.ktorlib.examples")
    }

    embeddedServer(
        factory = CIO,
        environment = env,
        configure = {
            connectors.add(
                EngineConnectorBuilder().apply {
                    host = "localhost"
                    port = 8080
                }
            )
        },
        module = { cofigure() }
    ).start(wait = true)
}
