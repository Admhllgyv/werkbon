package com.admtechnics.werkbon.ui.theme

import androidx.compose.ui.graphics.Color

// Profesyonel Renk Paletleri (Kullanıcı Ayarlardan Seçebilir)
val ThemeBlue = Color(0xFF1F4E78)    // Kurumsal Lacivert (Varsayılan)
val ThemeGreen = Color(0xFF047857)   // Zümrüt Yeşili
val ThemeRed = Color(0xFFB91C1C)     // Koyu Kırmızı
val ThemeOrange = Color(0xFFC2410C)  // Kiremit Turuncu
val ThemePurple = Color(0xFF6D28D9)  // Asil Mor
val ThemeDarkGray = Color(0xFF334155) // Antrasit (Koyu Gri)

// Sabit Renkler
val BackgroundGray = Color(0xFFF3F4F6) // Uygulama Arka Planı
val SurfaceWhite = Color(0xFFFFFFFF)
val TextDark = Color(0xFF1E293B)
val TextMuted = Color(0xFF64748B)
val BorderGray = Color(0xFFCBD5E1)
val TableHeaderColor = Color(0xFF1F4E78) // Tabloların koyu başlık rengi

fun getPrimaryColor(themeName: String): Color {
    return when (themeName) {
        "green" -> ThemeGreen
        "red" -> ThemeRed
        "orange" -> ThemeOrange
        "purple" -> ThemePurple
        "dark" -> ThemeDarkGray
        else -> ThemeBlue
    }
}
