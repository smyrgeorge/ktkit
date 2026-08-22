package io.github.smyrgeorge.ktkit.api.rest.openapi

/**
 * Documentation of one operation parameter, used inside [OpenApi.params].
 *
 * @property name The parameter name.
 * @property location Where the parameter lives: `"path"`, `"query"` (default) or `"header"`.
 * @property type Overrides the detected schema type: `"string"`, `"int"`, `"long"`, `"boolean"`,
 *                `"float"`, `"double"`, `"uuid"`, ... Empty = keep the detected type.
 * @property description The parameter description. Empty = absent.
 */
@Retention(AnnotationRetention.SOURCE)
annotation class OpenApiParam(
    val name: String,
    val location: String = "query",
    val type: String = "",
    val description: String = "",
)
