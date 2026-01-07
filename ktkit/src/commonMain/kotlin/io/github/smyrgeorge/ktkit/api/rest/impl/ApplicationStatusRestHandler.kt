package io.github.smyrgeorge.ktkit.api.rest.impl

import io.github.smyrgeorge.ktkit.util.vmMemoryMetrics
import io.github.smyrgeorge.ktkit.util.vmProcessorsMetrics
import io.github.smyrgeorge.log4k.Meter
import io.github.smyrgeorge.log4k.impl.OpenTelemetryAttributes
import io.ktor.server.routing.Route
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

class ApplicationStatusRestHandler : AnonymousRestHandler() {
    override fun String.uri(): String = "/api/status$this"

    val meter = Meter.of(this::class)

    init {
        meter.gauge<Long>(name = "vm.memory", unit = "Bytes", description = "VM Memory Metrics").apply {
            poll(every = 5.seconds) {
                vmMemoryMetrics().forEach {
                    record(
                        it.value,
                        OpenTelemetryAttributes.SERVICE_NAME to app.name,
                        "tag" to it.key.removePrefix("$name."),
                    )
                }
            }
        }

        meter.gauge<Int>(name = "vm.processors", unit = "Count", description = "VM Processors Metrics").apply {
            poll(every = 5.seconds) {
                vmProcessorsMetrics().forEach {
                    record(
                        it.value,
                        OpenTelemetryAttributes.SERVICE_NAME to app.name,
                        "tag" to it.key.removePrefix("$name."),
                    )
                }
            }
        }
    }

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
