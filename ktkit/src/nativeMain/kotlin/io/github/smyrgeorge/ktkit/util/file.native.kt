package io.github.smyrgeorge.ktkit.util

import kotlinx.io.files.Path

private const val resources = "./resources"

actual fun readEntireFileFromResources(path: Path): String =
    readEntireFileFromDisk("$resources/$path".toPath())
