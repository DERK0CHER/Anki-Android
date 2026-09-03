package net.bueffel.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.bueffel.domain.Schedule
import net.bueffel.model.Card
import net.bueffel.ui.theme.BueffelColors
import net.bueffel.ui.theme.BueffelMotion
import net.bueffel.ui.theme.BueffelShape
import kotlin.math.roundToInt

/**
 * The one button shape the app uses: a wide rounded bar.
 *
 * It gives under the finger - a small, quick scale-down while pressed - which is the only
 * touch feedback a black interface needs.
 */
@Composable
fun BueffelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "buttonPress",
    )
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
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }.clip(RoundedCornerShape(BueffelShape.Pill))
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
                ).clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick,
                ).padding(vertical = 18.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = foreground,
        )
    }
}

/**
 * "1 Frage · 1 sicher", and it says Fragen when there is more than one.
 *
 * Trivial, and it was wrong in two places: a count line that reads "1 Fragen" makes an app look
 * like nobody ever ran it.
 */
fun countLine(
    questions: Int,
    learned: Int,
): String = "${questionCount(questions)} · $learned sicher"

/** "1 Frage", "12 Fragen". Trivial, and it has now been got wrong in three places. */
fun questionCount(questions: Int): String = "$questions ${if (questions == 1) "Frage" else "Fragen"}"

/**
 * What is worth asking today, or when it will be.
 *
 * The second half matters as much as the first: a set with nothing due is not a set that is
 * finished, and saying when it comes back is what stops it being opened and drilled anyway.
 */
fun dueLine(
    cards: List<Card>,
    today: Long,
): String {
    val due = cards.count { it.isDue(today) }
    if (due > 0) return "$due fällig"
    val days = Schedule.nextDue(cards, today) ?: return "nichts fällig"
    return if (days <= 1) "morgen wieder" else "in $days Tagen wieder"
}

/** "34 min geübt", or nothing at all while there is nothing worth saying */
fun timeLine(seconds: Long): String? {
    val minutes = seconds / 60
    return when {
        minutes < 1 -> null
        minutes < 90 -> "$minutes min geübt"
        else -> "${minutes / 60} h ${minutes % 60} min geübt"
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

/** A small, quiet target: a word on a round surface, for things that are not the main act */
@Composable
fun QuietAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = BueffelColors.TextSecondary,
        modifier =
            modifier
                .clip(RoundedCornerShape(BueffelShape.Pill))
                .background(BueffelColors.Surface)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

/** A numbered line: a thin ring with the digit, then the step itself */
@Composable
fun StepLabel(
    number: String,
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = BueffelColors.TextPrimary,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(30.dp)
                    .border(BorderStroke(1.dp, BueffelColors.Border), CircleShape),
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelSmall,
                color = BueffelColors.TextSecondary,
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = textColor,
        )
    }
}

/**
 * How well a set is known, as one bar.
 *
 * The fill is a real gradient running red, amber, light green across the whole track, and only
 * the earned part of it is drawn. So the colour at the tip is the reading - a bar that is a
 * third full is still red at its end, and one that is nearly full has gone green.
 *
 * It carries no name. A bar that runs from red to green with a percentage beside it does not
 * need a word to explain that it is progress.
 *
 * The drawn part eases towards its new length rather than jumping, but the gradient itself
 * never moves: growth reveals more of the same run.
 */
@Composable
fun ProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
    showPercent: Boolean = true,
) {
    val safe = fraction.coerceIn(0f, 1f)
    val drawn by animateFloatAsState(
        targetValue = safe,
        animationSpec = tween(BueffelMotion.Settle),
        label = "meterFill",
    )
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BoxWithConstraints(
            modifier =
                Modifier
                    .weight(1f)
                    .height(height)
                    .clip(RoundedCornerShape(BueffelShape.Pill))
                    .background(BueffelColors.SurfaceRaised),
        ) {
            val track = maxWidth
            // never thinner than it is tall once there is anything at all: a one-box start
            // should read as a dot of red, not as an empty bar
            val filled = if (drawn <= 0f) 0.dp else maxOf(track * drawn, height)
            val trackPx = with(LocalDensity.current) { track.toPx() }
            Box(
                modifier =
                    Modifier
                        .width(filled)
                        .height(height)
                        .clip(RoundedCornerShape(BueffelShape.Pill))
                        // The gradient is told the width of the whole track rather than being
                        // left to fit whatever is drawn, so the shade at the tip means the same
                        // thing at every length. Sizing an oversized child to do this does not
                        // work: content that overflows its constraints gets centred, which
                        // slides the red off the left end.
                        .background(
                            Brush.horizontalGradient(
                                colors =
                                    listOf(
                                        BueffelColors.Wrong,
                                        BueffelColors.Almost,
                                        BueffelColors.LearnedGreen,
                                    ),
                                startX = 0f,
                                endX = trackPx,
                            ),
                        ),
            )
        }
        if (showPercent) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = "${(safe * 100).roundToInt()} %",
                style = MaterialTheme.typography.labelSmall,
                color = BueffelColors.progressColor(safe),
            )
        }
    }
}

/** The pill switch from the reference screens: a track, a sliding knob, and a word beside it */
@Composable
fun PillToggle(
    checked: Boolean,
    label: String,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val knobTravel by animateDpAsState(
        targetValue = if (checked) 22.dp else 0.dp,
        animationSpec = tween(BueffelMotion.Quick),
        label = "knob",
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) BueffelColors.TextPrimary else BueffelColors.SurfaceRaised,
        animationSpec = tween(BueffelMotion.Quick),
        label = "track",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .clip(RoundedCornerShape(BueffelShape.Pill))
                .clickable { onCheckedChange(!checked) }
                .padding(vertical = 6.dp),
    ) {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier =
                Modifier
                    .width(52.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(BueffelShape.Pill))
                    .background(trackColor)
                    .border(
                        BorderStroke(1.dp, if (checked) BueffelColors.TextPrimary else BueffelColors.Border),
                        RoundedCornerShape(BueffelShape.Pill),
                    ).padding(horizontal = 4.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .offset(x = knobTravel)
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
