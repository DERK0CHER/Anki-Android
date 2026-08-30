package net.bueffel.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.bueffel.ui.theme.BueffelColors
import net.bueffel.ui.theme.BueffelShape

/** The one button shape the app uses: a wide rounded bar */
@Composable
fun BueffelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean = true,
) {
    val background =
        when {
            !enabled -> BueffelColors.Surface
            filled -> BueffelColors.TextPrimary
            else -> BueffelColors.Surface
        }
    val foreground =
        when {
            !enabled -> BueffelColors.TextMuted
            filled -> BueffelColors.Background
            else -> BueffelColors.TextPrimary
        }
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BueffelShape.Pill))
                .background(background)
                .then(
                    if (filled) {
                        Modifier
                    } else {
                        Modifier.border(
                            BorderStroke(1.dp, BueffelColors.Border),
                            RoundedCornerShape(BueffelShape.Pill),
                        )
                    },
                ).clickable(enabled = enabled, onClick = onClick)
                .padding(vertical = 18.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = foreground,
        )
    }
}

/** A small caption above a block, in the grey the reference screens use for secondary text */
@Composable
fun Caption(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = BueffelColors.TextMuted,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier,
    )
}
