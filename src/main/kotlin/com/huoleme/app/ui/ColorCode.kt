package com.huoleme.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

object ColorCode {
    fun parse(input: String): Color? {
        val clean = input.trim().removePrefix("#")
        if (clean.length != 6 && clean.length != 8) return null
        if (!clean.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
        val argb = if (clean.length == 6) "FF$clean" else clean
        return runCatching { Color(argb.toLong(16)) }.getOrNull()
    }

    fun format(color: Color): String = "#%06X".format(color.toArgb() and 0xFFFFFF)
}
