package io.github.smyrgeorge.ktkit.api.rest.openapi

/**
 * Adds a description to an element of the generated OpenAPI specification.
 *
 * On a `@Serializable` class or one of its properties, the description lands in the generated
 * schema:
 *
 * ```
 * @OpenApiInfo("A user of the system.")
 * @Serializable
 * data class UserDto(
 *     @OpenApiInfo("The unique id of the user.")
 *     val id: String,
 * )
 * ```
 *
 * Inside a route lambda, it documents the parameter read by a `pathVariable`/`queryParam`/
 * `header`/`queryParams`/`headers` call — placed on the local variable or directly on the
 * expression:
 *
 * ```
 * GET("/{id}") {
 *     @OpenApiInfo("The id of the user.")
 *     val id = pathVariable("id").asUuid()
 *
 *     val verbose = @OpenApiInfo("Whether to include details.") queryParam("verbose").asBooleanOrNull()
 *     ...
 * }
 * ```
 *
 * NOTE: the annotation has SOURCE retention (required for expression annotations), so it is only
 * visible to the compiler plugin within the module being compiled — annotations on classes or
 * properties of a *different* module are not picked up.
 *
 * @property description The description text, rendered in the specification and Swagger UI.
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.EXPRESSION,
)
@Retention(AnnotationRetention.SOURCE)
annotation class OpenApiInfo(val description: String)
