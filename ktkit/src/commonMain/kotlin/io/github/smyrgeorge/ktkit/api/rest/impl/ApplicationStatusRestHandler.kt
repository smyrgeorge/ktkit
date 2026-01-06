package io.github.smyrgeorge.ktkit.api.rest.impl

import io.ktor.server.routing.Route

class ApplicationStatusRestHandler : AnonymousRestHandler() {
    override fun String.uri(): String = "/api/status$this"

    override fun Route.routes() {
        GET("/health") {
            mapOf(
                "status" to app.status.name
            )
        }
        GET("/metrics") {
            app.metrics.toOpenMetricsLineFormatString()
        }
    }
}
