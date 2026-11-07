package com.admtechnics.werkbon.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun WerkbonTheme(
    primaryColor: Color = ThemeBlue,
    content: @Composable () -> Unit
) {
    val colorScheme = lightColorScheme(
        primary = primaryColor,
        onPrimary = Color.White,
        secondary = TextMuted,
        onSecondary = Color.White,
        background = BackgroundGray,
        onBackground = TextDark,
        surface = SurfaceWhite,
        onSurface = TextDark,
        surfaceVariant = BackgroundGray,
        onSurfaceVariant = TextDark,
        outline = BorderGray
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
