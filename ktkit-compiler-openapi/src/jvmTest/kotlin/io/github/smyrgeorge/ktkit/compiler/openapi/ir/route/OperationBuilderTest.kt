package io.github.smyrgeorge.ktkit.compiler.openapi.ir.route

import io.github.smyrgeorge.ktkit.compiler.openapi.Compilations
import io.github.smyrgeorge.ktkit.compiler.openapi.obj
import io.github.smyrgeorge.ktkit.compiler.openapi.operation
import io.github.smyrgeorge.ktkit.compiler.openapi.refOf
import io.github.smyrgeorge.ktkit.compiler.openapi.str
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OperationBuilderTest {

    private val items: JsonObject get() = Compilations.handlers.fragment("analysis.ItemsRestHandler")

    @Test
    fun requestBodyAndCreatedStatus() {
        val op = items.operation("post", "/api/v1/items/create")
        assertEquals("true", op.obj("requestBody").str("required"))
        assertEquals(
            "#/components/schemas/ItemDto",
            op.obj("requestBody").obj("content").obj("application/json").refOf("schema")
        )
        assertTrue("201" in op.obj("responses").keys, "POST defaults to 201")
        assertTrue("400" in op.obj("responses").keys, "a request body implies a possible 400")
    }

    @Test
    fun unitResponsesHaveNoContent() {
        val response = items.operation("put", "/api/v1/items/legacy").obj("responses").obj("202")
        assertNull(response["content"], "Unit responses carry no content")
    }

    @Test
    fun stringResponsesArePlainText() {
        val response = items.operation("get", "/grouped/api/v1/items/inside").obj("responses").obj("200")
        assertTrue("text/plain" in response.obj("content").keys)
    }

    @Test
    fun operationIdsAreDeterministic() {
        assertEquals(
            "ItemsRestHandler__GET__api_v1_items_by_id",
            items.operation("get", "/api/v1/items/{id}").str("operationId")
        )
        assertEquals(
            "ItemsRestHandler__POST__api_v1_items_create",
            items.operation("post", "/api/v1/items/create").str("operationId")
        )
    }

    @Test
    fun authenticatedHandlersDocument401And403() {
        val responses = items.operation("get", "/api/v1/items/{id}").obj("responses")
        assertTrue("401" in responses.keys && "403" in responses.keys)
        assertTrue("500" in responses.keys, "every route documents the unexpected 500")
    }

    @Test
    fun anonymousHandlersOmit401And403() {
        val fragment = Compilations.handlers.fragment("analysis.OpenRestHandler")
        val responses = fragment.operation("get", "/open/ping").obj("responses")
        assertFalse("401" in responses.keys || "403" in responses.keys)
        assertTrue("500" in responses.keys)
    }
}
