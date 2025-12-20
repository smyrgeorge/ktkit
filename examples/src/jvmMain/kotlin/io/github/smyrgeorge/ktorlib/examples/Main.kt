package io.github.smyrgeorge.ktorlib.examples

import io.github.smyrgeorge.ktorlib.Application
import io.github.smyrgeorge.ktorlib.api.rest.auth.impl.XRealNamePrincipalExtractor
import io.github.smyrgeorge.ktorlib.application.ApplicationBuilder.Companion.builder
import io.ktor.server.application.Application as KtorApplication

fun KtorApplication.cofigure() {
    builder()
        .withAuthenticationExtractor(XRealNamePrincipalExtractor())
        .withRoutes(ExampleRestHandler())
        .build()
}

fun main() {
    Application(
        name = "io.github.smyrgeorge.ktorlib.examples",
        host = "localhost",
        port = 8080,
        cofigure = { cofigure() }
    ).start(wait = true)
}
