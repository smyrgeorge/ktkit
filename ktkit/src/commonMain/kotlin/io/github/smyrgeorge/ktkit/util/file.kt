package io.github.smyrgeorge.ktkit.util

import kotlinx.io.Buffer
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

private val fs = SystemFileSystem

fun readEntireFileUtf8(path: Path): String {
    val buffer = Buffer()
    val source = fs.source(path)
    try {
        source.readAtMostTo(buffer, Int.MAX_VALUE.toLong())
        return buffer.readByteArray().decodeToString() // UTF-8 by default
    } catch (e: Exception) {
        println("Could not read file: ${e.message}")
        throw e
    } finally {
        source.close()
        buffer.close()
    }
}
