package io.github.smyrgeorge.ktorlib.examples

import io.github.smyrgeorge.ktorlib.api.rest.auth.impl.XRealNamePrincipalExtractor
import io.github.smyrgeorge.ktorlib.application.ApplicationBuilder.Companion.builder
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*

fun Application.module() {
    builder()
        .withAuthenticationExtractor(XRealNamePrincipalExtractor())
        .withRoutes(ExampleHandler())
        .build()
}

fun main() {
    embeddedServer(
        factory = CIO,
        port = 8080,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}
