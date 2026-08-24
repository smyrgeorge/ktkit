package io.github.smyrgeorge.ktkit.compiler.openapi.ir.route

import io.github.smyrgeorge.ktkit.compiler.openapi.ir.classFq
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.schema.JsonNode
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.schema.JsonNode.Companion.arr
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.schema.JsonNode.Companion.bool
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.schema.JsonNode.Companion.obj
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.schema.JsonNode.Companion.str
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.schema.SchemaGenerator
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.schema.Schemas
import io.github.smyrgeorge.ktkit.compiler.openapi.utils.Metadata
import org.jetbrains.kotlin.ir.types.IrType

class OperationBuilder(
    private val schemas: SchemaGenerator,
    private val anonymous: Boolean,
    private val handlerName: String,
) {
    private val usedOperationIds = mutableSetOf<String>()

    /** The shared `components.responses` entries referenced by the built operations (name → status code). */
    val usedErrorResponses = sortedMapOf<String, Int>()

    fun build(
        verb: String,
        fullPath: String,
        pathParams: List<String>,
        successCode: Int,
        responseType: IrType?,
        streaming: Boolean,
        scan: HandlerLambdaScan,
        metadata: Metadata,
        defaultTag: String,
    ): JsonNode.Obj {
        val parameters = parameters(pathParams, scan)
        val requestBody = requestBody(scan)
        val responses = responses(verb, successCode, responseType, streaming, parameters, requestBody, scan)

        val operation = JsonNode.Obj()
        val tags = metadata.tags.ifEmpty { listOf(defaultTag) }
        operation["tags"] = arr(tags.map { str(it) })
        metadata.summary?.let { operation["summary"] = str(it) }
        val description = listOfNotNull(metadata.description, metadata.deprecated?.let { "Deprecated: $it" })
            .joinToString("\n\n").ifEmpty { null }
        description?.let { operation["description"] = str(it) }
        operation["operationId"] = str(uniqueOperationId(operationId(verb, fullPath)))
        if (metadata.deprecated != null) operation["deprecated"] = bool(true)
        if (parameters.items.isNotEmpty()) operation["parameters"] = parameters
        requestBody?.let { operation["requestBody"] = it }
        operation["responses"] = responses
        return operation
    }

    /** The operation's parameters: every `{param}` of the path first, then query and header parameters. */
    private fun parameters(pathParams: List<String>, scan: HandlerLambdaScan): JsonNode.Arr {
        val parameters = JsonNode.Arr()
        val emitted = mutableSetOf<String>()
        pathParams.forEach { name ->
            val info = scan.params["path:$name"]
            parameters.add(
                paramNode(
                    location = "path",
                    name = name,
                    required = true,
                    schema = info?.schema ?: Schemas.string(),
                    description = info?.description,
                )
            )
            emitted += "path:$name"
        }
        scan.params.values.forEach { info ->
            val key = "${info.location}:${info.name}"
            if (key in emitted || info.location == "path") return@forEach
            parameters.add(
                paramNode(
                    location = info.location,
                    name = info.name,
                    required = info.required,
                    schema = info.schema,
                    description = info.description,
                )
            )
            emitted += key
        }
        return parameters
    }

    private fun requestBody(scan: HandlerLambdaScan): JsonNode.Obj? =
        scan.bodyType?.let { bodyType ->
            val body = JsonNode.Obj()
            body["required"] = bool(true)
            body["content"] = obj("application/json" to obj("schema" to schemas.schemaFor(bodyType)))
            body
        }

    private fun responses(
        verb: String,
        successCode: Int,
        responseType: IrType?,
        streaming: Boolean,
        parameters: JsonNode.Arr,
        requestBody: JsonNode.Obj?,
        scan: HandlerLambdaScan,
    ): JsonNode.Obj {
        val responses = JsonNode.Obj()

        // The success response.
        val successDescription = HttpStatusCodes.phraseOf(successCode) ?: "Success"
        val responseTypeFq = responseType?.classFq()?.asString()
        val successContent: JsonNode.Obj? = when {
            verb == "HEAD" -> null
            responseType == null -> null
            responseTypeFq == "kotlin.Unit" || responseTypeFq == "kotlin.Nothing" -> null
            responseTypeFq == "kotlin.String" && !streaming ->
                obj("text/plain" to obj("schema" to Schemas.string()))

            else -> {
                var schema: JsonNode.Obj = schemas.schemaFor(responseType)
                if (streaming) schema = Schemas.arrayOf(schema)
                obj("application/json" to obj("schema" to schema))
            }
        }
        responses[successCode.toString()] = obj("description" to str(successDescription))
            .also { if (successContent != null) it["content"] = successContent }

        // The error responses (all referencing ktkit's standard ApiError schema).
        val errorCodes = sortedSetOf<Int>()
        if (parameters.items.isNotEmpty() || requestBody != null) errorCodes += 400
        if (!anonymous) {
            errorCodes += 401
            errorCodes += 403
        }
        // Every route can fail unexpectedly (ktkit's UnknownError maps to 500).
        errorCodes += 500
        errorCodes += scan.errorCodes
        errorCodes -= successCode
        // Error responses are identical everywhere, so each operation only references the shared
        // definition; the referenced components are emitted once into the fragment's
        // `components.responses` (see HandlerAnalyzer).
        errorCodes.forEach { code ->
            val name = HttpStatusCodes.nameOf(code) ?: "Error$code"
            usedErrorResponses[name] = code
            responses[code.toString()] = obj($$"$ref" to str("#/components/responses/$name"))
        }
        return responses
    }

    private fun paramNode(
        location: String,
        name: String,
        required: Boolean,
        schema: JsonNode.Obj,
        description: String?,
    ): JsonNode.Obj {
        val node = obj("name" to str(name), "in" to str(location))
        description?.let { node["description"] = str(it) }
        node["required"] = bool(required || location == "path")
        node["schema"] = schema
        return node
    }

    /**
     * `TestRestHandler__PUT__api_v1_test_update-and-fetch-all_by_id` for
     * `PUT /api/v1/test/update-and-fetch-all/{id}` — a `{param}` segment becomes `by_param`.
     */
    private fun operationId(verb: String, path: String): String {
        val segments = path.split('/').filter { it.isNotEmpty() }.joinToString("_") { segment ->
            if (segment.startsWith("{") && segment.endsWith("}")) "by_${segment.substring(1, segment.length - 1)}"
            else segment
        }
        return "${handlerName}__${verb}__$segments"
    }

    private fun uniqueOperationId(candidate: String): String {
        var id = candidate
        var counter = 2
        while (!usedOperationIds.add(id)) id = "${candidate}_${counter++}"
        return id
    }
}
