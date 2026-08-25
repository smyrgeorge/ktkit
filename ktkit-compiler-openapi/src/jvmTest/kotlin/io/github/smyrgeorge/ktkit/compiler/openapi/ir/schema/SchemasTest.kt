package io.github.smyrgeorge.ktkit.compiler.openapi.ir.schema

import io.github.smyrgeorge.ktkit.compiler.openapi.ir.schema.JsonNode.Companion.arr
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.schema.JsonNode.Companion.str
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SchemasTest {

    @Test
    fun primitiveShapes() {
        assertEquals("""{"type":"string"}""", Schemas.string().renderToString())
        assertEquals("""{"type":"integer","format":"int32"}""", Schemas.int32().renderToString())
        assertEquals("""{"type":"integer","format":"int64"}""", Schemas.int64().renderToString())
        assertEquals("""{"type":"number","format":"float"}""", Schemas.float().renderToString())
        assertEquals("""{"type":"number","format":"double"}""", Schemas.double().renderToString())
        assertEquals("""{"type":"boolean"}""", Schemas.boolean().renderToString())
        assertEquals("""{"type":"string","format":"uuid"}""", Schemas.uuid().renderToString())
        assertEquals("""{"type":"object"}""", Schemas.objectType().renderToString())
    }

    @Test
    fun compositeShapes() {
        assertEquals(
            """{"type":"array","items":{"type":"string"}}""",
            Schemas.arrayOf(Schemas.string()).renderToString()
        )
        assertEquals(
            """{"type":"array","items":{"type":"string"}}""",
            Schemas.stringArray().renderToString()
        )
        assertEquals(
            """{"application/json":{"schema":{"type":"boolean"}}}""",
            Schemas.jsonContent(Schemas.boolean()).renderToString()
        )
    }

    @Test
    fun fromConversionMapsEveryVarConversion() {
        assertEquals("""{"type":"string"}""", Schemas.fromConversion("asString")?.renderToString())
        assertEquals("""{"type":"integer","format":"int32"}""", Schemas.fromConversion("asInt")?.renderToString())
        assertEquals("""{"type":"integer","format":"int64"}""", Schemas.fromConversion("asLong")?.renderToString())
        assertEquals("""{"type":"number","format":"float"}""", Schemas.fromConversion("asFloat")?.renderToString())
        assertEquals("""{"type":"number","format":"double"}""", Schemas.fromConversion("asDouble")?.renderToString())
        assertEquals("""{"type":"boolean"}""", Schemas.fromConversion("asBoolean")?.renderToString())
        assertEquals("""{"type":"string","format":"uuid"}""", Schemas.fromConversion("asUuid")?.renderToString())
        // Enum and unknown conversions have no fixed schema.
        assertNull(Schemas.fromConversion("asEnum"))
        assertNull(Schemas.fromConversion("asSomethingElse"))
    }

    /**
     * Every factory must return a FRESH node per call: callers mutate the returned schema in place
     * (e.g. SchemaGenerator.nullable), so shared instances would corrupt unrelated schemas.
     */
    @Test
    fun factoriesReturnFreshInstances() {
        val first = Schemas.string()
        first["type"] = arr(str("string"), str("null")) // what nullable() does
        assertEquals("""{"type":"string"}""", Schemas.string().renderToString())

        val array = Schemas.stringArray()
        (array["items"] as JsonNode.Obj)["nullable"] = str("mutated")
        assertEquals("""{"type":"array","items":{"type":"string"}}""", Schemas.stringArray().renderToString())
    }
}
