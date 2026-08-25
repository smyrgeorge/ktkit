package io.github.smyrgeorge.ktkit.compiler.openapi.fir

import io.github.smyrgeorge.ktkit.compiler.openapi.Compilations
import io.github.smyrgeorge.ktkit.compiler.openapi.obj
import io.github.smyrgeorge.ktkit.compiler.openapi.operation
import io.github.smyrgeorge.ktkit.compiler.openapi.parametersByName
import io.github.smyrgeorge.ktkit.compiler.openapi.str
import io.github.smyrgeorge.ktkit.compiler.openapi.strings
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class MetadataCollectorTest {

    private val items: JsonObject get() = Compilations.handlers.fragment("analysis.ItemsRestHandler")

    @Test
    fun openApiMetadataIsApplied() {
        val op = items.operation("get", "/api/v1/items")
        assertEquals("From a const.", op.str("summary"), "const references are constant-evaluated")
        assertEquals("Lists items.", op.str("description"))
        assertEquals(listOf("items", "catalog"), op.strings("tags"))
    }

    @Test
    fun deprecatedMetadataMarksTheOperation() {
        val op = items.operation("put", "/api/v1/items/legacy")
        assertEquals("true", op.str("deprecated"))
        assertEquals("Deprecated: Use POST /create instead.", op.str("description"))
    }

    @Test
    fun expressionLevelOpenApiInfoDescribesTheParameter() {
        val verbose = items.operation("get", "/api/v1/items").parametersByName().getValue("verbose")
        assertEquals("boolean", verbose.obj("schema").str("type"))
        assertEquals("Verbose output.", verbose.str("description"), "@OpenApiInfo on the expression itself")
    }

    @Test
    fun ignoredRoutesAreExcluded() {
        assertFalse("/api/v1/items/internal" in items.obj("paths").keys)
    }
}
