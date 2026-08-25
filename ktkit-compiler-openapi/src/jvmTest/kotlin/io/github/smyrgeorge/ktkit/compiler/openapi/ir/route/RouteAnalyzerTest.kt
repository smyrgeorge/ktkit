package io.github.smyrgeorge.ktkit.compiler.openapi.ir.route

import io.github.smyrgeorge.ktkit.compiler.openapi.Compilations
import io.github.smyrgeorge.ktkit.compiler.openapi.obj
import io.github.smyrgeorge.ktkit.compiler.openapi.operation
import io.github.smyrgeorge.ktkit.compiler.openapi.parametersByName
import io.github.smyrgeorge.ktkit.compiler.openapi.refOf
import io.github.smyrgeorge.ktkit.compiler.openapi.str
import io.github.smyrgeorge.ktkit.compiler.openapi.strings
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RouteAnalyzerTest {

    private val items: JsonObject get() = Compilations.handlers.fragment("analysis.ItemsRestHandler")

    @Test
    fun queryHeaderAndMultiValueParametersAreDetected() {
        val params = items.operation("get", "/api/v1/items").parametersByName()
        val limit = params.getValue("limit")
        assertEquals("query", limit.str("in"))
        assertEquals("false", limit.str("required"))
        assertEquals("integer", limit.obj("schema").str("type"))

        val trace = params.getValue("x-trace")
        assertEquals("header", trace.str("in"))
        assertEquals("string", trace.obj("schema").str("type"))

        val colors = params.getValue("color")
        assertEquals("query", colors.str("in"))
        assertEquals("array", colors.obj("schema").str("type"), "queryParams() documents a string array")
    }

    @Test
    fun pathParameterConversionsDetermineTheSchema() {
        val id = items.operation("get", "/api/v1/items/{id}").parametersByName().getValue("id")
        assertEquals("path", id.str("in"))
        assertEquals("integer", id.obj("schema").str("type"))
        assertEquals("int64", id.obj("schema").str("format"), "asLong() documents int64")
    }

    @Test
    fun enumParametersListTheEntryNames() {
        val color = items.operation("get", "/api/v1/items/enum").parametersByName().getValue("color")
        assertEquals("true", color.str("required"), "asEnum() (non-OrNull) marks the parameter required")
        assertEquals("string", color.obj("schema").str("type"))
        assertEquals(listOf("RED", "GREEN"), color.obj("schema").strings("enum"))
    }

    @Test
    fun eitherIsUnwrappedAndConstructedErrorsAreDocumented() {
        val op = items.operation("get", "/api/v1/items/either")
        assertEquals(
            "#/components/schemas/ItemDto",
            op.obj("responses").obj("200").obj("content").obj("application/json").refOf("schema"),
            "Either<ErrorSpec, ItemDto> unwraps to ItemDto"
        )
        assertEquals(
            "#/components/responses/NotFound",
            op.obj("responses").obj("404").str($$"$ref"),
            "NotFound(...) constructed in the lambda documents a 404"
        )
    }

    @Test
    fun flowResponsesAreDetectedAsStreaming() {
        val schema = items.operation("get", "/api/v1/items/stream")
            .obj("responses").obj("200").obj("content").obj("application/json").obj("schema")
        assertEquals("array", schema.str("type"))
        assertEquals("#/components/schemas/ItemDto", schema.refOf("items"))
    }

    @Test
    fun explicitAndConstructedStatusCodesAreResolved() {
        val legacy = items.operation("put", "/api/v1/items/legacy")
        assertTrue("202" in legacy.obj("responses").keys, "HttpStatusCode.Accepted resolves to 202")
        val custom = items.operation("delete", "/api/v1/items/custom-status")
        assertTrue("299" in custom.obj("responses").keys, "HttpStatusCode(299, ...) resolves to 299")
        assertEquals("Success", custom.obj("responses").obj("299").str("description"), "unknown code phrase")
    }

    @Test
    fun optionalPathParametersAreNormalized() {
        assertTrue("/api/v1/items/opt/{id}" in items.obj("paths").keys, "{id?} normalizes to {id}")
        val id = items.operation("get", "/api/v1/items/opt/{id}").parametersByName().getValue("id")
        assertEquals("true", id.str("required"), "path parameters are always required in OpenAPI")
    }

    @Test
    fun theDefaultTagIsTheHandlerNameWithoutTheSuffix() {
        assertEquals(listOf("Items"), items.operation("get", "/api/v1/items/{id}").strings("tags"))
    }

    @Test
    fun dynamicPathsSkipTheRouteWithAWarning() {
        assertNull(
            Compilations.edgeCases.openApiSpec("warnings.DynamicPathHandler"),
            "the only route is dynamic, so no fragment is generated at all"
        )
        assertTrue(
            Compilations.edgeCases.warnings.any { "non-constant path for GET route in DynamicPathHandler" in it },
            "${Compilations.edgeCases.warnings}"
        )
    }

    @Test
    fun orphanPathVariablesAreDroppedWithAWarning() {
        val op = Compilations.edgeCases.fragment("warnings.OrphanParamHandler").operation("get", "/o/fixed")
        assertNull(op["parameters"], "the pathVariable(\"id\") is not part of the path")
        assertNull(op.obj("responses")["400"], "no documented inputs, no documented 400")
        assertTrue(
            Compilations.edgeCases.warnings.any { "pathVariable(\"id\") is not part of path '/o/fixed'" in it },
            "${Compilations.edgeCases.warnings}"
        )
    }
}
