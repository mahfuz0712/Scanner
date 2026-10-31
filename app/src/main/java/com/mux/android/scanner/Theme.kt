package com.mux.android.scanner

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Bg = Color(0xFF070D14)
val Panel = Color(0xFF0E1822)
val PanelHi = Color(0xFF16273A)
val Accent = Color(0xFF19E3FF)
val Amber = Color(0xFFFFB000)
val TextHi = Color(0xFFE6F7FF)
val TextDim = Color(0xFF7A93A6)
val Danger = Color(0xFFFF5470)

@Composable
fun ScannerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            onPrimary = Color(0xFF00212B),
            secondary = Amber,
            background = Bg,
            onBackground = TextHi,
            surface = Panel,
            onSurface = TextHi,
            surfaceVariant = PanelHi,
            onSurfaceVariant = TextDim,
            error = Danger
        ),
        content = content
    )
}