package com.hilight.studio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DurationInputTest {

    @Test
    fun `rejects blank malformed and non finite values`() {
        val invalid = listOf("", "  ", "seconds", "1.2.3", "NaN", "Infinity", "-Infinity")

        invalid.forEach { assertNull(it, parseDurationSeconds(it, 150, 8_000)) }
    }

    @Test
    fun `accepts dot comma whitespace and millisecond precision`() {
        assertEquals(3370, parseDurationSeconds("3.37", 150, 8_000))
        assertEquals(2500, parseDurationSeconds(" 2,5 ", 150, 8_000))
        assertEquals(301, parseDurationSeconds("0.3006", 150, 8_000))
    }

    @Test
    fun `clamps negative and huge input to the active range`() {
        assertEquals(150, parseDurationSeconds("-1", 150, 8_000))
        assertEquals(8_000, parseDurationSeconds("1e12", 150, 8_000))
        assertEquals(8_000, parseDurationSeconds("1e308", 150, 8_000))
    }

    @Test
    fun `rejects an invalid range`() {
        assertNull(parseDurationSeconds("1", 2_000, 1_000))
    }

    @Test
    fun `every accepted input remains inside the active range`() {
        listOf("-20", "0", "0.15", "1.234", "8", "999999").forEach { text ->
            val parsed = parseDurationSeconds(text, 150, 8_000)
            assertTrue("$text -> $parsed", parsed != null && parsed in 150..8_000)
        }
    }

    @Test
    fun `parseSliderNumber handles percentages properly`() {
        // Range 0.05..1.0
        val range = 0.05f..1.0f
        assertEquals(0.75f, parseSliderNumber("75", range, isPercentage = true)!!, 0.001f)
        assertEquals(0.75f, parseSliderNumber("75%", range, isPercentage = true)!!, 0.001f)
        assertEquals(0.75f, parseSliderNumber("0.75", range, isPercentage = true)!!, 0.001f)
        assertEquals(0.75f, parseSliderNumber("0,75", range, isPercentage = true)!!, 0.001f)
        assertEquals(0.05f, parseSliderNumber("1%", range, isPercentage = true)!!, 0.001f)
        assertEquals(0.05f, parseSliderNumber("5", range, isPercentage = true)!!, 0.001f)
        assertEquals(1.0f, parseSliderNumber("1", range, isPercentage = true)!!, 0.001f)
        assertEquals(1.0f, parseSliderNumber("100", range, isPercentage = true)!!, 0.001f)
        assertEquals(1.0f, parseSliderNumber("150%", range, isPercentage = true)!!, 0.001f)
    }

    @Test
    fun `parseSliderNumber handles arbitrary numeric ranges`() {
        val range = -19f..19f
        assertEquals(-5f, parseSliderNumber("-5", range, isPercentage = false)!!, 0.001f)
        assertEquals(10f, parseSliderNumber("10", range, isPercentage = false)!!, 0.001f)
        assertEquals(19f, parseSliderNumber("50", range, isPercentage = false)!!, 0.001f)
        assertEquals(-19f, parseSliderNumber("-100", range, isPercentage = false)!!, 0.001f)
        assertNull(parseSliderNumber("invalid", range, isPercentage = false))
    }
}
