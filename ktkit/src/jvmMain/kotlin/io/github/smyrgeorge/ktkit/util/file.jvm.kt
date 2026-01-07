package io.github.smyrgeorge.ktkit.util

import kotlinx.io.files.Path

actual fun readEntireFileFromResources(path: Path): String =
    ClassLoader.getSystemResourceAsStream(path.toString())?.bufferedReader()?.readText()
        ?: throw IllegalArgumentException("Resource not found: $path")
