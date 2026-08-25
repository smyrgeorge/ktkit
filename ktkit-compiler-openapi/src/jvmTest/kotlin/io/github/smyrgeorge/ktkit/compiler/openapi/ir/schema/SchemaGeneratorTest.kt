package io.github.smyrgeorge.ktkit.compiler.openapi.ir.schema

import io.github.smyrgeorge.ktkit.compiler.openapi.Compilations
import io.github.smyrgeorge.ktkit.compiler.openapi.arr
import io.github.smyrgeorge.ktkit.compiler.openapi.obj
import io.github.smyrgeorge.ktkit.compiler.openapi.operation
import io.github.smyrgeorge.ktkit.compiler.openapi.refOf
import io.github.smyrgeorge.ktkit.compiler.openapi.str
import io.github.smyrgeorge.ktkit.compiler.openapi.strings
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SchemaGeneratorTest {

    private val fragment: JsonObject get() = Compilations.schemas.fragment("schemagen.SchemaRestHandler")
    private val schemas: JsonObject get() = fragment.obj("components").obj("schemas")

    @Test
    fun compilationSucceedsWithTheExpectedWarnings() {
        Compilations.schemas.assertOk()
        assertTrue(
            Compilations.schemas.warnings.any { "schemagen.Custom" in it && "custom serializer" in it },
            "expected a custom-serializer warning, got: ${Compilations.schemas.warnings}"
        )
        assertTrue(
            Compilations.schemas.warnings.any { "schemagen.Plain" in it && "not @Serializable" in it },
            "expected a non-serializable warning, got: ${Compilations.schemas.warnings}"
        )
    }

    @Test
    fun propertiesRespectSerialNameTransientNullabilityAndDefaults() {
        val props = schemas.obj("PropsDto")
        assertEquals("PropsDto", props.str("title"))
        assertEquals("object", props.str("type"))
        // Declaration order, @SerialName applied, @Transient omitted.
        assertEquals(listOf("req", "renamed", "defaulted", "id", "at"), props.obj("properties").keys.toList())
        assertEquals("string", props.obj("properties").obj("req").str("type"))
        // Required = non-nullable without a default value.
        assertEquals(listOf("req", "renamed", "id", "at"), props.strings("required"))
        // Formats.
        assertEquals("uuid", props.obj("properties").obj("id").str("format"))
        assertEquals("date-time", props.obj("properties").obj("at").str("format"))
    }

    @Test
    fun nullablePropertiesAllowNull() {
        val opt = schemas.obj("NullableDto").obj("properties").obj("opt")
        assertEquals(listOf("string", "null"), opt.strings("type"))
        val maybe = schemas.obj("NullableDto").obj("properties").obj("maybe")
        assertEquals(listOf("string", "null"), maybe.strings("type"))
        assertTrue(maybe.arr("enum").contains(JsonNull), "a nullable enum lists null explicitly")
        assertNull(schemas.obj("NullableDto")["required"], "every property is nullable or defaulted")
    }

    @Test
    fun enumPropertiesListEntries() {
        val status = schemas.obj("NullableDto").obj("properties").obj("status")
        assertEquals("string", status.str("type"))
        assertEquals(listOf("ACTIVE", "INACTIVE"), status.strings("enum"))
    }

    @Test
    fun inheritedPropertiesComeFirst() {
        val child = schemas.obj("ChildDto")
        assertEquals(listOf("common", "own"), child.obj("properties").keys.toList())
        assertEquals(listOf("common"), child.strings("required"), "'own' has a default")
    }

    @Test
    fun sealedHierarchiesBecomeOneOfWithDiscriminator() {
        val shape = schemas.obj("Shape")
        val oneOf = shape.arr("oneOf").map { (it as JsonObject).str($$"$ref") }
        assertEquals(
            listOf("#/components/schemas/Circle", "#/components/schemas/Square"),
            oneOf
        )
        val discriminator = shape.obj("discriminator")
        assertEquals("@type", discriminator.str("propertyName"))
        assertEquals(
            "#/components/schemas/Circle",
            discriminator.obj("mapping").str("circle"),
            "@SerialName drives the mapping key"
        )
        assertEquals(
            "#/components/schemas/Square",
            discriminator.obj("mapping").str("schemagen.Square"),
            "unnamed leaves map by their fully qualified name"
        )
        // Each leaf declares the optional @type discriminator property.
        assertEquals("string", schemas.obj("Circle").obj("properties").obj("@type").str("type"))
        assertTrue("@type" !in schemas.obj("Circle").strings("required").toSet())
    }

    @Test
    fun genericInstantiationsGetTheirOwnComponent() {
        val page = schemas.obj("PageOfItemDto")
        assertEquals("PageOfItemDto", page.str("title"))
        val items = page.obj("properties").obj("items")
        assertEquals("array", items.str("type"))
        assertEquals("#/components/schemas/ItemDto", items.refOf("items"))
        assertEquals("int64", page.obj("properties").obj("total").str("format"))
    }

    @Test
    fun valueClassesSerializeAsTheirUnderlyingType() {
        val userId = schemas.obj("WithValueClass").obj("properties").obj("userId")
        assertEquals("integer", userId.str("type"))
        assertEquals("int64", userId.str("format"))
    }

    @Test
    fun recursiveTypesAreRepresentedWithRefs() {
        val node = schemas.obj("Node")
        assertEquals("#/components/schemas/Node", node.obj("properties").obj("children").refOf("items"))
    }

    @Test
    fun mapsBecomeAdditionalProperties() {
        val schema = fragment.operation("get", "/s/map")
            .obj("responses").obj("200").obj("content").obj("application/json").obj("schema")
        assertEquals("object", schema.str("type"))
        assertEquals("#/components/schemas/ItemDto", schema.refOf("additionalProperties"))
    }

    @Test
    fun openApiInfoDescribesClassesAndProperties() {
        val item = schemas.obj("ItemDto")
        assertEquals("An item of the system.", item.str("description"))
        assertEquals("The id.", item.obj("properties").obj("id").str("description"))
    }

    @Test
    fun customSerializersAndNonSerializableTypesAreFreeForm() {
        val custom = fragment.operation("get", "/s/custom")
            .obj("responses").obj("200").obj("content").obj("application/json").obj("schema")
        assertEquals(emptySet(), custom.keys, "a custom serializer yields a free-form schema")
        val plain = fragment.operation("get", "/s/plain")
            .obj("responses").obj("200").obj("content").obj("application/json").obj("schema")
        assertEquals(emptySet(), plain.keys, "a non-@Serializable type yields a free-form schema")
    }

    @Test
    fun simpleNameCollisionsFallBackToFullyQualifiedKeys() {
        assertTrue("Data" in schemas.keys, "${schemas.keys}")
        assertTrue("p3.Data" in schemas.keys, "the second 'Data' is keyed by its FQ name: ${schemas.keys}")
        assertEquals("p3.Data", schemas.obj("p3.Data").str("title"), "the title follows the renamed key")
    }

    @Test
    fun objectsAreEmptyObjectSchemas() {
        val singleton = schemas.obj("SingletonDto")
        assertEquals("object", singleton.str("type"))
        assertNull(singleton["properties"])
    }
}
