package io.github.smyrgeorge.ktorlib.examples.di

import io.github.smyrgeorge.ktorlib.examples.ExampleRestHandler
import io.github.smyrgeorge.ktorlib.examples.repository.UserRepository
import io.github.smyrgeorge.ktorlib.examples.repository.UserRepositoryImpl
import io.github.smyrgeorge.ktorlib.examples.service.UserService
import io.github.smyrgeorge.ktorlib.examples.service.UserServiceImpl
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val appModule = module {
    // Repositories (Singleton)
    singleOf(::UserRepositoryImpl) { bind<UserRepository>() }

    // Services (Singleton)
    singleOf(::UserServiceImpl) { bind<UserService>() }

    // REST Handlers
    singleOf(::ExampleRestHandler)
}
