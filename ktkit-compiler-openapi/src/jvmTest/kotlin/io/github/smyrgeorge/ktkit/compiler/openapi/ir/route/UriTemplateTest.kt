package io.github.smyrgeorge.ktkit.compiler.openapi.ir.route

import kotlin.test.Test
import kotlin.test.assertEquals

class UriTemplateTest {

    @Test
    fun substitutesThePathIntoHoles() {
        // "/api/v1/test$this"
        val template = UriTemplate(listOf(UriTemplate.Part.Lit("/api/v1/test"), UriTemplate.Part.Hole))
        assertEquals("/api/v1/test/x", template.apply("/x"))
        assertEquals("/api/v1/test", template.apply(""))
    }

    @Test
    fun supportsMultipleHolesAndLiterals() {
        // "$this-suffix$this"
        val template = UriTemplate(
            listOf(UriTemplate.Part.Hole, UriTemplate.Part.Lit("-suffix"), UriTemplate.Part.Hole)
        )
        assertEquals("/a-suffix/a", template.apply("/a"))
    }

    @Test
    fun literalOnlyTemplateIgnoresThePath() {
        val template = UriTemplate(listOf(UriTemplate.Part.Lit("/fixed")))
        assertEquals("/fixed", template.apply("/anything"))
    }

    @Test
    fun emptyTemplateRendersEmpty() {
        assertEquals("", UriTemplate(emptyList()).apply("/x"))
    }
}
