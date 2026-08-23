package io.github.smyrgeorge.ktkit.sqlx4k

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ReflectionJvmTest {

    @Test
    fun `sealedSubclasses returns the direct subclasses`() {
        assertEquals(
            setOf(Event.Created::class, Event.Deleted::class),
            Event::class.sealedSubclasses().toSet(),
        )
    }

    @Test
    fun `sealedSubclasses of a non-sealed class is empty`() {
        assertEquals(emptyList(), Sample::class.sealedSubclasses())
    }

    @Test
    fun `a sealed hierarchy is registered with one shared encoder`() {
        val registry = JsonSupport.encoders(setOf(Event::class))

        val parent = registry.get(Event::class)
        assertNotNull(parent)
        assertEquals(parent, registry.get(Event.Created::class))
        assertEquals(parent, registry.get(Event.Deleted::class))
    }
}
