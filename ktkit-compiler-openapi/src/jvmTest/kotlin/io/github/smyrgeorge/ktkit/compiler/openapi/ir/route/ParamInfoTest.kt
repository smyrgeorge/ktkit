package io.github.smyrgeorge.ktkit.compiler.openapi.ir.route

import io.github.smyrgeorge.ktkit.compiler.openapi.ir.schema.Schemas
import kotlin.test.Test
import kotlin.test.assertEquals

class ParamInfoTest {

    @Test
    fun keyFormatIsLocationColonName() {
        assertEquals("path:id", ParamInfo.key("path", "id"))
        assertEquals("query:fail", ParamInfo.key("query", "fail"))
        assertEquals("header:x-real-name", ParamInfo.key("header", "x-real-name"))
    }

    @Test
    fun instanceKeyMatchesTheCompanionFormat() {
        val info = ParamInfo(location = "query", name = "verbose", schema = Schemas.boolean(), required = false)
        assertEquals(ParamInfo.key("query", "verbose"), info.key)
        assertEquals("query:verbose", info.key)
    }
}
