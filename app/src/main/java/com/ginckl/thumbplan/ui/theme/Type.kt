package com.ginckl.thumbplan.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.ginckl.thumbplan.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val fontName = GoogleFont("Inter")

// Falls back to the system sans-serif automatically when Google Play services can't deliver Inter (offline / AOSP).
val interFontFamily = FontFamily(
    Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.Bold)
)

private fun style(weight: FontWeight, size: Int, line: Int, spacing: Double = 0.0) = TextStyle(
    fontFamily = interFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = spacing.sp
)

// Colours are intentionally NOT set here: text takes its colour from the theme (LocalContentColor) so
// every style stays readable in both light and dark mode.
val Typography = Typography(
    displayLarge = style(FontWeight.Bold, 32, 40),
    displayMedium = style(FontWeight.Bold, 28, 36),
    headlineMedium = style(FontWeight.Bold, 24, 32),
    titleLarge = style(FontWeight.SemiBold, 22, 28),
    titleMedium = style(FontWeight.SemiBold, 17, 24),
    bodyLarge = style(FontWeight.Normal, 15, 22),
    bodyMedium = style(FontWeight.Normal, 14, 20),
    bodySmall = style(FontWeight.Normal, 12, 16),
    labelLarge = style(FontWeight.SemiBold, 15, 20, 0.1),
    labelMedium = style(FontWeight.Medium, 12, 16),
    labelSmall = style(FontWeight.Medium, 11, 14, 0.2)
)
