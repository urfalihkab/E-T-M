package com.seblaziz.egitim.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Accent = Color(0xFFE9483B)
val Bg = Color(0xFF13151F)
val Surface1 = Color(0xFF1C1F2C)
val Surface2 = Color(0xFF262A3A)
val TextDim = Color(0xFFA9AEC2)

private val scheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondary = Color(0xFFFFB547),
    background = Bg,
    onBackground = Color.White,
    surface = Bg,
    onSurface = Color.White,
    surfaceVariant = Surface1,
    onSurfaceVariant = TextDim,
    surfaceContainer = Surface1,
    surfaceContainerHigh = Surface2,
    outline = Color(0xFF3A3F55),
)

@Composable
fun SeblazizTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
