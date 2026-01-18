package io.github.smyrgeorge.ktkit.util

import kotlinx.io.Buffer
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

val FS = SystemFileSystem

fun String.toPath() = Path(this)

fun readEntireFileFromDisk(path: Path): String {
    val buffer = Buffer()
    val source = FS.source(path)
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

expect fun readEntireFileFromResources(path: Path): String
