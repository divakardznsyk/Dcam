package com.dcam.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/** Dcam's palette: warm charcoal, cream paper, and one ember accent. */
object DC {
    val Bg = Color(0xFF121110)
    val Surface = Color(0xFF1C1A18)
    val Surface2 = Color(0xFF292623)
    val Line = Color(0xFF3A3632)
    val Cream = Color(0xFFF2EBDD)
    val Muted = Color(0xFF8F877C)
    val Ember = Color(0xFFFF6A2B)
    val Glass = Color(0x80000000)
}

val Serif = FontFamily.Serif
val Mono = FontFamily.Monospace

@Composable
fun DcamTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = DC.Ember,
            background = DC.Bg,
            surface = DC.Surface,
            onSurface = DC.Cream,
        ),
        content = content,
    )
}
