package io.github.smyrgeorge.ktkit.compiler.openapi.ir.route

import io.github.smyrgeorge.ktkit.compiler.openapi.ir.schema.JsonNode

/** A request parameter detected in a route lambda: its location (`path`/`query`/`header`), name, schema, requiredness. */
class ParamInfo(
    val location: String,
    val name: String,
    var schema: JsonNode.Obj,
    var required: Boolean,
    var description: String? = null,
) {
    /** This parameter's key in [HandlerLambdaScan.params]. */
    val key: String get() = key(location, name)

    companion object {
        /** The `"<location>:<name>"` key format of [HandlerLambdaScan.params]. */
        fun key(location: String, name: String): String = "$location:$name"
    }
}
