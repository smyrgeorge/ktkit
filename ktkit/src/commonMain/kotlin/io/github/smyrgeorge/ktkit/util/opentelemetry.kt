package io.github.smyrgeorge.ktkit.util

import io.github.smyrgeorge.log4k.TracingEvent

const val TRACE_PARENT_HEADER = "traceparent"

data class TraceParent(
    val version: String,
    val traceId: String,
    val spanId: String,
    val sampled: Boolean
)

/**
 * Parses a W3C `traceparent` [header] value, or returns `null` when it is not valid — the
 * caller then starts a fresh trace, as the spec requires for an invalid header.
 *
 * Every field must be hexadecimal. Nothing downstream re-checks this: the tracing appenders hash an
 * id that is not hex, so the span would be exported under a fabricated trace id instead of
 * correlating with the caller, and [toOpenTelemetryHeader] would keep propagating the malformed
 * value to the next service.
 *
 * Fields shorter than their canonical length are zero-padded, and hex digits are lower-cased, so the
 * ids are always in the 32/16-character lower-case form used by both the header and the wire formats.
 */
fun extractOpenTelemetryHeader(header: String): TraceParent? {
    // Format: version-trace_id-span_id-flags
    val parts = header.split("-")
    if (parts.size != 4) return null

    val (rawVersion, rawTraceId, rawSpanId, rawFlags) = parts

    // Pad if shorter, return null if longer or not hexadecimal.
    if (!rawVersion.isHex(max = 2)) return null
    if (!rawTraceId.isHex(max = 32)) return null
    if (!rawSpanId.isHex(max = 16)) return null
    if (!rawFlags.isHex(max = 2)) return null

    val traceId = rawTraceId.lowercase().padStart(32, '0')
    val spanId = rawSpanId.lowercase().padStart(16, '0')

    if (traceId.all { it == '0' } || spanId.all { it == '0' }) return null

    return TraceParent(
        version = rawVersion.lowercase().padStart(2, '0'),
        traceId = traceId,
        spanId = spanId,
        // The `sampled` flag is the least significant bit of the trace flags.
        sampled = (rawFlags.toInt(16) and 0x01) == 1
    )
}

fun TracingEvent.Span.toOpenTelemetryHeader(): String {
    // Format: version-trace_id-span_id-flags
    val version = "00"
    val traceId = context.traceId.padStart(32, '0')
    val spanId = context.spanId.padStart(16, '0')
    val flags = "00"
    return "$version-$traceId-$spanId-$flags"
}

/**
 * Whether this is a non-empty hexadecimal number of at most [max] digits. Upper-case digits are
 * accepted — the W3C format asks for lower-case ones, but rejecting a peer over the casing of an
 * otherwise well-formed id would lose the trace for no gain.
 */
private fun String.isHex(max: Int): Boolean =
    isNotEmpty() && length <= max && all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
