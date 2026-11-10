package io.github.smyrgeorge.ktkit

import io.github.smyrgeorge.ktkit.api.rest.AbstractRestHandler
import io.github.smyrgeorge.ktkit.api.rest.impl.AnonymousRestHandler
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.testing.testApplication
import org.koin.core.context.stopKoin
import org.koin.dsl.bind
import org.koin.dsl.koinConfiguration
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ConfigurerKoinConfigurationTest {

    private class Greeting(val text: String)

    private class PingHandler : AnonymousRestHandler() {
        override fun String.uri(): String = this

        override fun Route.routes() {
            get("/ping") { call.respondText("pong") }
        }
    }

    private val app = Application(name = "configurer-test", conf = Application.Conf())

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    // di(koinConfiguration { modules(module { single { PingHandler() } bind AbstractRestHandler::class }) })
    // GET /ping -> 200 "pong"
    @Test
    fun `a handler declared only in a KoinConfiguration is mounted`() = testApplication {
        application {
            Application.Configurer(app, this).apply {
                di(koinConfiguration {
                    modules(module { single { PingHandler() } bind AbstractRestHandler::class })
                })
            }.configure()
        }

        val response = client.get("/ping")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("pong", response.bodyAsText())
    }

    // di { single { Greeting("module") } } + di(koinConfiguration { single { Greeting("configuration") } })
    // get<Greeting>().text == "configuration"
    @Test
    fun `a KoinConfiguration definition overrides the di module`() = testApplication {
        application {
            Application.Configurer(app, this).apply {
                di { single { Greeting("module") } }
                di(koinConfiguration { modules(module { single { Greeting("configuration") } }) })
            }.configure()
        }
        startApplication()

        assertEquals("configuration", app.di.koin.get<Greeting>().text)
    }

    // di(koinConfiguration { single { Greeting("first") } }) + di(koinConfiguration { single { 42 } })
    // get<Greeting>().text == "first", get<Int>() == 42
    @Test
    fun `every KoinConfiguration passed to di is applied`() = testApplication {
        application {
            Application.Configurer(app, this).apply {
                di(koinConfiguration { modules(module { single { Greeting("first") } }) })
                di(koinConfiguration { modules(module { single { 42 } }) })
            }.configure()
        }
        startApplication()

        assertEquals("first", app.di.koin.get<Greeting>().text)
        assertEquals(42, app.di.koin.get<Int>())
    }
}
