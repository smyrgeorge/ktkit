package io.github.smyrgeorge.ktorlib.examples

import io.github.smyrgeorge.ktorlib.Application
import io.github.smyrgeorge.ktorlib.api.rest.AbstractRestHandler
import io.github.smyrgeorge.ktorlib.api.rest.auth.impl.XRealNamePrincipalExtractor
import io.github.smyrgeorge.ktorlib.examples.di.appModule
import io.github.smyrgeorge.ktorlib.util.getAll
import org.koin.core.context.startKoin

fun main() {
    // Initialize Koin
    val di = startKoin {
        modules(appModule)
    }

    Application(
        name = "io.github.smyrgeorge.ktorlib.examples.Application",
        host = "localhost",
        port = 8080,
        configure = {
            withAuthenticationExtractor(XRealNamePrincipalExtractor())
            withRestHandlers(di.getAll<AbstractRestHandler>())
        }
    ).start(wait = true)
}
