package io.github.smyrgeorge.ktkit.api.rest

import io.github.smyrgeorge.ktkit.api.error.ErrorSpec
import io.github.smyrgeorge.ktkit.api.error.RuntimeError
import io.github.smyrgeorge.ktkit.api.error.impl.MalformedParameter
import io.github.smyrgeorge.ktkit.api.error.impl.MissingParameter
import io.github.smyrgeorge.ktkit.api.error.impl.UnsupportedEnumValue
import io.github.smyrgeorge.ktkit.api.rest.HttpContext.Var
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class HttpContextVarTest {

    private enum class Color { RED, GREEN }

    private fun of(value: String?, kind: Var.Kind = Var.Kind.QUERY_PARAM, name: String = "x"): Var =
        Var(kind, name, value)

    // --- asString ----------------------------------------------------------------------------

    @Test
    fun asStringReturnsTheValue() {
        assertEquals("hello", of("hello").asString())
        assertEquals("", of("").asString())
        assertEquals(" spaced ", of(" spaced ").asString())
    }

    @Test
    fun asStringMissingThrowsMissingParameter() {
        val error = assertFailsWith<RuntimeError> { of(null, name = "q").asString() }
        assertEquals(MissingParameter("QUERY_PARAM", "q"), error.error)
        assertEquals("Missing required parameter 'q' of type 'QUERY_PARAM'", error.message)
    }

    @Test
    fun missingParameterCarriesTheVarKind() {
        Var.Kind.entries.forEach { kind ->
            val error = assertFailsWith<RuntimeError> { of(null, kind = kind, name = "v").asString() }
            assertEquals(MissingParameter(kind.name, "v"), error.error)
        }
    }

    @Test
    fun malformedParameterCarriesTheVarKind() {
        Var.Kind.entries.forEach { kind ->
            val error = assertFailsWith<RuntimeError> { of("abc", kind = kind, name = "v").asInt() }
            assertEquals(MalformedParameter(kind.name, "v", "Int", "abc"), error.error)
        }
    }

    @Test
    fun asStringOrNull() {
        assertEquals("hello", of("hello").asStringOrNull())
        assertEquals("", of("").asStringOrNull())
        assertNull(of(null).asStringOrNull())
    }

    // --- asLong ------------------------------------------------------------------------------

    @Test
    fun asLongParsesValidValues() {
        assertEquals(5L, of("5").asLong())
        assertEquals(-42L, of("-42").asLong())
        assertEquals(0L, of("0").asLong())
        assertEquals(Long.MAX_VALUE, of("9223372036854775807").asLong())
        assertEquals(Long.MIN_VALUE, of("-9223372036854775808").asLong())
    }

    @Test
    fun asLongMalformedThrowsMalformedParameter() {
        // Malformed input is a 400 (MalformedParameter), never an uncaught NumberFormatException.
        listOf("abc", "1.5", "", "9223372036854775808" /* overflow */).forEach { raw ->
            val error = assertFailsWith<RuntimeError> { of(raw).asLong() }
            assertEquals(MalformedParameter("QUERY_PARAM", "x", "Long", raw), error.error)
            assertEquals(ErrorSpec.HttpStatus.BAD_REQUEST, error.error.httpStatus)
        }
        val error = assertFailsWith<RuntimeError> { of("abc").asLong() }
        assertEquals("Malformed parameter 'x' of type 'QUERY_PARAM': 'abc' is not a valid Long", error.message)
    }

    @Test
    fun asLongMissingThrowsMissingParameter() {
        val error = assertFailsWith<RuntimeError> { of(null).asLong() }
        assertEquals(MissingParameter("QUERY_PARAM", "x"), error.error)
    }

    @Test
    fun asLongOrNull() {
        assertEquals(5L, of("5").asLongOrNull())
        // Absent is null; a provided-but-malformed value is still a 400.
        assertNull(of(null).asLongOrNull())
        listOf("abc", "1.5", "9223372036854775808" /* overflow */).forEach { raw ->
            val error = assertFailsWith<RuntimeError> { of(raw).asLongOrNull() }
            assertEquals(MalformedParameter("QUERY_PARAM", "x", "Long", raw), error.error)
        }
    }

    // --- asInt -------------------------------------------------------------------------------

    @Test
    fun asIntParsesValidValues() {
        assertEquals(5, of("5").asInt())
        assertEquals(-42, of("-42").asInt())
        assertEquals(Int.MAX_VALUE, of("2147483647").asInt())
        assertEquals(Int.MIN_VALUE, of("-2147483648").asInt())
    }

    @Test
    fun asIntMalformedThrowsMalformedParameter() {
        listOf("abc", " 5", "2147483648" /* > Int.MAX_VALUE */).forEach { raw ->
            val error = assertFailsWith<RuntimeError> { of(raw).asInt() }
            assertEquals(MalformedParameter("QUERY_PARAM", "x", "Int", raw), error.error)
        }
    }

    @Test
    fun asIntMissingThrowsMissingParameter() {
        val error = assertFailsWith<RuntimeError> { of(null).asInt() }
        assertEquals(MissingParameter("QUERY_PARAM", "x"), error.error)
    }

    @Test
    fun asIntOrNull() {
        assertEquals(5, of("5").asIntOrNull())
        assertNull(of(null).asIntOrNull())
        listOf("abc", "2147483648" /* overflow */).forEach { raw ->
            val error = assertFailsWith<RuntimeError> { of(raw).asIntOrNull() }
            assertEquals(MalformedParameter("QUERY_PARAM", "x", "Int", raw), error.error)
        }
    }

    // --- asFloat / asDouble ------------------------------------------------------------------

    @Test
    fun asFloatParsesValidValues() {
        assertEquals(1.5f, of("1.5").asFloat())
        assertEquals(-0.25f, of("-0.25").asFloat())
        assertEquals(3f, of("3").asFloat())
    }

    @Test
    fun asFloatMalformedThrowsMalformedParameter() {
        listOf("abc", "1,5").forEach { raw ->
            val error = assertFailsWith<RuntimeError> { of(raw).asFloat() }
            assertEquals(MalformedParameter("QUERY_PARAM", "x", "Float", raw), error.error)
        }
    }

    @Test
    fun asFloatMissingThrowsMissingParameter() {
        val error = assertFailsWith<RuntimeError> { of(null).asFloat() }
        assertEquals(MissingParameter("QUERY_PARAM", "x"), error.error)
    }

    @Test
    fun asFloatOrNull() {
        assertEquals(1.5f, of("1.5").asFloatOrNull())
        assertNull(of(null).asFloatOrNull())
        val error = assertFailsWith<RuntimeError> { of("abc").asFloatOrNull() }
        assertEquals(MalformedParameter("QUERY_PARAM", "x", "Float", "abc"), error.error)
    }

    @Test
    fun asFloatAndAsDoubleParseIeeeSpecialValues() {
        // Unlike asInt/asLong, the floating-point conversions do not throw on overflow —
        // they saturate to Infinity — and the IEEE literals parse successfully.
        assertEquals(Float.POSITIVE_INFINITY, of("1e40").asFloat())
        assertEquals(Double.POSITIVE_INFINITY, of("1e400").asDouble())
        assertEquals(Double.NEGATIVE_INFINITY, of("-1e400").asDouble())
        assertEquals(Float.POSITIVE_INFINITY, of("Infinity").asFloat())
        assertTrue(of("NaN").asDouble().isNaN())
        // The OrNull variants saturate as well (they do not fail on overflow).
        assertEquals(Float.POSITIVE_INFINITY, of("1e40").asFloatOrNull())
        assertEquals(Double.POSITIVE_INFINITY, of("1e400").asDoubleOrNull())
    }

    @Test
    fun asDoubleParsesValidValues() {
        assertEquals(1.5, of("1.5").asDouble())
        assertEquals(-0.25, of("-0.25").asDouble())
        assertEquals(1000.0, of("1e3").asDouble())
    }

    @Test
    fun asDoubleMalformedThrowsMalformedParameter() {
        listOf("abc", "").forEach { raw ->
            val error = assertFailsWith<RuntimeError> { of(raw).asDouble() }
            assertEquals(MalformedParameter("QUERY_PARAM", "x", "Double", raw), error.error)
        }
    }

    @Test
    fun asDoubleMissingThrowsMissingParameter() {
        val error = assertFailsWith<RuntimeError> { of(null).asDouble() }
        assertEquals(MissingParameter("QUERY_PARAM", "x"), error.error)
    }

    @Test
    fun asDoubleOrNull() {
        assertEquals(1.5, of("1.5").asDoubleOrNull())
        assertNull(of(null).asDoubleOrNull())
        val error = assertFailsWith<RuntimeError> { of("abc").asDoubleOrNull() }
        assertEquals(MalformedParameter("QUERY_PARAM", "x", "Double", "abc"), error.error)
    }

    // --- asBoolean ---------------------------------------------------------------------------

    @Test
    fun asBooleanIsStrict() {
        assertEquals(true, of("true").asBoolean())
        assertEquals(false, of("false").asBoolean())
    }

    @Test
    fun asBooleanMalformedThrowsMalformedParameter() {
        // Only the exact literals "true"/"false" are accepted; everything else is a 400 rather
        // than being silently coerced to false.
        listOf("TRUE", "True", "yes", "1", "").forEach { raw ->
            val error = assertFailsWith<RuntimeError> { of(raw).asBoolean() }
            assertEquals(MalformedParameter("QUERY_PARAM", "x", "Boolean", raw), error.error)
        }
    }

    @Test
    fun asBooleanMissingThrowsMissingParameter() {
        val error = assertFailsWith<RuntimeError> { of(null).asBoolean() }
        assertEquals(MissingParameter("QUERY_PARAM", "x"), error.error)
    }

    @Test
    fun asBooleanOrNullIsStrict() {
        assertEquals(true, of("true").asBooleanOrNull())
        assertEquals(false, of("false").asBooleanOrNull())
        assertNull(of(null).asBooleanOrNull())
        // Strict parsing: only the exact literals "true" and "false" are accepted, and anything
        // else that is provided is a 400 rather than a null.
        listOf("TRUE", "yes", "1", "").forEach { raw ->
            val error = assertFailsWith<RuntimeError> { of(raw).asBooleanOrNull() }
            assertEquals(MalformedParameter("QUERY_PARAM", "x", "Boolean", raw), error.error)
        }
    }

    // --- asUuid ------------------------------------------------------------------------------

    @Test
    fun asUuidParsesValidValues() {
        val uuid = "3f06af63-a93c-11e4-9797-00505690773f"
        assertEquals(Uuid.parse(uuid), of(uuid).asUuid())
        // Uppercase hex digits are accepted.
        assertEquals(Uuid.parse(uuid), of(uuid.uppercase()).asUuid())
    }

    @Test
    fun asUuidMalformedThrowsMalformedParameter() {
        listOf("not-a-uuid", "3f06af63", "").forEach { raw ->
            val error = assertFailsWith<RuntimeError> { of(raw).asUuid() }
            assertEquals(MalformedParameter("QUERY_PARAM", "x", "Uuid", raw), error.error)
        }
    }

    @Test
    fun asUuidMissingThrowsMissingParameter() {
        val error = assertFailsWith<RuntimeError> { of(null).asUuid() }
        assertEquals(MissingParameter("QUERY_PARAM", "x"), error.error)
    }

    @Test
    fun asUuidOrNull() {
        val uuid = "3f06af63-a93c-11e4-9797-00505690773f"
        assertEquals(Uuid.parse(uuid), of(uuid).asUuidOrNull())
        assertNull(of(null).asUuidOrNull())
        listOf("not-a-uuid", "").forEach { raw ->
            val error = assertFailsWith<RuntimeError> { of(raw).asUuidOrNull() }
            assertEquals(MalformedParameter("QUERY_PARAM", "x", "Uuid", raw), error.error)
        }
    }

    // --- asEnum ------------------------------------------------------------------------------

    @Test
    fun asEnumParsesValidEntryNames() {
        assertEquals(Color.RED, of("RED").asEnum<Color>())
        assertEquals(Color.GREEN, of("GREEN").asEnum<Color>())
    }

    @Test
    fun asEnumIsCaseSensitive() {
        val error = assertFailsWith<RuntimeError> { of("red").asEnum<Color>() }
        assertEquals(UnsupportedEnumValue("Color", "red"), error.error)
    }

    @Test
    fun asEnumFailsOnUnknownValues() {
        val error = assertFailsWith<RuntimeError> { of("PURPLE").asEnum<Color>() }
        assertEquals(UnsupportedEnumValue("Color", "PURPLE"), error.error)
        assertEquals("Unsupported enum value 'PURPLE' for type 'Color'", error.message)
        // The underlying enumValueOf failure is preserved as the cause.
        assertNotNull(error.cause)
    }

    @Test
    fun asEnumMissingThrowsMissingParameter() {
        val error = assertFailsWith<RuntimeError> { of(null).asEnum<Color>() }
        assertEquals(MissingParameter("QUERY_PARAM", "x"), error.error)
    }

    @Test
    fun asEnumOrNull() {
        assertEquals(Color.RED, of("RED").asEnumOrNull<Color>())
        assertNull(of(null).asEnumOrNull<Color>())
        // A provided-but-unknown entry name is an UnsupportedEnumValue (400), not a null.
        listOf("PURPLE", "red").forEach { raw ->
            val error = assertFailsWith<RuntimeError> { of(raw).asEnumOrNull<Color>() }
            assertEquals(UnsupportedEnumValue("Color", raw), error.error)
        }
    }
}
