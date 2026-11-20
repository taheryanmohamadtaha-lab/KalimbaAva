package com.kalimba.ava.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.kalimba.ava.R

object Wood {
    val Primary = Color(0xFF795238)
    val Secondary = Color(0xFFB58A55)
    val Background = Color(0xFFF5EFE4)
    val Surface = Color(0xFFE9DDCA)
    val Accent = Color(0xFFD5AA63)
    val DarkBackground = Color(0xFF211A15)
    val Text = Color(0xFF35271D)
    val Mahogany = Color(0xFF8B4A2B)
    val Good = Color(0xFF4E8F4A)
    val Bad = Color(0xFFB5453A)
}

val VazirFont = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
    Font(R.font.vazirmatn_bold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Medium),
)

private val LightColors = lightColorScheme(
    primary = Wood.Primary, onPrimary = Color.White, secondary = Wood.Secondary, onSecondary = Color.White,
    tertiary = Wood.Accent, onTertiary = Wood.Text,
    background = Wood.Background, onBackground = Wood.Text, surface = Wood.Background, onSurface = Wood.Text,
    surfaceVariant = Wood.Surface, onSurfaceVariant = Color(0xFF5A4636),
    primaryContainer = Color(0xFFEBCFAE), onPrimaryContainer = Color(0xFF3B2412),
    secondaryContainer = Color(0xFFE9D3AF), onSecondaryContainer = Color(0xFF3B2412),
    outline = Color(0xFF8D7560), error = Wood.Bad,
)

private val DarkColors = darkColorScheme(
    primary = Wood.Accent, onPrimary = Wood.DarkBackground, secondary = Wood.Secondary, onSecondary = Wood.DarkBackground,
    tertiary = Wood.Accent, onTertiary = Wood.DarkBackground,
    background = Wood.DarkBackground, onBackground = Color(0xFFF0E4D2), surface = Wood.DarkBackground, onSurface = Color(0xFFF0E4D2),
    surfaceVariant = Color(0xFF30251D), onSurfaceVariant = Color(0xFFD9C7AF),
    primaryContainer = Color(0xFF5A3A24), onPrimaryContainer = Color(0xFFFFE2BF),
    secondaryContainer = Color(0xFF4A3524), onSecondaryContainer = Color(0xFFF0DDC0),
    outline = Color(0xFF9C8570), error = Color(0xFFE08A7E),
)

private fun TextStyle.v() = copy(fontFamily = VazirFont)

private fun vazirTypography(): Typography {
    val b = Typography()
    return Typography(
        displayLarge = b.displayLarge.v(), displayMedium = b.displayMedium.v(), displaySmall = b.displaySmall.v(),
        headlineLarge = b.headlineLarge.v(), headlineMedium = b.headlineMedium.v(), headlineSmall = b.headlineSmall.v(),
        titleLarge = b.titleLarge.v(), titleMedium = b.titleMedium.v(), titleSmall = b.titleSmall.v(),
        bodyLarge = b.bodyLarge.v(), bodyMedium = b.bodyMedium.v(), bodySmall = b.bodySmall.v(),
        labelLarge = b.labelLarge.v(), labelMedium = b.labelMedium.v(), labelSmall = b.labelSmall.v(),
    )
}

@Composable
fun KalimbaTheme(themeMode: Int, content: @Composable () -> Unit) {
    val dark = when (themeMode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = vazirTypography(), content = content)
}
