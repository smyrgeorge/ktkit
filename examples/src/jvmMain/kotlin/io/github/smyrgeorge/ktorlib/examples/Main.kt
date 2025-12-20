package io.github.smyrgeorge.ktorlib.examples

import io.github.smyrgeorge.ktorlib.Application
import io.github.smyrgeorge.ktorlib.api.rest.auth.impl.XRealNamePrincipalExtractor

fun main() {
    Application(
        name = "io.github.smyrgeorge.ktorlib.examples.Application",
        host = "localhost",
        port = 8080,
        configure = {
            withAuthenticationExtractor(XRealNamePrincipalExtractor())
            withRestHandler(ExampleRestHandler())
        }
    ).start(wait = true)
}
