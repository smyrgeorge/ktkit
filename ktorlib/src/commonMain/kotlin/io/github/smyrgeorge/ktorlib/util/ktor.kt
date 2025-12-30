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

fun ApplicationCall.extractOpenTelemetryTraceParent(): TraceParent? =
    request.headers[TRACE_PARENT_HEADER]?.let { extractOpenTelemetryTraceParent(it) }
