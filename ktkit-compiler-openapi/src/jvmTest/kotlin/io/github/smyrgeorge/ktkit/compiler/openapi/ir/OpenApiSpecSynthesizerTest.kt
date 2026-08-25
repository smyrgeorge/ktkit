package io.github.smyrgeorge.ktkit.compiler.openapi.ir

import io.github.smyrgeorge.ktkit.compiler.openapi.Compilations
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OpenApiSpecSynthesizerTest {

    @Test
    fun synthesizedOverridesReplaceTheBaseNull() {
        val spec = Compilations.handlers.openApiSpec("analysis.OpenRestHandler")
        assertNotNull(spec, "the override must return the fragment instead of the base's null")
        assertTrue(spec.startsWith("""{"x-handler":"analysis.OpenRestHandler""""), spec)
    }

    @Test
    fun handWrittenOverridesAreRespected() {
        assertEquals("""{"hand":"written"}""", Compilations.edgeCases.openApiSpec("warnings.HandWrittenHandler"))
    }
}
