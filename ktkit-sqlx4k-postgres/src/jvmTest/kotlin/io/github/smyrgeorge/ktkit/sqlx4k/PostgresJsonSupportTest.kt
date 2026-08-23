package io.github.smyrgeorge.ktkit.sqlx4k

import io.r2dbc.postgresql.codec.Json as PgJson
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PostgresJsonSupportTest {

    @Test
    fun `encode produces a postgres json value with the snake_case payload`() {
        val encoded = PostgresJsonSupport.encoder(Account::class).encode(Account(fullName = "Yorgos")) as PgJson

        // The encoder wraps the payload in r2dbc's Json type, as the postgres driver expects.
        assertEquals("""{"full_name":"Yorgos","age":3}""", encoded.asString())
    }

    @Test
    fun `values round-trip through a column`() {
        val encoder = PostgresJsonSupport.encoder(Account::class)
        val original = Account(fullName = "Yorgos S.", age = 42, nickName = "smyrgeorge")
        assertEquals(original, encoder.decode(column((encoder.encode(original) as PgJson).asString())))
    }

    @Test
    fun `a custom json configuration is respected`() {
        val encoded = PostgresJsonSupport.encoder(Account::class, Json).encode(Account(fullName = "Yorgos")) as PgJson
        assertEquals("""{"fullName":"Yorgos"}""", encoded.asString())
    }

    @Test
    fun `a sealed hierarchy is registered with one shared encoder and a discriminator`() {
        val registry = PostgresJsonSupport.encoders(setOf(Change::class))

        val parent = registry.get(Change::class)
        assertNotNull(parent)
        assertEquals(parent, registry.get(Change.Added::class))
        assertEquals(parent, registry.get(Change.Removed::class))
        assertNull(registry.get(String::class))

        val encoded = parent.encode(Change.Added(id = 1)) as PgJson
        assertEquals("""{"@type":"added","id":1}""", encoded.asString())
        assertEquals(Change.Added(id = 1), parent.decode(column(encoded.asString())))
    }
}
