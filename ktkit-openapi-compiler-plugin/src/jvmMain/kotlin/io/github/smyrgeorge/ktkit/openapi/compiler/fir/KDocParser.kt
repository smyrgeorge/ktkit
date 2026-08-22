package io.github.smyrgeorge.ktkit.openapi.compiler.fir

import io.github.smyrgeorge.ktkit.openapi.compiler.utils.Metadata

/**
 * Extracts and parses the KDoc block placed directly above a route call into a [Metadata] — the
 * fallback of the FIR collector when no `@OpenApi(...)` annotation is present:
 *
 * ```
 * /**
 *  * Returns a single user by id.
 *  *
 *  * A longer, multi-line description.
 *  *
 *  * Tag: users
 *  * Path: id [Int] The id of the user.
 *  * Query: verbose [Boolean] Whether to include details.
 *  * Response: 404 The user was not found.
 *  * Deprecated: Use /api/v2/users instead.
 *  */
 * GET("/{id}") { ... }
 * ```
 *
 * Supported tags (case-insensitive, `Tag: value` syntax):
 * - `Tag:` — tag name(s), comma-separated, repeatable.
 * - `Path:` / `Query:` / `Header:` — parameter documentation: `name [Type] description`
 *   (or `[Type] name description`). The `[Type]` is optional.
 * - `Body:` — request body description.
 * - `Response:` — `code description`, repeatable. `Responses:` starts a block of `- code description` lines.
 * - `Description:` — appends to the description.
 * - `Deprecated:` — marks the operation deprecated (the value is appended to the description).
 * - `OperationId:` — overrides the generated operationId.
 * - `Security: none` — suppresses the authentication error responses (401/403) of the operation.
 * - `Ignore:` — excludes the route from the specification.
 *
 * Everything before the first recognized tag is the summary (first line) and description (rest).
 */
object KDocParser {

    private val TAG_REGEX = Regex("^([A-Za-z]+)\\s*:\\s*(.*)$")
    private val RESPONSE_LINE_REGEX = Regex("^[-–—]?\\s*(\\d{3})\\s*(?:\\[[^]]*]\\s*)?(.*)$")
    private val KNOWN_TAGS = setOf(
        "tag", "path", "query", "header", "body", "response", "responses",
        "deprecated", "description", "operationid", "security", "ignore",
    )

    /**
     * Extracts the content of a KDoc comment (`/** ... */`) that immediately precedes
     * [callStartOffset] in [fileText] (only whitespace and `//` line comments in between),
     * or `null`.
     */
    fun extract(fileText: String, callStartOffset: Int): String? {
        if (callStartOffset <= 0 || callStartOffset > fileText.length) return null
        val i = skipTriviaBackwards(fileText, callStartOffset - 1)
        if (i < 3 || fileText[i] != '/' || fileText[i - 1] != '*') return null
        val start = fileText.lastIndexOf("/**", i - 1)
        if (start < 0 || start + 3 > i - 1) return null
        // The opener must belong to the closer we scanned back from — otherwise a plain
        // `/* ... */` comment above the call would absorb an earlier KDoc plus the source
        // code in between.
        if (fileText.indexOf("*/", start + 3) != i - 1) return null
        return fileText.substring(start + 3, i - 1)
    }

    /** Steps back over whitespace and `//` line comments, returning the index of the last relevant char. */
    private fun skipTriviaBackwards(fileText: String, from: Int): Int {
        var i = from
        while (true) {
            while (i >= 0 && fileText[i].isWhitespace()) i--
            if (i < 0) return i
            val lineStart = fileText.lastIndexOf('\n', i) + 1
            val line = fileText.substring(lineStart, i + 1).trimStart()
            if (line.startsWith("//")) i = lineStart - 1 else return i
        }
    }

    fun parse(raw: String?): Metadata {
        if (raw == null) return Metadata.EMPTY
        val lines = raw.lines().map { it.trim().removePrefix("*").trim() }

        val free = mutableListOf<String>()
        val tags = mutableListOf<String>()
        val params = mutableListOf<Metadata.Param>()
        val responses = mutableListOf<Metadata.Response>()
        var deprecated: String? = null
        var operationId: String? = null
        var ignore = false
        var securityNone = false
        var bodyDescription: String? = null
        var extraDescription = ""
        var inResponsesBlock = false
        var sawTag = false

        lines.forEach { line ->
            if (line.isEmpty()) {
                if (!sawTag) free += ""
                return@forEach
            }
            if (inResponsesBlock) {
                val m = RESPONSE_LINE_REGEX.matchEntire(line)
                if (m != null) {
                    responses += Metadata.Response(m.groupValues[1].toInt(), m.groupValues[2].trim())
                    return@forEach
                }
                inResponsesBlock = false
            }
            val tagMatch = TAG_REGEX.matchEntire(line)
            val tagName = tagMatch?.groupValues?.get(1)?.lowercase()
            if (tagMatch == null || tagName !in KNOWN_TAGS) {
                if (!sawTag) free += line else extraDescription += (if (extraDescription.isEmpty()) "" else "\n") + line
                return@forEach
            }
            sawTag = true
            val value = tagMatch.groupValues[2].trim()
            when (tagName) {
                "tag" -> tags += value.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                "path", "query", "header" -> parseParam(tagName, value)?.let { params += it }
                "body" -> bodyDescription = value.ifEmpty { null }
                "response" -> RESPONSE_LINE_REGEX.matchEntire(value)
                    ?.let { responses += Metadata.Response(it.groupValues[1].toInt(), it.groupValues[2].trim()) }

                "responses" -> inResponsesBlock = true
                "deprecated" -> deprecated = value.ifEmpty { "Deprecated." }
                "description" -> extraDescription += (if (extraDescription.isEmpty()) "" else "\n") + value
                "operationid" -> operationId = value.ifEmpty { null }
                "security" -> if (value.equals("none", ignoreCase = true)) securityNone = true
                "ignore" -> ignore = true
            }
        }

        val freeText = free.joinToString("\n").trim().lines()
        val summary = freeText.firstOrNull()?.trim()?.ifEmpty { null }
        var description = freeText.drop(1).joinToString("\n").trim().ifEmpty { null }
        if (extraDescription.isNotEmpty()) {
            description = listOfNotNull(description, extraDescription.trim()).joinToString("\n")
        }

        return Metadata(
            summary = summary,
            description = description,
            tags = tags,
            deprecated = deprecated,
            operationId = operationId,
            ignore = ignore,
            securityNone = securityNone,
            params = params,
            responses = responses,
            bodyDescription = bodyDescription,
        )
    }

    /** Parses `name [Type] description` or `[Type] name description`. */
    private fun parseParam(location: String, value: String): Metadata.Param? {
        var rest = value.trim()
        if (rest.isEmpty()) return null
        var type: String? = null
        val name: String

        if (rest.startsWith('[')) {
            val end = rest.indexOf(']')
            if (end < 0) return null
            type = rest.substring(1, end).trim()
            rest = rest.substring(end + 1).trim()
            name = rest.substringBefore(' ').trim().ifEmpty { return null }
            rest = rest.substringAfter(' ', "").trim()
        } else {
            name = rest.substringBefore(' ').trim().ifEmpty { return null }
            rest = rest.substringAfter(' ', "").trim()
            if (rest.startsWith('[')) {
                val end = rest.indexOf(']')
                if (end > 0) {
                    type = rest.substring(1, end).trim()
                    rest = rest.substring(end + 1).trim()
                }
            }
        }
        return Metadata.Param(location, name, type?.ifEmpty { null }, rest.ifEmpty { null })
    }
}
