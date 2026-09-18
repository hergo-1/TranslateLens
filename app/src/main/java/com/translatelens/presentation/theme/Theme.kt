package com.translatelens.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.translatelens.R

val IBMPlexSansArabic = FontFamily(
    Font(R.font.ibm_plex_sans_arabic_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_arabic_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_arabic_semi_bold, FontWeight.SemiBold),
    Font(R.font.ibm_plex_sans_arabic_bold, FontWeight.Bold)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF7C4DFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E1FA),
    onPrimaryContainer = Color(0xFF231744),
    secondary = Color(0xFF5D5870),
    onSecondary = Color.White,
    background = Color(0xFFF4F5F9),
    onBackground = Color(0xFF111827),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFF0F1F6),
    onSurfaceVariant = Color(0xFF6B7280),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF0F1F6),
    outline = Color(0xFFE2E4ED),
    outlineVariant = Color(0xFFD4D7E2),
    error = Color(0xFFB3261E)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB79CFF),
    onPrimary = Color(0xFF241545),
    primaryContainer = Color(0xFF3D2A68),
    onPrimaryContainer = Color(0xFFE9E1FA),
    secondary = Color(0xFFC9C2D8),
    onSecondary = Color(0xFF2C2739),
    background = Color(0xFF111016),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF111016),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1F1D27),
    onSurfaceVariant = Color(0xFF9E9BA8),
    surfaceContainer = Color(0xFF1A1821),
    surfaceContainerHigh = Color(0xFF1F1D27),
    outline = Color(0xFF2A2635),
    outlineVariant = Color(0xFF343042),
    error = Color(0xFFF2B8B5)
)

private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = IBMPlexSansArabic,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
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

@Composable
fun isAppDark(): Boolean {
    val bg = MaterialTheme.colorScheme.background
    val lum = 0.299f * bg.red + 0.587f * bg.green + 0.114f * bg.blue
    return lum < 0.5f
}

object AppGradients {
    val Title = Brush.linearGradient(
        listOf(Color(0xFF7C4DFF), Color(0xFF9C6BFF))
    )
    val Button = Brush.linearGradient(
        listOf(Color(0xFF7C4DFF), Color(0xFF9C6BFF))
    )
    val Logo = Brush.linearGradient(
        listOf(Color(0xFF7C4DFF), Color(0xFF9C6BFF))
    )
}

object AppSpec {
    val Active = Color(0xFF7C4DFF)
    val Download = Color(0xFF6C5CE7)
    val DeleteDarkBg = Color(0xFF221F2D)
    val DeleteDarkFg = Color(0xFFFF6B6B)
    val DeleteLightBg = Color(0xFFFCE8E8)
    val DeleteLightFg = Color(0xFFD32F2F)
    val InactiveDarkBg = Color(0xFF1F1D27)
    val InactiveDarkFg = Color(0xFFC4C2CE)
    val InactiveDarkBorder = Color(0xFF343042)
    val InactiveLightBg = Color(0xFFF0F1F6)
    val InactiveLightFg = Color(0xFF4A4D5E)
    val InactiveLightBorder = Color(0xFFD4D7E2)
}

fun themeModeToDark(themeMode: Int, systemDark: Boolean): Boolean {
    return when (themeMode) {
        1 -> false
        2 -> true
        else -> systemDark
    }
}
