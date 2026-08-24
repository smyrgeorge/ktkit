package io.github.smyrgeorge.ktkit.api.rest

import io.github.smyrgeorge.ktkit.api.auth.impl.UserToken
import io.github.smyrgeorge.ktkit.api.error.RuntimeError
import io.github.smyrgeorge.ktkit.api.error.impl.MalformedRequestBody
import io.github.smyrgeorge.ktkit.api.error.impl.MissingParameter
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class HttpContextTest {

    @Serializable
    private data class TestBody(val name: String, val count: Int)

    private val user = UserToken(
        uuid = Uuid.parse("3f06af63-a93c-11e4-9797-00505690773f"),
        username = "tester",
    )

    /**
     * Serves [route], handles the GET request for [path] (with [headers]) by running [block]
     * against a fresh [HttpContext], and returns the text [block] produced.
     */
    private fun handle(
        route: String,
        path: String,
        headers: Map<String, List<String>> = emptyMap(),
        block: suspend (HttpContext) -> String,
    ): String {
        var result = ""
        testApplication {
            application {
                routing {
                    get(route) {
                        val ctx = HttpContext(user, call)
                        call.respondText(block(ctx))
                    }
                }
            }
            val response = client.get(path) {
                headers.forEach { (name, values) -> values.forEach { header(name, it) } }
            }
            assertEquals(HttpStatusCode.OK, response.status)
            result = response.bodyAsText()
        }
        return result
    }

    // --- uri ---------------------------------------------------------------------------------

    @Test
    fun uriReturnsTheFullRequestUri() {
        val uri = handle("/echo/{id}", "/echo/42?verbose=true") { it.uri() }
        assertEquals("/echo/42?verbose=true", uri)
    }

    // --- user --------------------------------------------------------------------------------

    @Test
    fun exposesTheAuthenticatedUser() {
        val username = handle("/me", "/me") { it.user.username }
        assertEquals("tester", username)
    }

    // --- pathVariable ------------------------------------------------------------------------

    @Test
    fun pathVariableReturnsTheValue() {
        val id = handle("/echo/{id}", "/echo/42") { it.pathVariable("id").asString() }
        assertEquals("42", id)
    }

    @Test
    fun pathVariableSupportsTypedConversions() {
        val uuid = "3f06af63-a93c-11e4-9797-00505690773f"
        val result = handle("/typed/{id}/{uid}", "/typed/42/$uuid") {
            val id = it.pathVariable("id").asInt()
            val uid = it.pathVariable("uid").asUuid()
            "$id|$uid"
        }
        assertEquals("42|$uuid", result)
    }

    @Test
    fun pathVariableMissingThrowsMissingParameter() {
        val result = handle("/opt/{id?}", "/opt") {
            val error = assertFailsWith<RuntimeError> { it.pathVariable("id").asString() }
            assertEquals(MissingParameter("PATH_VARIABLE", "id"), error.error)
            assertNull(it.pathVariable("id").asStringOrNull())
            "ok"
        }
        assertEquals("ok", result)
    }

    @Test
    fun pathVariableFallsBackToASameNamedQueryParameter() {
        // `call.parameters` merges query and path parameters, so pathVariable() resolves a
        // same-named query parameter when the path segment is absent.
        val result = handle("/opt/{id?}", "/opt?id=from-query") {
            it.pathVariable("id").asString()
        }
        assertEquals("from-query", result)
    }

    @Test
    fun pathVariableIsShadowedByASameNamedQueryParameter() {
        // In the merged `call.parameters`, query parameters take precedence over path ones.
        val result = handle("/shadow/{id}", "/shadow/from-path?id=from-query") {
            it.pathVariable("id").asString()
        }
        assertEquals("from-query", result)
    }

    // --- queryParam / queryParams ------------------------------------------------------------

    @Test
    fun queryParamReturnsTheValue() {
        val result = handle("/q", "/q?name=hello&empty=") {
            val name = it.queryParam("name").asString()
            val empty = it.queryParam("empty").asString()
            "$name|[$empty]"
        }
        assertEquals("hello|[]", result)
    }

    @Test
    fun queryParamSupportsTypedConversions() {
        val result = handle("/q", "/q?count=7&active=true&missing-ignored=x") {
            val count = it.queryParam("count").asInt()
            val active = it.queryParam("active").asBoolean()
            val absent = it.queryParam("absent").asIntOrNull()
            "$count|$active|$absent"
        }
        assertEquals("7|true|null", result)
    }

    @Test
    fun queryParamMissingThrowsMissingParameter() {
        val result = handle("/q", "/q") {
            val error = assertFailsWith<RuntimeError> { it.queryParam("name").asString() }
            assertEquals(MissingParameter("QUERY_PARAM", "name"), error.error)
            assertNull(it.queryParam("name").asStringOrNull())
            "ok"
        }
        assertEquals("ok", result)
    }

    @Test
    fun queryParamsReturnsAllValues() {
        val result = handle("/q", "/q?tag=a&tag=b&tag=c&other=x") {
            it.queryParams("tag").joinToString(",")
        }
        assertEquals("a,b,c", result)
    }

    @Test
    fun queryParamsReturnsEmptyListWhenAbsent() {
        val result = handle("/q", "/q") { it.queryParams("tag").size.toString() }
        assertEquals("0", result)
    }

    @Test
    fun queryParamReturnsTheFirstOfRepeatedValues() {
        val result = handle("/q", "/q?tag=a&tag=b") { it.queryParam("tag").asString() }
        assertEquals("a", result)
    }

    // --- header / headers --------------------------------------------------------------------

    @Test
    fun headerReturnsTheValue() {
        val result = handle("/h", "/h", headers = mapOf("X-Request-Id" to listOf("abc-123"))) {
            it.header("X-Request-Id").asString()
        }
        assertEquals("abc-123", result)
    }

    @Test
    fun headerNamesAreCaseInsensitive() {
        val result = handle("/h", "/h", headers = mapOf("X-Request-Id" to listOf("abc-123"))) {
            it.header("x-request-id").asString()
        }
        assertEquals("abc-123", result)
    }

    @Test
    fun repeatedHeadersArriveAsASingleFoldedValue() {
        // The client transport folds repeated headers into one comma-separated entry
        // (HTTP header folding), so header() and headers() both see the single folded value.
        val result = handle("/h", "/h", headers = mapOf("X-Multi" to listOf("a", "b"))) {
            val all = it.headers("X-Multi")
            "${all.size}:${all.joinToString("|")}:${it.header("X-Multi").asString()}"
        }
        assertEquals("1:a,b:a,b", result)
    }

    @Test
    fun headerMissingThrowsMissingParameter() {
        val result = handle("/h", "/h") {
            val error = assertFailsWith<RuntimeError> { it.header("X-Request-Id").asString() }
            assertEquals(MissingParameter("HEADER", "X-Request-Id"), error.error)
            assertNull(it.header("X-Request-Id").asStringOrNull())
            "ok"
        }
        assertEquals("ok", result)
    }


    @Test
    fun headersReturnsEmptyListWhenAbsent() {
        val result = handle("/h", "/h") { it.headers("X-Multi").size.toString() }
        assertEquals("0", result)
    }

    // --- body --------------------------------------------------------------------------------

    @Test
    fun bodyDeserializesJson() {
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                routing {
                    post("/body") {
                        val ctx = HttpContext(user, call)
                        val body = ctx.body<TestBody>()
                        call.respondText("${body.name}|${body.count}")
                    }
                }
            }
            val response = client.post("/body") {
                contentType(ContentType.Application.Json)
                setBody("""{"name":"hello","count":7}""")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("hello|7", response.bodyAsText())
        }
    }

    @Test
    fun bodyMalformedThrowsMalformedRequestBody() {
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                routing {
                    post("/body") {
                        val ctx = HttpContext(user, call)
                        val error = assertFailsWith<RuntimeError> { ctx.body<TestBody>() }
                        val spec = assertIs<MalformedRequestBody>(error.error)
                        // The original deserialization failure is preserved on both the
                        // error spec and the thrown RuntimeError, and drives the message.
                        assertNotNull(error.cause)
                        assertSame(error.cause, spec.cause)
                        assertTrue(error.message!!.startsWith("Could not parse request body:"))
                        call.respondText("caught")
                    }
                }
            }
            val response = client.post("/body") {
                contentType(ContentType.Application.Json)
                setBody("""{"name":"hello"""") // truncated JSON, count missing too
            }
            assertEquals("caught", response.bodyAsText())
        }
    }

    @Test
    fun bodyWithUnsupportedContentTypeThrowsMalformedRequestBody() {
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                routing {
                    post("/body") {
                        val ctx = HttpContext(user, call)
                        val error = assertFailsWith<RuntimeError> { ctx.body<TestBody>() }
                        assertIs<MalformedRequestBody>(error.error)
                        call.respondText("caught")
                    }
                }
            }
            val response = client.post("/body") {
                contentType(ContentType.Text.Plain)
                setBody("name=hello")
            }
            assertEquals("caught", response.bodyAsText())
        }
    }

    // --- request accessor ---------------------------------------------------------------------

    @Test
    fun requestExposesTheUnderlyingApplicationRequest() {
        val result = handle("/r", "/r?x=1") { it.request.queryParameters["x"] ?: "missing" }
        assertEquals("1", result)
    }

    @Test
    fun varConversionsWorkThroughARealCall() {
        val result = handle("/full/{id}", "/full/9?ratio=2.5&flag=true&level=GREEN") {
            val id = it.pathVariable("id").asLong()
            val ratio = it.queryParam("ratio").asDouble()
            val flag = it.queryParam("flag").asBoolean()
            // OrNull means "optional", not "lenient": absent is null, but a provided value is
            // still parsed strictly.
            val absent = it.queryParam("absent").asBooleanOrNull()
            "$id|$ratio|$flag|$absent"
        }
        assertEquals("9|2.5|true|null", result)
    }
}
