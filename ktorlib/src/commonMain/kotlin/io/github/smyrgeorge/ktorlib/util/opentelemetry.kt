package io.github.smyrgeorge.ktorlib.util

const val TRACE_PARENT_HEADER = "traceparent"

data class TraceParent(
    val version: String,
    val traceId: String,
    val spanId: String,
    val sampled: Boolean
)

fun extractOpenTelemetryTraceParent(header: String): TraceParent? {
    // Format: version-trace_id-span_id-flags
    val parts = header.split("-")
    if (parts.size != 4) return null

    val (version, rawTraceId, rawSpanId, rawFlags) = parts

    // Pad if shorter, return null if longer
    if (rawTraceId.length > 32 || rawSpanId.length > 16 || rawFlags.length > 2) return null

    val traceId = rawTraceId.padStart(32, '0')
    val spanId = rawSpanId.padStart(16, '0')
    val flags = rawFlags.padStart(2, '0')

    if (traceId.all { it == '0' } || spanId.all { it == '0' }) return null

    val sampled = flags.toIntOrNull(16)?.and(0x01) == 1

    return TraceParent(
        version = version,
        traceId = traceId,
        spanId = spanId,
        sampled = sampled
    )
}
