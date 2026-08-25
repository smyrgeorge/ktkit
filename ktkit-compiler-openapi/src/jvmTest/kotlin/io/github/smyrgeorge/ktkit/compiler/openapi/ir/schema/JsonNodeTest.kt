package io.github.smyrgeorge.ktkit.compiler.openapi.ir.schema

import io.github.smyrgeorge.ktkit.compiler.openapi.ir.schema.JsonNode.Companion.arr
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.schema.JsonNode.Companion.bool
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.schema.JsonNode.Companion.num
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.schema.JsonNode.Companion.obj
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.schema.JsonNode.Companion.str
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JsonNodeTest {

    @Test
    fun rendersPrimitives() {
        assertEquals("\"hello\"", str("hello").renderToString())
        assertEquals("42", num(42).renderToString())
        assertEquals("-1", num(-1).renderToString())
        assertEquals("true", bool(true).renderToString())
        assertEquals("false", bool(false).renderToString())
        assertEquals("null", JsonNode.Null.renderToString())
    }

    @Test
    fun rendersObjectsInInsertionOrder() {
        val node = obj("b" to str("2"), "a" to str("1"))
        node["c"] = num(3)
        assertEquals("""{"b":"2","a":"1","c":3}""", node.renderToString())
    }

    @Test
    fun rendersArrays() {
        assertEquals("[]", JsonNode.Arr().renderToString())
        assertEquals("""["a",1,true,null]""", arr(str("a"), num(1), bool(true), JsonNode.Null).renderToString())
        val list = arr(listOf(str("x")))
        list.add(str("y"))
        assertEquals("""["x","y"]""", list.renderToString())
    }

    @Test
    fun setDropsNullValuesAndOverwrites() {
        val node = obj("keep" to str("v"), "dropped" to null)
        assertEquals("""{"keep":"v"}""", node.renderToString())
        assertNull(node["dropped"])
        node["keep"] = str("v2")
        assertEquals("""{"keep":"v2"}""", node.renderToString())
    }

    @Test
    fun emptinessChecks() {
        val node = JsonNode.Obj()
        assertTrue(node.isEmpty())
        assertFalse(node.isNotEmpty())
        node["k"] = str("v")
        assertFalse(node.isEmpty())
        assertTrue(node.isNotEmpty())
        assertTrue(JsonNode.Arr().isEmpty())
        assertFalse(arr(num(1)).isEmpty())
    }

    @Test
    fun escapesStrings() {
        assertEquals("\"a\\\"b\"", str("a\"b").renderToString())
        assertEquals("\"a\\\\b\"", str("a\\b").renderToString())
        assertEquals("\"a\\nb\"", str("a\nb").renderToString())
        assertEquals("\"a\\rb\"", str("a\rb").renderToString())
        assertEquals("\"a\\tb\"", str("a\tb").renderToString())
        assertEquals("\"a\\bb\"", str("a\bb").renderToString())
        assertEquals("\"a\\fb\"", str("ab").renderToString())
        // Other control characters become \u escapes.
        assertEquals("\"a\\u0001b\"", str("ab").renderToString())
        assertEquals("\"a\\u001fb\"", str("ab").renderToString())
        // Non-control unicode passes through unescaped.
        assertEquals("\"aλ\"", str("aλ").renderToString())
    }

    /** Everything JsonNode renders must be valid JSON — kotlinx must be able to parse it back. */
    @Test
    fun renderedOutputIsValidJson() {
        val tricky = "quote \" backslash \\ newline \n tab \t control  unicode λ"
        val node = obj(
            "s" to str(tricky),
            "n" to num(9),
            "b" to bool(false),
            "arr" to arr(str("x"), JsonNode.Null, obj("nested" to num(1))),
            "empty" to obj(),
        )
        val parsed = Json.parseToJsonElement(node.renderToString()).jsonObject
        assertEquals(tricky, (parsed["s"] as JsonPrimitive).content)
        assertEquals("9", (parsed["n"] as JsonPrimitive).content)
        assertEquals(3, (parsed["arr"] as JsonArray).size)
    }
}
