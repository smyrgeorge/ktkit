package io.github.smyrgeorge.ktorlib.example

import io.github.smyrgeorge.ktorlib.Application
import io.github.smyrgeorge.ktorlib.api.rest.AbstractRestHandler
import io.github.smyrgeorge.ktorlib.api.rest.auth.impl.XRealNamePrincipalExtractor
import io.github.smyrgeorge.ktorlib.api.rest.impl.ApplicationStatusRestHandler
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf

fun start() {
//    val options = ConnectionPool.Options.builder()
//        .maxConnections(10)
//        .build()
//
//    val db = postgreSQL(
//        url = "postgresql://localhost:15432/test",
//        username = "postgres",
//        password = "postgres",
//        options = options
//    )

    Application(
        name = "io.github.smyrgeorge.ktorlib.example.Application",
        host = "localhost",
        port = 8080,
        configure = {
            withAuthenticationExtractor(XRealNamePrincipalExtractor())
            di {
                singleOf(::UserServiceImpl) { bind<UserService>() }
                singleOf(::UserRepositoryImpl) { bind<UserRepository>() }
                singleOf(::UserRestHandler) { bind<AbstractRestHandler>() }
                singleOf(::ApplicationStatusRestHandler) { bind<AbstractRestHandler>() }
            }
        }
    ).start(wait = true)
}
