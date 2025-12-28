package io.github.smyrgeorge.ktorlib.util

import io.github.smyrgeorge.log4k.impl.OpenTelemetry
import io.github.smyrgeorge.log4k.impl.Tags
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.host
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri

fun ApplicationCall.spanName(): String =
    "${request.httpMethod.value.lowercase()}_${request.local.uri}"

fun ApplicationCall.spanTags(): Tags =
    mapOf(
        OpenTelemetry.HTTP_METHOD to request.httpMethod.value,
        OpenTelemetry.HTTP_URL to request.uri,
        OpenTelemetry.HTTP_SCHEME to request.local.scheme,
        OpenTelemetry.HTTP_HOST to request.host()
    )
