package com.admtechnics.werkbon.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Tipografi Ayarları (Premium Görünüm İçin Küçültüldü)
val AppTypography = Typography(
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 18.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 13.sp)
)

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
        onSurfaceVariant = TextMuted,
        outline = BorderGray
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
