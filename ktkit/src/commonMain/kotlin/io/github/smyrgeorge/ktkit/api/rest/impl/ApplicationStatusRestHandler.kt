package io.github.smyrgeorge.ktkit.api.rest.impl

import io.ktor.server.routing.Route
import kotlinx.serialization.Serializable

class ApplicationStatusRestHandler : AnonymousRestHandler() {
    override fun String.uri(): String = "/api/status$this"

    override fun Route.routes() {
        GET("/health") {
            Health(Health.Status.UP)
        }
        GET("/metrics") {
            app.metrics.toOpenMetricsLineFormatString()
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
