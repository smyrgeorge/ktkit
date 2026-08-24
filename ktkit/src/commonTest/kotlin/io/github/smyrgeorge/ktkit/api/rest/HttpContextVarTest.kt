package io.github.smyrgeorge.ktkit.api.rest

import io.github.smyrgeorge.ktkit.api.error.RuntimeError
import io.github.smyrgeorge.ktkit.api.error.impl.MissingParameter
import io.github.smyrgeorge.ktkit.api.error.impl.UnsupportedEnumValue
import io.github.smyrgeorge.ktkit.api.rest.HttpContext.Var
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
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
    fun asLongFailsOnMalformedValues() {
        assertFailsWith<NumberFormatException> { of("abc").asLong() }
        assertFailsWith<NumberFormatException> { of("1.5").asLong() }
        assertFailsWith<NumberFormatException> { of("").asLong() }
        // Overflow.
        assertFailsWith<NumberFormatException> { of("9223372036854775808").asLong() }
    }

    @Test
    fun asLongMissingThrowsMissingParameter() {
        val error = assertFailsWith<RuntimeError> { of(null).asLong() }
        assertEquals(MissingParameter("QUERY_PARAM", "x"), error.error)
    }

    @Test
    fun asLongOrNull() {
        assertEquals(5L, of("5").asLongOrNull())
        assertNull(of("abc").asLongOrNull())
        assertNull(of("1.5").asLongOrNull())
        assertNull(of("9223372036854775808").asLongOrNull()) // overflow
        assertNull(of(null).asLongOrNull())
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
    fun asIntFailsOnMalformedValues() {
        assertFailsWith<NumberFormatException> { of("abc").asInt() }
        assertFailsWith<NumberFormatException> { of(" 5").asInt() }
        // Larger than Int.MAX_VALUE.
        assertFailsWith<NumberFormatException> { of("2147483648").asInt() }
    }

    @Test
    fun asIntMissingThrowsMissingParameter() {
        val error = assertFailsWith<RuntimeError> { of(null).asInt() }
        assertEquals(MissingParameter("QUERY_PARAM", "x"), error.error)
    }

    @Test
    fun asIntOrNull() {
        assertEquals(5, of("5").asIntOrNull())
        assertNull(of("abc").asIntOrNull())
        assertNull(of("2147483648").asIntOrNull()) // overflow
        assertNull(of(null).asIntOrNull())
    }

    // --- asFloat / asDouble ------------------------------------------------------------------

    @Test
    fun asFloatParsesValidValues() {
        assertEquals(1.5f, of("1.5").asFloat())
        assertEquals(-0.25f, of("-0.25").asFloat())
        assertEquals(3f, of("3").asFloat())
    }

    @Test
    fun asFloatFailsOnMalformedValues() {
        assertFailsWith<NumberFormatException> { of("abc").asFloat() }
        assertFailsWith<NumberFormatException> { of("1,5").asFloat() }
    }

    @Test
    fun asFloatMissingThrowsMissingParameter() {
        val error = assertFailsWith<RuntimeError> { of(null).asFloat() }
        assertEquals(MissingParameter("QUERY_PARAM", "x"), error.error)
    }

    @Test
    fun asFloatOrNull() {
        assertEquals(1.5f, of("1.5").asFloatOrNull())
        assertNull(of("abc").asFloatOrNull())
        assertNull(of(null).asFloatOrNull())
    }

    @Test
    fun asDoubleParsesValidValues() {
        assertEquals(1.5, of("1.5").asDouble())
        assertEquals(-0.25, of("-0.25").asDouble())
        assertEquals(1000.0, of("1e3").asDouble())
    }

    @Test
    fun asDoubleFailsOnMalformedValues() {
        assertFailsWith<NumberFormatException> { of("abc").asDouble() }
        assertFailsWith<NumberFormatException> { of("").asDouble() }
    }

    @Test
    fun asDoubleMissingThrowsMissingParameter() {
        val error = assertFailsWith<RuntimeError> { of(null).asDouble() }
        assertEquals(MissingParameter("QUERY_PARAM", "x"), error.error)
    }

    @Test
    fun asDoubleOrNull() {
        assertEquals(1.5, of("1.5").asDoubleOrNull())
        assertNull(of("abc").asDoubleOrNull())
        assertNull(of(null).asDoubleOrNull())
    }

    // --- asBoolean ---------------------------------------------------------------------------

    @Test
    fun asBooleanIsLenientAndCaseInsensitive() {
        assertEquals(true, of("true").asBoolean())
        assertEquals(true, of("TRUE").asBoolean())
        assertEquals(true, of("True").asBoolean())
        assertEquals(false, of("false").asBoolean())
        // Anything that is not "true" (ignoring case) is false.
        assertEquals(false, of("yes").asBoolean())
        assertEquals(false, of("1").asBoolean())
        assertEquals(false, of("").asBoolean())
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
        // Strict parsing: only the exact literals "true" and "false" are accepted.
        assertNull(of("TRUE").asBooleanOrNull())
        assertNull(of("yes").asBooleanOrNull())
        assertNull(of("1").asBooleanOrNull())
        assertNull(of(null).asBooleanOrNull())
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
    fun asUuidFailsOnMalformedValues() {
        assertFailsWith<IllegalArgumentException> { of("not-a-uuid").asUuid() }
        assertFailsWith<IllegalArgumentException> { of("3f06af63").asUuid() }
        assertFailsWith<IllegalArgumentException> { of("").asUuid() }
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
        assertNull(of("not-a-uuid").asUuidOrNull())
        assertNull(of("").asUuidOrNull())
        assertNull(of(null).asUuidOrNull())
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
    }

    @Test
    fun asEnumMissingThrowsMissingParameter() {
        val error = assertFailsWith<RuntimeError> { of(null).asEnum<Color>() }
        assertEquals(MissingParameter("QUERY_PARAM", "x"), error.error)
    }

    @Test
    fun asEnumOrNull() {
        assertEquals(Color.RED, of("RED").asEnumOrNull<Color>())
        assertNull(of("PURPLE").asEnumOrNull<Color>())
        assertNull(of("red").asEnumOrNull<Color>())
        assertNull(of(null).asEnumOrNull<Color>())
    }
}
