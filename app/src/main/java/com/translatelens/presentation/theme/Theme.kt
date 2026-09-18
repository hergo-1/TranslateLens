package com.translatelens.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.translatelens.R

val AccentLight = Color(0xFF6B4FA8)
val AccentDark = Color(0xFFB79CFF)

val IBMPlexSansArabic = FontFamily(
    Font(R.font.ibm_plex_sans_arabic_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_arabic_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_arabic_semi_bold, FontWeight.SemiBold),
    Font(R.font.ibm_plex_sans_arabic_bold, FontWeight.Bold)
)

private val LightColors = lightColorScheme(
    primary = AccentLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E1FA),
    onPrimaryContainer = Color(0xFF231744),
    secondary = Color(0xFF5D5870),
    onSecondary = Color.White,
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF191A1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191A1F),
    surfaceVariant = Color(0xFFF2F0F6),
    onSurfaceVariant = Color(0xFF4A4458),
    surfaceContainer = Color(0xFFF6F4FA),
    outline = Color(0xFFE2DEE9),
    outlineVariant = Color(0xFFEDEAF2),
    error = Color(0xFFB3261E)
)

private val DarkColors = darkColorScheme(
    primary = AccentDark,
    onPrimary = Color(0xFF241545),
    primaryContainer = Color(0xFF3D2A68),
    onPrimaryContainer = Color(0xFFE9E1FA),
    secondary = Color(0xFFC9C2D8),
    onSecondary = Color(0xFF2C2739),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF4F2F8),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFF4F2F8),
    surfaceVariant = Color(0xFF17161C),
    onSurfaceVariant = Color(0xFFC9C2D8),
    surfaceContainer = Color(0xFF131218),
    surfaceContainerHigh = Color(0xFF1D1C22),
    outline = Color(0xFF2C2A33),
    outlineVariant = Color(0xFF232228),
    error = Color(0xFFF2B8B5)
)

private val AppTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = IBMPlexSansArabic,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = IBMPlexSansArabic,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 27.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = IBMPlexSansArabic,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = IBMPlexSansArabic,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = IBMPlexSansArabic,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily = IBMPlexSansArabic,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = IBMPlexSansArabic,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = IBMPlexSansArabic,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.sp
    )
)

@Composable
fun TranslateLensTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}

fun themeModeToDark(themeMode: Int, systemDark: Boolean): Boolean {
    return when (themeMode) {
        1 -> false
        2 -> true
        else -> systemDark
    }
}
