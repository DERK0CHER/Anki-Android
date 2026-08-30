package net.bueffel.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The palette: black, white, and three greys.
 *
 * Colour is spent only where it carries meaning - green for right, red for wrong. Everything
 * else is a shade, which is what lets a screen of answer boxes stay quiet.
 */
object BueffelColors {
    val Background = Color(0xFF000000)
    val Surface = Color(0xFF101010)
    val SurfaceRaised = Color(0xFF1A1A1A)
    val Border = Color(0xFF232323)

    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF8A8A8E)
    val TextMuted = Color(0xFF5A5A5E)

    val Correct = Color(0xFF32D583)
    val CorrectSurface = Color(0xFF0E1F16)
    val Wrong = Color(0xFFF97066)
    val WrongSurface = Color(0xFF1F1211)
}

/** Radii and spacing, in one place so every surface agrees */
object BueffelShape {
    /** Panels and answer boxes: generous, in the spirit of the reference screens */
    val Radius = 24.dp
    val Gutter = 24.dp
    val Gap = 12.dp
}

private val BueffelColorScheme =
    darkColorScheme(
        primary = BueffelColors.TextPrimary,
        onPrimary = BueffelColors.Background,
        background = BueffelColors.Background,
        onBackground = BueffelColors.TextPrimary,
        surface = BueffelColors.Surface,
        onSurface = BueffelColors.TextPrimary,
        surfaceVariant = BueffelColors.SurfaceRaised,
        onSurfaceVariant = BueffelColors.TextSecondary,
        outline = BueffelColors.Border,
        error = BueffelColors.Wrong,
    )

/**
 * Two sizes carry the screen: a large tight headline for the question, and a relaxed grey
 * body for everything being read rather than answered.
 */
private val BueffelTypography =
    Typography(
        displayLarge = TextStyle(fontSize = 56.sp, lineHeight = 58.sp, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp),
        displaySmall = TextStyle(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp),
        titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 26.sp, fontWeight = FontWeight.Normal),
        bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal),
        labelLarge = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
        labelSmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    )

/** The app is black whatever the system theme says: a light variant would be a different design. */
@Composable
fun BueffelTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BueffelColorScheme,
        typography = BueffelTypography,
        content = content,
    )
}
