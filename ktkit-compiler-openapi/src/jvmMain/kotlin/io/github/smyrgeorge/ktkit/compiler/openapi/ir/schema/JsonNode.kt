package io.github.smyrgeorge.ktkit.compiler.openapi.ir.schema

sealed interface JsonNode {
    fun render(sb: StringBuilder)

    fun renderToString(): String = StringBuilder().also { render(it) }.toString()

    class Obj : JsonNode {
        val entries: LinkedHashMap<String, JsonNode> = LinkedHashMap()

        operator fun set(key: String, value: JsonNode?) {
            if (value != null) entries[key] = value
        }

        operator fun get(key: String): JsonNode? = entries[key]
        fun isEmpty(): Boolean = entries.isEmpty()
        fun isNotEmpty(): Boolean = entries.isNotEmpty()

        override fun render(sb: StringBuilder) {
            sb.append('{')
            var first = true
            entries.forEach { (key, value) ->
                if (!first) sb.append(',')
                first = false
                sb.appendQuoted(key).append(':')
                value.render(sb)
            }
            sb.append('}')
        }
    }

    class Arr(val items: MutableList<JsonNode> = mutableListOf()) : JsonNode {
        fun add(node: JsonNode) {
            items.add(node)
        }

        fun isEmpty(): Boolean = items.isEmpty()

        override fun render(sb: StringBuilder) {
            sb.append('[')
            items.forEachIndexed { i, item ->
                if (i > 0) sb.append(',')
                item.render(sb)
            }
            sb.append(']')
        }
    }

    class Str(val value: String) : JsonNode {
        override fun render(sb: StringBuilder) {
            sb.appendQuoted(value)
        }
    }

    class Num(val value: Long) : JsonNode {
        override fun render(sb: StringBuilder) {
            sb.append(value)
        }
    }

    class Bool(val value: Boolean) : JsonNode {
        override fun render(sb: StringBuilder) {
            sb.append(value)
        }
    }

    object Null : JsonNode {
        override fun render(sb: StringBuilder) {
            sb.append("null")
        }
    }

    companion object {
        fun obj(vararg pairs: Pair<String, JsonNode?>): Obj = Obj().apply { pairs.forEach { (k, v) -> set(k, v) } }
        fun arr(items: List<JsonNode>): Arr = Arr(items.toMutableList())
        fun arr(vararg items: JsonNode): Arr = Arr(items.toMutableList())
        fun str(value: String): Str = Str(value)
        fun num(value: Int): Num = Num(value.toLong())
        fun bool(value: Boolean): Bool = Bool(value)

        private fun StringBuilder.appendQuoted(value: String): StringBuilder {
            append('"')
            value.forEach { c ->
                when (c) {
                    '"' -> append("\\\"")
                    '\\' -> append("\\\\")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    '\b' -> append("\\b")
                    '\u000C' -> append("\\f")
                    else ->
                        if (c < ' ') append("\\u").append(c.code.toString(16).padStart(4, '0'))
                        else append(c)
                }
            }
            append('"')
            return this
        }
    }
}
