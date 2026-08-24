package io.github.smyrgeorge.ktkit.compiler.openapi.utils

import io.github.smyrgeorge.ktkit.api.error.impl.DatabaseError
import io.github.smyrgeorge.ktkit.api.error.impl.Forbidden
import io.github.smyrgeorge.ktkit.api.error.impl.MalformedRequestBody
import io.github.smyrgeorge.ktkit.api.error.impl.MissingParameter
import io.github.smyrgeorge.ktkit.api.error.impl.NotFound
import io.github.smyrgeorge.ktkit.api.error.impl.SystemError
import io.github.smyrgeorge.ktkit.api.error.impl.Unauthorized
import io.github.smyrgeorge.ktkit.api.error.impl.UnknownError
import io.github.smyrgeorge.ktkit.api.error.impl.UnsupportedEnumValue
import io.github.smyrgeorge.ktkit.api.rest.AbstractRestHandler
import io.github.smyrgeorge.ktkit.api.rest.HttpContext
import io.github.smyrgeorge.ktkit.api.rest.impl.AnonymousRestHandler
import io.github.smyrgeorge.ktkit.api.rest.openapi.OpenApi
import io.github.smyrgeorge.ktkit.api.rest.openapi.OpenApiIgnore
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KtkitNamesTest {

    @Test
    fun ktkitClassNamesMatchTheRuntime() {
        assertEquals(AbstractRestHandler::class.qualifiedName, KtkitNames.ABSTRACT_REST_HANDLER.asString())
        assertEquals(AnonymousRestHandler::class.qualifiedName, KtkitNames.ANONYMOUS_REST_HANDLER.asString())
        assertEquals(OpenApiIgnore::class.qualifiedName, KtkitNames.OPEN_API_IGNORE.asString())
        assertEquals(OpenApi::class.qualifiedName, KtkitNames.OPEN_API_ANNOTATION.asFqNameString())
        assertEquals(HttpContext::class.qualifiedName, KtkitNames.HTTP_CONTEXT.asString())
        assertEquals(HttpContext.Var::class.qualifiedName, KtkitNames.HTTP_CONTEXT_VAR.asString())
    }

    @Test
    fun kotlinxSerializationNamesMatch() {
        assertEquals(Serializable::class.qualifiedName, KtkitNames.SERIALIZABLE.asString())
        assertEquals(SerialName::class.qualifiedName, KtkitNames.SERIAL_NAME.asString())
        assertEquals(Transient::class.qualifiedName, KtkitNames.TRANSIENT.asString())
    }

    @Test
    fun memberNamesExistOnAbstractRestHandler() {
        val methods = AbstractRestHandler::class.java.methods.map { it.name }.toSet()
        (KtkitNames.VERBS.keys
                + KtkitNames.OPEN_API_SPEC
                + KtkitNames.URI
                + KtkitNames.ROUTES).forEach { name ->
            assertTrue(name in methods, "AbstractRestHandler is missing the member '$name'")
        }
    }

    @Test
    fun errorStatusTableMatchesTheRuntimeErrorTypes() {
        val expected = mapOf(
            NotFound::class to NotFound("").httpStatus.code,
            Unauthorized::class to Unauthorized("").httpStatus.code,
            Forbidden::class to Forbidden("").httpStatus.code,
            MalformedRequestBody::class to MalformedRequestBody(IllegalArgumentException()).httpStatus.code,
            MissingParameter::class to MissingParameter("", "").httpStatus.code,
            UnsupportedEnumValue::class to UnsupportedEnumValue("", "").httpStatus.code,
            DatabaseError::class to DatabaseError("", null, "").httpStatus.code,
            UnknownError::class to UnknownError("").httpStatus.code,
        )
        // A new SystemError implementation must be added above and to KtkitNames.ERROR_STATUS_BY_FQ.
        assertEquals(SystemError::class.sealedSubclasses.toSet(), expected.keys, "uncovered SystemError implementations")
        assertEquals(expected.size, KtkitNames.ERROR_STATUS_BY_FQ.size)
        expected.forEach { (cls, code) ->
            assertEquals(code, KtkitNames.ERROR_STATUS_BY_FQ[cls.qualifiedName], "status of ${cls.simpleName}")
        }
    }
}
