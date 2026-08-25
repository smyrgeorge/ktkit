package io.github.smyrgeorge.ktkit.compiler.openapi.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MetadataStoreTest {

    private val metadata = Metadata(summary = "s", description = "d", tags = listOf("t"), deprecated = null)

    @Test
    fun entriesAreFoundByStartOrEndOffset() {
        val store = MetadataStore()
        store.put("File.kt", 10, 20, MetadataStore.Entry(metadata))
        // The FIR (source) offsets and the IR offsets of the same call may disagree on one end —
        // the store indexes both so either one resolves.
        assertEquals(metadata, store.get("File.kt", 10, 999)?.metadata)
        assertEquals(metadata, store.get("File.kt", 999, 20)?.metadata)
        assertNull(store.get("File.kt", 11, 21))
        assertNull(store.get("Other.kt", 10, 20))
    }

    @Test
    fun entriesCarryTheOptionalWarning() {
        val store = MetadataStore()
        store.put("File.kt", 1, 2, MetadataStore.Entry(Metadata.EMPTY, warning = "boom"))
        assertEquals("boom", store.get("File.kt", 1, 2)?.warning)
        store.put("File.kt", 3, 4, MetadataStore.Entry(Metadata.EMPTY))
        assertNull(store.get("File.kt", 3, 4)?.warning)
    }

    @Test
    fun infosAreFoundByStartOrEndOffset() {
        val store = MetadataStore()
        store.putInfo("File.kt", 5, 9, "The id of the user.")
        assertEquals("The id of the user.", store.getInfo("File.kt", 5, 123))
        assertEquals("The id of the user.", store.getInfo("File.kt", 123, 9))
        assertNull(store.getInfo("File.kt", 6, 10))
        assertNull(store.getInfo("Other.kt", 5, 9))
    }

    @Test
    fun entriesAndInfosAreIndependent() {
        val store = MetadataStore()
        store.put("File.kt", 1, 2, MetadataStore.Entry(metadata))
        assertNull(store.getInfo("File.kt", 1, 2))
        store.putInfo("File.kt", 1, 2, "info")
        assertEquals(metadata, store.get("File.kt", 1, 2)?.metadata)
        assertEquals("info", store.getInfo("File.kt", 1, 2))
    }
}
