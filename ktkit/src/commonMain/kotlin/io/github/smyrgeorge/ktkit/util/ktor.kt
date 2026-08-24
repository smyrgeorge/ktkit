package io.github.smyrgeorge.ktkit.util

import io.github.smyrgeorge.log4k.impl.OpenTelemetryAttributes
import io.github.smyrgeorge.log4k.impl.Tags
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.queryString
import io.ktor.server.routing.RoutingCall

fun RoutingCall.spanName(): String =
    "${request.httpMethod.value.lowercase()}_${request.local.uri}"

fun RoutingCall.spanTags(serviceName: String): Tags =
    mapOf(
        OpenTelemetryAttributes.SERVICE_NAME to serviceName,
        OpenTelemetryAttributes.HTTP_REQUEST_METHOD to request.httpMethod.value,
        OpenTelemetryAttributes.URL_PATH to request.path(),
        OpenTelemetryAttributes.URL_QUERY to request.queryString(),
        OpenTelemetryAttributes.URL_SCHEME to request.local.scheme,
    )

fun RoutingCall.extractOpenTelemetryHeader(): TraceParent? =
    request.headers[TRACE_PARENT_HEADER]?.let { extractOpenTelemetryHeader(it) }
