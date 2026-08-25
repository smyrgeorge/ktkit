package io.github.smyrgeorge.ktkit.compiler.openapi.ir.route

import io.github.smyrgeorge.ktkit.compiler.openapi.Compilations
import io.github.smyrgeorge.ktkit.compiler.openapi.obj
import kotlin.test.Test
import kotlin.test.assertEquals

class UriParserTest {

    @Test
    fun plusChainUriTemplatesAreEvaluated() {
        val fragment = Compilations.edgeCases.fragment("warnings.PlusUriHandler")
        assertEquals(setOf("/plus/chain/x"), fragment.obj("paths").keys)
    }
}
