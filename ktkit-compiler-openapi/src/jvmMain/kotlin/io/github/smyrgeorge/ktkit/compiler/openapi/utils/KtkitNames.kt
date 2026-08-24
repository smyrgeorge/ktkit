package io.github.smyrgeorge.ktkit.compiler.openapi.utils

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object KtkitNames {
    val ABSTRACT_REST_HANDLER = FqName("io.github.smyrgeorge.ktkit.api.rest.AbstractRestHandler")
    val ANONYMOUS_REST_HANDLER = FqName("io.github.smyrgeorge.ktkit.api.rest.impl.AnonymousRestHandler")
    val OPEN_API_IGNORE = FqName("io.github.smyrgeorge.ktkit.api.rest.openapi.OpenApiIgnore")
    val OPEN_API_IGNORE_ANNOTATION = ClassId.topLevel(OPEN_API_IGNORE)
    val OPEN_API_ANNOTATION = ClassId(FqName("io.github.smyrgeorge.ktkit.api.rest.openapi"), Name.identifier("OpenApi"))
    val HTTP_CONTEXT = FqName("io.github.smyrgeorge.ktkit.api.rest.HttpContext")
    val HTTP_CONTEXT_VAR = FqName("io.github.smyrgeorge.ktkit.api.rest.HttpContext.Var")
    val SERIALIZABLE = FqName("kotlinx.serialization.Serializable")
    val SERIAL_NAME = FqName("kotlinx.serialization.SerialName")
    val TRANSIENT = FqName("kotlinx.serialization.Transient")

    const val OPEN_API_SPEC = "openApiSpec"
    const val URI = "uri"
    const val ROUTES = "routes"

    /** Route-defining member functions of AbstractRestHandler, mapped to their default success status code. */
    val VERBS: Map<String, Int> = mapOf(
        "GET" to 200,
        "POST" to 201,
        "PUT" to 200,
        "PATCH" to 200,
        "DELETE" to 200,
        "HEAD" to 200,
        "OPTIONS" to 200,
    )

    /** ktkit's built-in ErrorSpec implementations, mapped to the HTTP status code they produce. */
    val ERROR_STATUS_BY_FQ: Map<String, Int> = mapOf(
        "io.github.smyrgeorge.ktkit.api.error.impl.NotFound" to 404,
        "io.github.smyrgeorge.ktkit.api.error.impl.Unauthorized" to 401,
        "io.github.smyrgeorge.ktkit.api.error.impl.Forbidden" to 403,
        "io.github.smyrgeorge.ktkit.api.error.impl.MalformedRequestBody" to 400,
        "io.github.smyrgeorge.ktkit.api.error.impl.MissingParameter" to 400,
        "io.github.smyrgeorge.ktkit.api.error.impl.UnsupportedEnumValue" to 400,
        "io.github.smyrgeorge.ktkit.api.error.impl.DatabaseError" to 500,
        "io.github.smyrgeorge.ktkit.api.error.impl.UnknownError" to 500,
    )
}
