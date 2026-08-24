package io.github.smyrgeorge.ktkit.api.rest.openapi

import io.github.smyrgeorge.ktkit.Application
import io.github.smyrgeorge.ktkit.api.rest.AbstractRestHandler
import io.github.smyrgeorge.log4k.Logger
import io.github.smyrgeorge.log4k.classic.warn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * A utility object responsible for building and merging OpenAPI 3.1 specifications
 * across multiple handlers into a single compliant OpenAPI document.
 */
object OpenApiDocBuilder {
    private val log: Logger = Logger.of(this::class)
    private val json: Json = Json { prettyPrint = true }

    private const val SCHEMA_REF_PREFIX = "#/components/schemas/"
    private const val RESPONSE_REF_PREFIX = "#/components/responses/"

    /**
     * Builds an OpenAPI specification document in JSON format based on the provided application and REST handlers.
     *
     * @param app The application containing configuration details such as host, port, and OpenAPI metadata.
     * @param handlers A list of `AbstractRestHandler` objects whose OpenAPI fragments will be merged into the final document.
     * @return A string representation of the OpenAPI specification in JSON format.
     */
    fun build(app: Application, handlers: List<AbstractRestHandler>): String {
        val paths = mutableMapOf<String, MutableMap<String, JsonElement>>()
        val schemas = mutableMapOf<String, JsonElement>()
        val responses = mutableMapOf<String, JsonElement>()
        val usedOperationIds = mutableSetOf<String>()

        // Deterministic merge order, independent of DI discovery order.
        val fragments = handlers.mapNotNull { handler ->
            val fragment = runCatching { handler.openApiSpec() }.getOrNull() ?: return@mapNotNull null
            (handler::class.simpleName ?: "") to fragment
        }.sortedWith(compareBy({ it.first }, { it.second }))

        fragments.forEach { (handlerName, fragment) ->
            try {
                merge(handlerName, fragment, paths, schemas, responses, usedOperationIds)
            } catch (e: Exception) {
                log.warn { "Could not merge OpenAPI fragment of $handlerName: ${e.message}" }
            }
        }

        val conf = app.conf.openApi
        val doc = buildJsonObject {
            put("openapi", "3.1.0")
            putJsonObject("info") {
                put("title", conf.title ?: app.name)
                (conf.description ?: app.description)?.let { put("description", it) }
                put("version", conf.version)
            }
            putJsonArray("servers") {
                val servers = conf.servers.ifEmpty {
                    @Suppress("HttpUrlsUsage")
                    listOf("http://${app.conf.host}:${app.conf.port}")
                }
                servers.forEach { add(buildJsonObject { put("url", it) }) }
            }
            putJsonObject("paths") {
                paths.entries.sortedBy { it.key }.forEach { (path, operations) ->
                    put(path, JsonObject(operations))
                }
            }
            if (schemas.isNotEmpty() || responses.isNotEmpty()) {
                putJsonObject("components") {
                    if (schemas.isNotEmpty()) put(
                        "schemas",
                        JsonObject(schemas.entries.sortedBy { it.key }.associate { it.key to it.value })
                    )
                    if (responses.isNotEmpty()) put(
                        "responses",
                        JsonObject(responses.entries.sortedBy { it.key }.associate { it.key to it.value })
                    )
                }
            }
        }
        return json.encodeToString(JsonObject.serializer(), doc)
    }

    private fun merge(
        handlerName: String,
        fragment: String,
        paths: MutableMap<String, MutableMap<String, JsonElement>>,
        schemas: MutableMap<String, JsonElement>,
        responses: MutableMap<String, JsonElement>,
        usedOperationIds: MutableSet<String>,
    ) {
        var parsed = Json.parseToJsonElement(fragment).jsonObject

        // Resolve schema-name collisions first: same key, different content → rename the incoming
        // schema and rewrite every $ref inside this fragment (fragment $refs are self-contained).
        val fragmentSchemas = parsed["components"]?.jsonObject?.get("schemas")?.jsonObject ?: JsonObject(emptyMap())
        val renames = mutableMapOf<String, String>()
        fragmentSchemas.forEach { (name, schema) ->
            val existing = schemas[name]
            if (existing != null && existing != schema) {
                var counter = 2
                var candidate = "${name}_$counter"
                while (candidate in schemas || candidate in fragmentSchemas) candidate = "${name}_${++counter}"
                renames[name] = candidate
                log.warn { "OpenAPI schema '$name' of $handlerName conflicts with another handler's; renamed to '$candidate'." }
            }
        }
        if (renames.isNotEmpty()) parsed = rewriteRefs(parsed, renames, SCHEMA_REF_PREFIX).jsonObject

        parsed["components"]?.jsonObject?.get("schemas")?.jsonObject?.forEach { (name, schema) ->
            val key = renames[name] ?: name
            // Keep the displayed name (the `title` keyword) in sync with the renamed key.
            if (key !in schemas) schemas[key] = if (key != name) retitle(schema, key) else schema
        }

        // Shared component responses — compared after the schema rewrite, so two fragments whose
        // responses reference the same (possibly renamed) schemas dedupe cleanly.
        val fragmentResponses = parsed["components"]?.jsonObject?.get("responses")?.jsonObject ?: JsonObject(emptyMap())
        val responseRenames = mutableMapOf<String, String>()
        fragmentResponses.forEach { (name, response) ->
            val existing = responses[name]
            if (existing != null && existing != response) {
                var counter = 2
                var candidate = "${name}_$counter"
                while (candidate in responses || candidate in fragmentResponses) candidate = "${name}_${++counter}"
                responseRenames[name] = candidate
                log.warn { "OpenAPI response '$name' of $handlerName conflicts with another handler's; renamed to '$candidate'." }
            }
        }
        if (responseRenames.isNotEmpty()) parsed = rewriteRefs(parsed, responseRenames, RESPONSE_REF_PREFIX).jsonObject

        parsed["components"]?.jsonObject?.get("responses")?.jsonObject?.forEach { (name, response) ->
            val key = responseRenames[name] ?: name
            if (key !in responses) responses[key] = response
        }

        parsed["paths"]?.jsonObject?.forEach { (path, item) ->
            val operations = paths.getOrPut(path) { linkedMapOf() }
            item.jsonObject.forEach { (method, operation) ->
                if (operations.containsKey(method)) {
                    log.warn { "Duplicate OpenAPI operation '${method.uppercase()} $path' (from $handlerName), keeping the first one." }
                } else {
                    operations[method] = uniqueOperationId(operation, usedOperationIds)
                }
            }
        }
    }

    private fun uniqueOperationId(operation: JsonElement, used: MutableSet<String>): JsonElement {
        val obj = operation as? JsonObject ?: return operation
        val id = (obj["operationId"] as? JsonPrimitive)?.takeIf { it.isString }?.content ?: return operation
        if (used.add(id)) return operation
        var counter = 2
        var candidate = "${id}_$counter"
        while (!used.add(candidate)) candidate = "${id}_${++counter}"
        return JsonObject(obj.toMutableMap().apply { put("operationId", JsonPrimitive(candidate)) })
    }

    private fun retitle(schema: JsonElement, title: String): JsonElement {
        val obj = schema as? JsonObject ?: return schema
        return JsonObject(obj.toMutableMap().apply { put("title", JsonPrimitive(title)) })
    }

    private fun rewriteRefs(element: JsonElement, renames: Map<String, String>, prefix: String): JsonElement =
        when (element) {
            is JsonObject -> JsonObject(element.mapValues { (_, value) -> rewriteRefs(value, renames, prefix) })
            is JsonArray -> JsonArray(element.map { rewriteRefs(it, renames, prefix) })
            is JsonPrimitive ->
                if (element.isString && element.content.startsWith(prefix)) {
                    val key = element.content.removePrefix(prefix)
                    renames[key]?.let { JsonPrimitive(prefix + it) } ?: element
                } else element
        }
}
