package com.pranav.dotto.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = DottoPrimary,
    secondary = DottoSecondary,
    tertiary = DottoTertiary,
    background = Color(0xFFF7F8FC),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1B1D22),
    onSurface = Color(0xFF1B1D22)
)

private val DarkColors = darkColorScheme(
    primary = DottoPrimary,
    secondary = DottoSecondary,
    tertiary = DottoTertiary,
    background = DottoBackground,
    surface = DottoSurface,
    onBackground = DottoOnBackground,
    onSurface = DottoOnSurface
)

private val DottoTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp)
)

@Composable
fun DottoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = DottoTypography,
        content = content
    )
}
