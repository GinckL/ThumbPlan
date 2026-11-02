package com.ginckl.thumbplan.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** App colour themes unlocked in the shop. [gradient] drives hero cards, the FAB and the alarm hero. */
data class Accent(val id: String, val primary: Color, val secondary: Color, val gradient: List<Color>)

object Accents {
    val Violet = Accent("violet", Color(0xFF6C5CE7), Color(0xFF00CEC9), listOf(Color(0xFF7C6CF0), Color(0xFF5A4BDB), Color(0xFF00B8B3)))
    val Ocean = Accent("ocean", Color(0xFF2563EB), Color(0xFF06B6D4), listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8), Color(0xFF0891B2)))
    val Sunset = Accent("sunset", Color(0xFFF97316), Color(0xFFEC4899), listOf(Color(0xFFFB923C), Color(0xFFF43F5E), Color(0xFFC026D3)))
    val Forest = Accent("forest", Color(0xFF16A34A), Color(0xFF84CC16), listOf(Color(0xFF22C55E), Color(0xFF15803D), Color(0xFF0D9488)))
    val Rose = Accent("rose", Color(0xFFDB2777), Color(0xFFF59E0B), listOf(Color(0xFFF472B6), Color(0xFFDB2777), Color(0xFF9333EA)))
    val Gold = Accent("gold", Color(0xFFD97706), Color(0xFF7C3AED), listOf(Color(0xFFFBBF24), Color(0xFFD97706), Color(0xFFB45309)))

    val all = listOf(Violet, Ocean, Sunset, Forest, Rose, Gold)
    fun get(id: String?): Accent = all.firstOrNull { it.id == id } ?: Violet
}

val LocalAccent = staticCompositionLocalOf { Accents.Violet }
