package io.github.smyrgeorge.ktorlib.api.rest.impl

import io.github.smyrgeorge.log4k.Logger
import io.ktor.server.routing.Route
import kotlinx.serialization.Serializable

class ApplicationStatusRestHandler : AnonymousRestHandler() {
    override val log: Logger = Logger.of(this::class)

    override fun String.uri(): String = "/api/status$this"

    override fun Route.routes() {
        GET("/health") {
            Health(Health.Status.UP)
        }
    }

    @Serializable
    data class Health(val status: Status) {
        @Serializable
        enum class Status {
            UP
        }
    }
}
