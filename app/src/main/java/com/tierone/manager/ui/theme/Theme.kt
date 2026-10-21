package com.tierone.manager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Bg = Color(0xFF070B14)
val Bg2 = Color(0xFF0D1526)
val Surface = Color(0xFF111B2E)
val Surface2 = Color(0xFF17243A)
val Surface3 = Color(0xFF1E2D47)
val Cyan = Color(0xFF4DE3FF)
val Magenta = Color(0xFFFF4D9D)
val Gold = Color(0xFFFFC857)
val TextMain = Color(0xFFEAF2FF)
val TextDim = Color(0xFF8FA3C0)
val Success = Color(0xFF51E08A)
val Danger = Color(0xFFFF5C6C)
val Outline = Color(0xFF25344F)

private val DarkColors = darkColorScheme(
    primary = Cyan,
    onPrimary = Color(0xFF00131A),
    primaryContainer = Color(0xFF0D3A47),
    onPrimaryContainer = Cyan,
    secondary = Magenta,
    onSecondary = Color(0xFF20000D),
    secondaryContainer = Color(0xFF42122A),
    onSecondaryContainer = Color(0xFFFFC2D9),
    tertiary = Gold,
    onTertiary = Color(0xFF1E1400),
    background = Bg,
    onBackground = TextMain,
    surface = Surface,
    onSurface = TextMain,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextDim,
    outline = Outline,
    error = Danger,
    onError = Color.White
)

private val AppTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Black, fontSize = 36.sp, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontWeight = FontWeight.Black, fontSize = 30.sp, letterSpacing = (-0.5).sp),
    displaySmall = TextStyle(fontWeight = FontWeight.Black, fontSize = 26.sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 21.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 13.5.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 10.5.sp, letterSpacing = 0.4.sp)
)

@Composable
fun TierOneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        content = content
    )
}
