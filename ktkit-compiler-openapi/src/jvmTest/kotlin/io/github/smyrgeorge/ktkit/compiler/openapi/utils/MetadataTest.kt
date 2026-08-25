package io.github.smyrgeorge.ktkit.compiler.openapi.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class MetadataTest {

    @Test
    fun emptyMetadataDefaults() {
        val empty = Metadata.EMPTY
        assertNull(empty.summary)
        assertNull(empty.description)
        assertEquals(emptyList(), empty.tags)
        assertNull(empty.deprecated)
        assertFalse(empty.ignore)
        // The ignore-only entry used for bare @OpenApiIgnore.
        assertEquals(true, Metadata.EMPTY.copy(ignore = true).ignore)
    }
}
