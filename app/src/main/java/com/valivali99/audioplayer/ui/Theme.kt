package com.valivali99.audioplayer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Black and grays only; white is kept for what's active or playing. */
object Palette {
    val Black = Color(0xFF0B0B0B)
    val Surface = Color(0xFF161616)
    val SurfaceHigh = Color(0xFF222222)
    val Track = Color(0xFF3A3A3A)
    val Muted = Color(0xFF5E5E5E)
    val TextDim = Color(0xFF8E8E8E)
    val Text = Color(0xFFE6E6E6)
    val White = Color(0xFFFFFFFF)
}

@Composable
fun AudioPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Palette.White,
            onPrimary = Palette.Black,
            secondary = Palette.TextDim,
            background = Palette.Black,
            onBackground = Palette.Text,
            surface = Palette.Surface,
            onSurface = Palette.Text,
            surfaceVariant = Palette.SurfaceHigh,
            onSurfaceVariant = Palette.TextDim,
            outline = Palette.Track,
        ),
        content = content,
    )
}
