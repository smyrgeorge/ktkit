package io.github.smyrgeorge.ktkit.api.rest.impl

import io.ktor.server.routing.Route
import kotlinx.serialization.Serializable
import kotlin.time.Clock

class ApplicationStatusRestHandler : AnonymousRestHandler() {
    override fun String.uri(): String = "/api/status$this"

    override fun Route.routes() {
        GET("/health") {
            val uptime = Clock.System.now() - app.startedAt
            Status(
                name = app.name,
                status = app.status.name,
                startedAt = app.startedAt.toString(),
                uptime = uptime.toString(),
                uptimeSeconds = uptime.inWholeSeconds
            )
        }
        GET("/metrics") {
            app.metrics.toOpenMetricsLineFormatString()
        }
    }

    @Serializable
    data class Status(
        val name: String,
        val status: String,
        val startedAt: String,
        val uptime: String,
        val uptimeSeconds: Long
    )
}
