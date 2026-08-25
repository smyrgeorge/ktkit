package io.github.smyrgeorge.ktkit.compiler.openapi.ir.route

import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HttpStatusCodesTest {

    /**
     * BY_NAME mirrors Ktor's `HttpStatusCode` companion properties — resolve() matches route
     * arguments like `HttpStatusCode.Created` by that property name, so every entry must exist
     * on the Ktor version actually on the classpath, with the same numeric code.
     */
    @Test
    fun byNameMatchesTheKtorCompanion() {
        val ktor = HttpStatusCode.Companion::class.java.methods
            .filter { it.name.startsWith("get") && it.returnType == HttpStatusCode::class.java && it.parameterCount == 0 }
            .associate { it.name.removePrefix("get") to (it.invoke(HttpStatusCode.Companion) as HttpStatusCode).value }
        HttpStatusCodes.BY_NAME.forEach { (name, code) ->
            assertEquals(ktor[name], code, "HttpStatusCode.$name")
        }
    }

    @Test
    fun phrasesSplitCamelCaseIntoWords() {
        assertEquals("OK", HttpStatusCodes.phraseOf(200))
        assertEquals("Created", HttpStatusCodes.phraseOf(201))
        assertEquals("Bad Request", HttpStatusCodes.phraseOf(400))
        assertEquals("Internal Server Error", HttpStatusCodes.phraseOf(500))
        assertEquals("Non Authoritative Information", HttpStatusCodes.phraseOf(203))
        assertNull(HttpStatusCodes.phraseOf(299))
    }

    @Test
    fun namesAreTheCamelCaseComponentKeys() {
        assertEquals("BadRequest", HttpStatusCodes.nameOf(400))
        assertEquals("Unauthorized", HttpStatusCodes.nameOf(401))
        assertEquals("Forbidden", HttpStatusCodes.nameOf(403))
        assertEquals("NotFound", HttpStatusCodes.nameOf(404))
        assertEquals("InternalServerError", HttpStatusCodes.nameOf(500))
        assertNull(HttpStatusCodes.nameOf(299))
    }

    @Test
    fun everyCodeRoundTripsThroughNameAndPhrase() {
        HttpStatusCodes.BY_NAME.values.toSet().forEach { code ->
            assertTrue(HttpStatusCodes.nameOf(code) in HttpStatusCodes.BY_NAME.keys, "nameOf($code)")
            assertEquals(code, HttpStatusCodes.BY_NAME[HttpStatusCodes.nameOf(code)], "round-trip of $code")
            assertTrue(!HttpStatusCodes.phraseOf(code).isNullOrBlank(), "phraseOf($code)")
        }
    }
}
