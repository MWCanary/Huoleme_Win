package com.huoleme.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ColorCodeTest {
    @Test
    fun parsesSixDigitColors() {
        assertEquals(Color(0xFF123456), ColorCode.parse("#123456"))
        assertEquals(Color(0xFFABCDEF), ColorCode.parse("ABCDEF"))
    }

    @Test
    fun parsesEightDigitColorsWithAlpha() {
        assertEquals(Color(0x80123456), ColorCode.parse("#80123456"))
    }

    @Test
    fun rejectsInvalidColors() {
        assertNull(ColorCode.parse(""))
        assertNull(ColorCode.parse("#12345"))
        assertNull(ColorCode.parse("#GGGGGG"))
        assertNull(ColorCode.parse("#123456789"))
    }

    @Test
    fun formatsWithoutAlpha() {
        assertEquals("#123456", ColorCode.format(Color(0xFF123456)))
        assertEquals("#123456", ColorCode.format(Color(0x80123456)))
    }

    @Test
    fun rgbSliderComponentsRoundTrip() {
        val source = Color(0xFF00897B)
        val argb = source.toArgb()
        val r = ((argb shr 16) and 0xFF).toFloat()
        val g = ((argb shr 8) and 0xFF).toFloat()
        val b = (argb and 0xFF).toFloat()
        val rebuilt = Color(r.toInt(), g.toInt(), b.toInt())
        assertEquals(source, rebuilt)
        assertEquals("#00897B", ColorCode.format(rebuilt))
    }
}
