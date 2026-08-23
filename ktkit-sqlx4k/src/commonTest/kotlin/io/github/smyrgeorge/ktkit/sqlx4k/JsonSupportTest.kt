package io.github.smyrgeorge.ktkit.sqlx4k

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JsonSupportTest {

    @Test
    fun `encode uses snake_case keeps defaults and omits nulls`() {
        val encoded = JsonSupport.encoder(Sample::class).encode(Sample(fullName = "Yorgos"))
        assertEquals("""{"full_name":"Yorgos","age":3}""", encoded)
    }

    @Test
    fun `decode reads snake_case and ignores unknown keys`() {
        val decoded = JsonSupport.encoder(Sample::class)
            .decode(column("""{"full_name":"Yorgos","age":7,"unknown":true}"""))
        assertEquals(Sample(fullName = "Yorgos", age = 7), decoded)
    }

    @Test
    fun `values round-trip through a column`() {
        val encoder = JsonSupport.encoder(Sample::class)
        val original = Sample(fullName = "Yorgos S.", age = 42, nickName = "smyrgeorge")
        assertEquals(original, encoder.decode(column(encoder.encode(original) as String)))
    }

    @Test
    fun `a custom json configuration is respected`() {
        val encoded = JsonSupport.encoder(Sample::class, Json).encode(Sample(fullName = "Yorgos"))
        assertEquals("""{"fullName":"Yorgos"}""", encoded)
    }

    @Test
    fun `encoders for a class and explicit subclasses share one encoder`() {
        val pairs = JsonSupport.encoders(Event::class, setOf(Event.Created::class, Event.Deleted::class))

        assertEquals(3, pairs.size)
        assertEquals(1, pairs.map { it.second }.distinct().size)
        assertEquals(
            setOf(Event::class, Event.Created::class, Event.Deleted::class),
            pairs.map { it.first }.toSet(),
        )
    }

    @Test
    fun `polymorphic encoding carries the class discriminator`() {
        val encoder = JsonSupport.encoders(Event::class, setOf(Event.Created::class, Event.Deleted::class))
            .first().second

        val encoded = encoder.encode(Event.Created(id = 1)) as String
        assertEquals("""{"@type":"created","id":1}""", encoded)
        assertEquals(Event.Created(id = 1), encoder.decode(column(encoded)))
    }

    @Test
    fun `the registry resolves registered types and misses others`() {
        val registry = JsonSupport.encoders(setOf(Sample::class))

        val encoder = registry.get(Sample::class)
        assertNotNull(encoder)
        assertNull(registry.get(String::class))
        assertTrue(encoder.encode(Sample(fullName = "A")).toString().contains("full_name"))
    }
}
