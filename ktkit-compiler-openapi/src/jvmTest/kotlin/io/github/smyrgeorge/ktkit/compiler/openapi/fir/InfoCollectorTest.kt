package io.github.smyrgeorge.ktkit.compiler.openapi.fir

import io.github.smyrgeorge.ktkit.compiler.openapi.Compilations
import io.github.smyrgeorge.ktkit.compiler.openapi.obj
import io.github.smyrgeorge.ktkit.compiler.openapi.operation
import io.github.smyrgeorge.ktkit.compiler.openapi.parametersByName
import io.github.smyrgeorge.ktkit.compiler.openapi.str
import kotlin.test.Test
import kotlin.test.assertEquals

class InfoCollectorTest {

    @Test
    fun localValOpenApiInfoDescribesTheParameter() {
        val limit = Compilations.handlers.fragment("analysis.ItemsRestHandler")
            .operation("get", "/api/v1/items").parametersByName().getValue("limit")
        assertEquals("integer", limit.obj("schema").str("type"))
        assertEquals("Max items returned.", limit.str("description"), "@OpenApiInfo on the local val")
    }
}
