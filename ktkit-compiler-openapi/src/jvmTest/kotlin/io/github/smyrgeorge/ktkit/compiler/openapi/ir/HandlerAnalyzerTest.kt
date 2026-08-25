package io.github.smyrgeorge.ktkit.compiler.openapi.ir

import io.github.smyrgeorge.ktkit.compiler.openapi.Compilations
import io.github.smyrgeorge.ktkit.compiler.openapi.obj
import io.github.smyrgeorge.ktkit.compiler.openapi.operation
import io.github.smyrgeorge.ktkit.compiler.openapi.refOf
import io.github.smyrgeorge.ktkit.compiler.openapi.str
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HandlerAnalyzerTest {

    private val items: JsonObject get() = Compilations.handlers.fragment("analysis.ItemsRestHandler")

    @Test
    fun happyPathHandlersCompileWithoutWarnings() {
        Compilations.handlers.assertOk()
        assertEquals(emptyList(), Compilations.handlers.warnings, "expected no plugin warnings")
    }

    @Test
    fun uriTemplateAndPathsAreResolved() {
        val paths = items.obj("paths").keys
        assertTrue("/api/v1/items" in paths, "$paths")
        assertTrue("/api/v1/items/{id}" in paths, "$paths")
        assertTrue("/grouped/api/v1/items/inside" in paths, "route(\"grouped\") prefixes the path: $paths")
    }

    @Test
    fun duplicateRoutesKeepTheFirstWithAWarning() {
        val fragment = Compilations.edgeCases.fragment("warnings.DuplicateRouteHandler")
        assertEquals("first", fragment.operation("get", "/dup").str("summary"), "the first declaration wins")
        assertTrue(
            Compilations.edgeCases.warnings.any { "duplicate route 'GET /dup' in warnings.DuplicateRouteHandler" in it },
            "${Compilations.edgeCases.warnings}"
        )
    }

    @Test
    fun dynamicRouteGroupsSkipTheirSubtreeWithAWarning() {
        val fragment = Compilations.edgeCases.fragment("warnings.DynamicGroupHandler")
        assertEquals(setOf("/g/ok"), fragment.obj("paths").keys, "the dynamic group's routes are not documented")
        assertTrue(
            Compilations.edgeCases.warnings.any { "non-constant route(...) group in DynamicGroupHandler" in it },
            "${Compilations.edgeCases.warnings}"
        )
    }

    @Test
    fun dynamicUriFallsBackToPerRoutePaths() {
        val fragment = Compilations.edgeCases.fragment("warnings.DynamicUriHandler")
        assertEquals(setOf("/as-is"), fragment.obj("paths").keys, "the uri() prefix could not be applied")
        assertTrue(
            Compilations.edgeCases.warnings.any { "could not statically evaluate warnings.DynamicUriHandler.uri()" in it },
            "${Compilations.edgeCases.warnings}"
        )
    }

    @Test
    fun sharedErrorResponsesAreEmittedOnce() {
        val responses = items.obj("components").obj("responses")
        listOf("BadRequest", "Unauthorized", "Forbidden", "NotFound", "InternalServerError").forEach {
            assertTrue(it in responses.keys, "missing shared response '$it' in ${responses.keys}")
            assertEquals(
                "#/components/schemas/ApiError",
                responses.obj(it).obj("content").obj("application/json").refOf("schema")
            )
        }
    }

    @Test
    fun theFragmentNamesItsHandler() {
        assertEquals("analysis.ItemsRestHandler", items.str("x-handler"))
    }
}
