package io.github.smyrgeorge.ktkit.compiler.openapi.utils

import java.util.concurrent.ConcurrentHashMap

class MetadataStore {

    class Entry(val metadata: Metadata, val warning: String? = null)

    private data class Key(val filePath: String, val offset: Int)

    private val entries = ConcurrentHashMap<Key, Entry>()

    fun put(filePath: String, startOffset: Int, endOffset: Int, entry: Entry) {
        entries[Key(filePath, startOffset)] = entry
        entries[Key(filePath, endOffset)] = entry
    }

    fun get(filePath: String, startOffset: Int, endOffset: Int): Entry? =
        entries[Key(filePath, startOffset)] ?: entries[Key(filePath, endOffset)]
}
