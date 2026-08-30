package net.bueffel.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
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
 * The palette.
 *
 * One near black ground, two greys for the surfaces on top of it, a single violet accent and
 * a green and a red used only to say "right" and "wrong". Nothing else gets a colour, which is
 * what keeps a screen full of answer boxes calm.
 */
object BueffelColors {
    val Background = Color(0xFF0B0B0F)
    val Surface = Color(0xFF15151C)
    val SurfaceRaised = Color(0xFF1E1E27)
    val Border = Color(0xFF2A2A36)

    val TextPrimary = Color(0xFFF2F2F5)
    val TextSecondary = Color(0xFF9A9AAB)
    val TextMuted = Color(0xFF63636F)

    val Accent = Color(0xFF7C5CFF)
    val AccentMuted = Color(0xFF2A2440)

    val Correct = Color(0xFF32D583)
    val CorrectMuted = Color(0xFF11291F)
    val Wrong = Color(0xFFF97066)
    val WrongMuted = Color(0xFF2B1616)
}

/** Corner radii and spacing, kept in one place so every surface agrees */
object BueffelShape {
    val Radius = 16.dp
    val RadiusSmall = 10.dp
    val Gutter = 20.dp
    val Gap = 12.dp
}

private val BueffelColorScheme =
    darkColorScheme(
        primary = BueffelColors.Accent,
        onPrimary = Color.White,
        background = BueffelColors.Background,
        onBackground = BueffelColors.TextPrimary,
        surface = BueffelColors.Surface,
        onSurface = BueffelColors.TextPrimary,
        surfaceVariant = BueffelColors.SurfaceRaised,
        onSurfaceVariant = BueffelColors.TextSecondary,
        outline = BueffelColors.Border,
        error = BueffelColors.Wrong,
    )

private val BueffelTypography =
    Typography(
        displaySmall = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
        titleLarge = TextStyle(fontSize = 21.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 25.sp, fontWeight = FontWeight.Normal),
        bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Normal),
        labelLarge = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
        labelSmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    )

/**
 * The app is dark whatever the system says: the design is built around one near black ground,
 * and a light variant would be a different design rather than the same one inverted.
 */
@Composable
fun BueffelTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = BueffelColorScheme,
        typography = BueffelTypography,
        content = content,
    )
}
