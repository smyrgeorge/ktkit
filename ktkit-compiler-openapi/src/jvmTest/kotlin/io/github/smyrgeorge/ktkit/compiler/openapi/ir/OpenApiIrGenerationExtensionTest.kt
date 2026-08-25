package io.github.smyrgeorge.ktkit.compiler.openapi.ir

import io.github.smyrgeorge.ktkit.compiler.openapi.Compilations
import io.github.smyrgeorge.ktkit.compiler.openapi.obj
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OpenApiIrGenerationExtensionTest {

    @Test
    fun edgeCasesCompile() {
        Compilations.edgeCases.assertOk()
    }

    @Test
    fun classLevelOpenApiIgnoreDisablesTheHandler() {
        assertNull(Compilations.edgeCases.openApiSpec("warnings.IgnoredHandler"))
    }

    @Test
    fun concreteSubclassesOfAbstractHandlersAreAnalyzed() {
        // uri()/routes() live on the abstract intermediary of the same module — the concrete
        // class is analyzed through the chain, the abstract one itself gets no override.
        val fragment = Compilations.edgeCases.fragment("warnings.ConcreteOfAbstract")
        assertEquals(setOf("/base/route"), fragment.obj("paths").keys)
    }
}
