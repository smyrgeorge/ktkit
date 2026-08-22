package io.github.smyrgeorge.ktkit.openapi.compiler.ir.route

import io.github.smyrgeorge.ktkit.openapi.compiler.ir.schema.JsonNode

/** A request parameter detected in a route lambda: its location (`path`/`query`/`header`), name, schema, requiredness. */
class ParamInfo(
    val location: String,
    val name: String,
    var schema: JsonNode.Obj,
    var required: Boolean,
)
