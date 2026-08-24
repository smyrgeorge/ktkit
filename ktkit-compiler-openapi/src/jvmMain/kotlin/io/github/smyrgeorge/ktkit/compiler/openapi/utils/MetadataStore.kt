package io.github.smyrgeorge.ktkit.compiler.openapi.utils

import java.util.concurrent.ConcurrentHashMap

class MetadataStore {

    class Entry(val metadata: Metadata, val warning: String? = null)

    private data class Key(val filePath: String, val offset: Int)

    private val entries = ConcurrentHashMap<Key, Entry>()
    private val infos = ConcurrentHashMap<Key, String>()

    fun put(filePath: String, startOffset: Int, endOffset: Int, entry: Entry) {
        entries[Key(filePath, startOffset)] = entry
        entries[Key(filePath, endOffset)] = entry
    }

    fun get(filePath: String, startOffset: Int, endOffset: Int): Entry? =
        entries[Key(filePath, startOffset)] ?: entries[Key(filePath, endOffset)]

    fun putInfo(filePath: String, startOffset: Int, endOffset: Int, description: String) {
        infos[Key(filePath, startOffset)] = description
        infos[Key(filePath, endOffset)] = description
    }

    fun getInfo(filePath: String, startOffset: Int, endOffset: Int): String? =
        infos[Key(filePath, startOffset)] ?: infos[Key(filePath, endOffset)]
}
