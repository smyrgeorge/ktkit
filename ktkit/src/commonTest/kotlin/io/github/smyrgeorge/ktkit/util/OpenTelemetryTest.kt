package io.github.smyrgeorge.ktkit.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OpenTelemetryTest {

    private val traceId = "4bf92f3577b34da6a3ce929d0e0e4736"
    private val spanId = "00f067aa0ba902b7"

    // --- valid headers -----------------------------------------------------------------------

    @Test
    fun parsesACanonicalHeader() {
        val parent = assertNotNull(extractOpenTelemetryHeader("00-$traceId-$spanId-01"))
        assertEquals("00", parent.version)
        assertEquals(traceId, parent.traceId)
        assertEquals(spanId, parent.spanId)
        assertTrue(parent.sampled)
    }

    @Test
    fun readsTheSampledFlagFromTheLeastSignificantBit() {
        fun sampled(flags: String): Boolean =
            assertNotNull(extractOpenTelemetryHeader("00-$traceId-$spanId-$flags")).sampled

        assertTrue(sampled("01"))
        assertTrue(sampled("03"))
        assertTrue(sampled("ff"))
        assertEquals(false, sampled("00"))
        assertEquals(false, sampled("02"))
        assertEquals(false, sampled("fe"))
    }

    @Test
    fun padsShorterFieldsAndLowerCasesHexDigits() {
        val parent = assertNotNull(extractOpenTelemetryHeader("0-ABC-DEF-1"))
        assertEquals("00", parent.version)
        assertEquals("00000000000000000000000000000abc", parent.traceId)
        assertEquals("0000000000000def", parent.spanId)
        assertTrue(parent.sampled)
    }

    // --- rejected headers --------------------------------------------------------------------

    @Test
    fun rejectsNonHexadecimalFields() {
        assertNull(extractOpenTelemetryHeader("garbage-XYZ!-@@-zz"))
        assertNull(extractOpenTelemetryHeader("0g-$traceId-$spanId-01"))
        assertNull(extractOpenTelemetryHeader("00-${traceId.dropLast(1)}z-$spanId-01"))
        assertNull(extractOpenTelemetryHeader("00-$traceId-${spanId.dropLast(1)}z-01"))
        assertNull(extractOpenTelemetryHeader("00-$traceId-$spanId-0z"))
    }

    @Test
    fun rejectsASignedFlagsField() {
        // `toIntOrNull(16)` accepts a leading sign, so `+1` must be turned away by the hex check.
        assertNull(extractOpenTelemetryHeader("00-$traceId-$spanId-+1"))
    }

    @Test
    fun rejectsEmptyFields() {
        assertNull(extractOpenTelemetryHeader("-$traceId-$spanId-01"))
        assertNull(extractOpenTelemetryHeader("00--$spanId-01"))
        assertNull(extractOpenTelemetryHeader("00-$traceId--01"))
        assertNull(extractOpenTelemetryHeader("00-$traceId-$spanId-"))
    }

    @Test
    fun rejectsFieldsLongerThanTheirCanonicalLength() {
        assertNull(extractOpenTelemetryHeader("000-$traceId-$spanId-01"))
        assertNull(extractOpenTelemetryHeader("00-${traceId}0-$spanId-01"))
        assertNull(extractOpenTelemetryHeader("00-$traceId-${spanId}0-01"))
        assertNull(extractOpenTelemetryHeader("00-$traceId-$spanId-001"))
    }

    @Test
    fun rejectsTheAllZeroIds() {
        assertNull(extractOpenTelemetryHeader("00-${"0".repeat(32)}-$spanId-01"))
        assertNull(extractOpenTelemetryHeader("00-$traceId-${"0".repeat(16)}-01"))
    }

    @Test
    fun rejectsHeadersThatAreNotFourFields() {
        assertNull(extractOpenTelemetryHeader(""))
        assertNull(extractOpenTelemetryHeader("00-$traceId-$spanId"))
        assertNull(extractOpenTelemetryHeader("00-$traceId-$spanId-01-extra"))
    }
}
