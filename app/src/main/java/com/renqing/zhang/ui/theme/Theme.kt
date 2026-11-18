package com.renqing.zhang.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Cinnabar = Color(0xFFCC392B)
val CinnabarDeep = Color(0xFFA32E22)
val Pine = Color(0xFF1F7A4D)
val Amber = Color(0xFF8A5A12)
val Slate = Color(0xFF3E5878)
val Honey = Color(0xFFF6F1EA)
val Ink = Color(0xFF241C18)
val InkSoft = Color(0xFF7A6C64)
val Hint = Color(0xFFC8BEB4)
val CardWhite = Color(0xFFFFFCF9)
val Line = Color(0xFFE7DFD6)
val AmberSoft = Color(0xFFF8EBD8)
val PineSoft = Color(0xFFE5F2EB)
val SlateSoft = Color(0xFFE6EBF1)
val CinnabarSoft = Color(0xFFF8E6E3)
val DisabledBg = Color(0xFFE8DDD4)
val DisabledText = Color(0xFFA89890)

val AmountFamily = FontFamily.Serif

private val scheme = lightColorScheme(
    primary = Cinnabar,
    onPrimary = Color.White,
    secondary = Pine,
    background = Honey,
    surface = CardWhite,
    onBackground = Ink,
    onSurface = Ink,
    onSurfaceVariant = InkSoft,
    outline = Line,
    outlineVariant = Line,
    error = Cinnabar,
)

private val type = Typography(
    headlineLarge = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun RenqingTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = type, content = content)
}
