package io.github.smyrgeorge.ktkit.util

import io.github.smyrgeorge.log4k.impl.OpenTelemetryAttributes
import io.github.smyrgeorge.log4k.impl.Tags
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.routing.path as routePath

/**
 * The template of the route that matched this call e.g. `/api/v1/users/{id}`.
 *
 * Deliberately not the request URI: the URI carries the path parameters and the query string, so
 * using it would give every request a name of its own — tracing backends aggregate on the span name
 * — and would copy any secret passed as a query parameter into that name.
 */
fun RoutingCall.routeTemplate(): String = route.routePath.ifEmpty { "/" }

fun RoutingCall.spanName(): String =
    "${request.httpMethod.value.lowercase()}_${routeTemplate()}"

/**
 * The tags attached to the span of this call.
 *
 * The query string is intentionally left out: it routinely carries credentials and personal data,
 * and once a tag reaches the tracing backend, it is out of the application's hands.
 */
fun RoutingCall.spanTags(serviceName: String): Tags =
    mapOf(
        OpenTelemetryAttributes.SERVICE_NAME to serviceName,
        OpenTelemetryAttributes.HTTP_REQUEST_METHOD to request.httpMethod.value,
        OpenTelemetryAttributes.HTTP_ROUTE to routeTemplate(),
        OpenTelemetryAttributes.URL_PATH to request.path(),
        OpenTelemetryAttributes.URL_SCHEME to request.local.scheme,
    )

fun RoutingCall.extractOpenTelemetryHeader(): TraceParent? =
    request.headers[TRACE_PARENT_HEADER]?.let { extractOpenTelemetryHeader(it) }
