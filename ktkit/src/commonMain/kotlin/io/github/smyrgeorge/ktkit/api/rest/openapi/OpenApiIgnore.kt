package io.github.smyrgeorge.ktkit.api.rest.openapi

/**
 * Excludes a REST handler from the generated OpenAPI specification.
 *
 * When a concrete [io.github.smyrgeorge.ktkit.api.rest.AbstractRestHandler] subclass is annotated
 * with this annotation, the ktkit OpenAPI compiler plugin skips it entirely: no `openApiSpec()`
 * override is generated and none of its routes appear in the merged specification.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class OpenApiIgnore
