package io.github.smyrgeorge.ktorlib.util

import io.github.smyrgeorge.log4k.impl.OpenTelemetry
import io.github.smyrgeorge.log4k.impl.Tags
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.queryString

fun ApplicationCall.spanName(): String =
    "${request.httpMethod.value.lowercase()}_${request.local.uri}"

fun ApplicationCall.spanTags(): Tags =
    mapOf(
        OpenTelemetry.HTTP_REQUEST_METHOD to request.httpMethod.value,
        OpenTelemetry.URL_PATH to request.path(),
        OpenTelemetry.URL_QUERY to request.queryString(),
        OpenTelemetry.URL_SCHEME to request.local.scheme,
    )

data class TraceParent(
    val version: String,
    val traceId: String,
    val spanId: String,
    val sampled: Boolean
)

fun ApplicationCall.extractOpenTelemetryTraceParent(): TraceParent? {
    val header = request.headers["traceparent"] ?: return null

    // Expected format:
    // version-trace_id-span_id-flags
    val parts = header.split("-")
    if (parts.size != 4) return null

    val (version, traceId, spanId, flags) = parts

    // Basic validation per spec
    if (traceId.length != 32 || spanId.length != 16 || flags.length != 2) return null
    if (traceId.all { it == '0' } || spanId.all { it == '0' }) return null

    val sampled = flags.toIntOrNull(16)?.and(0x01) == 1

    return TraceParent(
        version = version,
        traceId = traceId,
        spanId = spanId,
        sampled = sampled
    )
}
