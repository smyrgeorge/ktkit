package io.github.smyrgeorge.ktorlib.examples

import io.github.smyrgeorge.ktorlib.Application
import io.github.smyrgeorge.ktorlib.api.rest.AbstractRestHandler
import io.github.smyrgeorge.ktorlib.api.rest.auth.impl.XRealNamePrincipalExtractor
import io.github.smyrgeorge.ktorlib.api.rest.impl.ApplicationStatusRestHandler
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf

fun start() {
    Application(
        name = "io.github.smyrgeorge.ktorlib.examples.Application",
        host = "localhost",
        port = 8080,
        configure = {
            withAuthenticationExtractor(XRealNamePrincipalExtractor())
            di {
                singleOf(::UserServiceImpl) { bind<UserService>() }
                singleOf(::UserRepositoryImpl) { bind<UserRepository>() }
                singleOf(::ExampleRestHandler) { bind<AbstractRestHandler>() }
                singleOf(::ApplicationStatusRestHandler) { bind<AbstractRestHandler>() }
            }
        }
    ).start(wait = true)
}
