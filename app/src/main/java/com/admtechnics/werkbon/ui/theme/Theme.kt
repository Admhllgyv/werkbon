package com.admtechnics.werkbon.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    secondary = SecondaryBlue,
    onSecondary = Color.White,
    background = BackgroundGray,
    onBackground = TextDark,
    surface = SurfaceWhite,
    onSurface = TextDark
)

@Composable
fun WerkbonTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
