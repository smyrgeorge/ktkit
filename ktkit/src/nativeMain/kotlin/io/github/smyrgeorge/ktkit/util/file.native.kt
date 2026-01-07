package io.github.smyrgeorge.ktkit.util

import kotlinx.io.files.Path

actual fun readEntireFileFromResources(path: Path): String =
    readEntireFileFromDisk(Path("resources/$path"))
