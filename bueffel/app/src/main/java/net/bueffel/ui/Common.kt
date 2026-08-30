package net.bueffel.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.bueffel.ui.theme.BueffelColors
import net.bueffel.ui.theme.BueffelShape
import kotlin.math.roundToInt

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

/**
 * The Lern-O-Meter: how well a set is known, as one bar.
 *
 * The fill is a real gradient running red, amber, light green across the whole track, and only
 * the earned part of it is drawn. So the colour at the tip is the reading - a bar that is a
 * third full is still red at its end, and one that is nearly full has gone green.
 */
@Composable
fun LernOMeter(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
    label: String? = null,
) {
    val safe = fraction.coerceIn(0f, 1f)
    Column(modifier = modifier) {
        if (label != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Caption(text = label)
                Caption(
                    text = "${(safe * 100).roundToInt()} %",
                    color = BueffelColors.progressColor(safe),
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(height)
                    .clip(RoundedCornerShape(BueffelShape.Pill))
                    .background(BueffelColors.SurfaceRaised),
        ) {
            val track = maxWidth
            // never thinner than it is tall once there is anything at all: a one-box start
            // should read as a dot of red, not as an empty bar
            val filled = if (safe <= 0f) 0.dp else maxOf(track * safe, height)
            Box(
                modifier =
                    Modifier
                        .width(filled)
                        .height(height)
                        .clip(RoundedCornerShape(BueffelShape.Pill)),
            ) {
                // as wide as the whole track, so the gradient does not squeeze into the filled
                // part: the shade at the tip then means the same thing at every length
                Box(
                    modifier =
                        Modifier
                            .width(track)
                            .height(height)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        BueffelColors.Wrong,
                                        BueffelColors.Almost,
                                        BueffelColors.LearnedGreen,
                                    ),
                                ),
                            ),
                )
            }
        }
    }
}

/** The pill switch from the reference screens: a track, a knob, and a word beside it */
@Composable
fun PillToggle(
    checked: Boolean,
    label: String,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .clip(RoundedCornerShape(BueffelShape.Pill))
                .clickable { onCheckedChange(!checked) }
                .padding(vertical = 6.dp),
    ) {
        Box(
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
            modifier =
                Modifier
                    .width(52.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(BueffelShape.Pill))
                    .background(if (checked) BueffelColors.TextPrimary else BueffelColors.SurfaceRaised)
                    .border(
                        BorderStroke(1.dp, if (checked) BueffelColors.TextPrimary else BueffelColors.Border),
                        RoundedCornerShape(BueffelShape.Pill),
                    ).padding(horizontal = 4.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (checked) BueffelColors.Background else BueffelColors.TextMuted),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (checked) BueffelColors.TextPrimary else BueffelColors.TextMuted,
        )
    }
}
