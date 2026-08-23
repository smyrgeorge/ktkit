package io.github.smyrgeorge.ktkit.compiler.openapi.utils

data class Metadata(
    val summary: String?,
    val description: String?,
    val tags: List<String>,
    val deprecated: String?,
    val operationId: String?,
    val ignore: Boolean,
    val securityNone: Boolean,
    val params: List<Param>,
    val responses: List<Response>,
    val bodyDescription: String?,
) {
    data class Param(val location: String, val name: String, val type: String?, val description: String?)
    data class Response(val code: Int, val description: String)

    fun param(location: String, name: String): Param? =
        params.firstOrNull { it.location == location && it.name == name }

    companion object {
        val EMPTY = Metadata(
            summary = null,
            description = null,
            tags = emptyList(),
            deprecated = null,
            operationId = null,
            ignore = false,
            securityNone = false,
            params = emptyList(),
            responses = emptyList(),
            bodyDescription = null
        )
    }
}
