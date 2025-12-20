package io.github.smyrgeorge.ktorlib.api.rest

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages

/**
 * Extension function to easily install exception handling in your Ktor application.
 *
 * Usage:
 * ```
 * fun Application.module() {
 *     installExceptionHandling()
 *     // ... other configuration
 * }
 * ```
 */
fun Application.installExceptionHandling() {
    install(StatusPages) {
        ExceptionHandler.configure(this)
    }
}
