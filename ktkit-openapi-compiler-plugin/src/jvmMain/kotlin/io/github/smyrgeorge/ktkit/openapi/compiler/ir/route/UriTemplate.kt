package io.github.smyrgeorge.ktkit.openapi.compiler.ir.route

/**
 * The statically evaluated `String.uri()` implementation of a handler, as a template of string
 * literals and `$this` holes: `"/api/v1/test$this"` becomes `[Lit("/api/v1/test"), Hole]`, and
 * [apply] substitutes the per-route path into the holes. The result of [UriParser.parse].
 */
class UriTemplate internal constructor(private val parts: List<Part>) {

    internal sealed interface Part {
        class Lit(val value: String) : Part
        object Hole : Part
    }

    fun apply(path: String): String =
        parts.joinToString("") { if (it is Part.Lit) it.value else path }
}
