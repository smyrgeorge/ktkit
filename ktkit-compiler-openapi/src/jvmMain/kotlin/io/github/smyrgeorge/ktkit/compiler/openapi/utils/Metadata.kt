package io.github.smyrgeorge.ktkit.compiler.openapi.utils

data class Metadata(
    val summary: String?,
    val description: String?,
    val tags: List<String>,
    val deprecated: String?,
    val ignore: Boolean = false,
) {
    companion object {
        val EMPTY = Metadata(
            summary = null,
            description = null,
            tags = emptyList(),
            deprecated = null,
        )
    }
}
