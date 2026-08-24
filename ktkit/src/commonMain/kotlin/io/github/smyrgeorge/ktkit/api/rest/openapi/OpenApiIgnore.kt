package io.github.smyrgeorge.ktkit.api.rest.openapi

/**
 * Excludes a REST handler — or a single route — from the generated OpenAPI specification.
 *
 * On a concrete [io.github.smyrgeorge.ktkit.api.rest.AbstractRestHandler] subclass, the ktkit
 * OpenAPI compiler plugin skips the handler entirely: no `openApiSpec()` override is generated
 * and none of its routes appear in the merged specification.
 *
 * Placed directly above a route call, only that route is excluded:
 *
 * ```
 * @OpenApiIgnore
 * GET("") { ... }
 * ```
 *
 * NOTE: the annotation has SOURCE retention (required for expression annotations), so a
 * class-level `@OpenApiIgnore` is only visible to the compiler plugin within the module being
 * compiled — an annotated base class in a *different* module does not carry the exclusion over.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class OpenApiIgnore
