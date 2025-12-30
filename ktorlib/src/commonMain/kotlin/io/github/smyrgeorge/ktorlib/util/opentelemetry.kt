package io.github.smyrgeorge.ktorlib.util

const val TRACE_PARENT_HEADER = "traceparent"

data class TraceParent(
    val version: String,
    val traceId: String,
    val spanId: String,
    val sampled: Boolean
)

fun extractOpenTelemetryTraceParent(header: String): TraceParent? {

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
