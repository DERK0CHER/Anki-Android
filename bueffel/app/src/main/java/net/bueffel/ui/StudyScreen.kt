package net.bueffel.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.bueffel.model.Question
import net.bueffel.model.SampleQuestions
import net.bueffel.ui.theme.BueffelColors
import net.bueffel.ui.theme.BueffelShape
import net.bueffel.ui.theme.BueffelTheme

/**
 * The study loop: a question, one box per answer, and nothing else on screen.
 *
 * Picking a box reveals the outcome immediately, and the screen then waits. Nothing advances on
 * a timer, so how long the answer stays up is the reader's decision.
 */
@Composable
fun StudyScreen(questions: List<Question> = SampleQuestions.all) {
    var index by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<Int?>(null) }
    var solved by remember { mutableIntStateOf(0) }

    val question = questions[index % questions.size]

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BueffelColors.Background)
                .systemBarsPadding()
                .padding(horizontal = BueffelShape.Gutter),
    ) {
        RunCounter(
            position = (index % questions.size) + 1,
            total = questions.size,
            solved = solved,
            modifier = Modifier.padding(top = 24.dp, bottom = 36.dp),
        )

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = question.prompt,
                style = MaterialTheme.typography.displaySmall,
                color = BueffelColors.TextPrimary,
            )

            Spacer(Modifier.height(28.dp))

            question.choices.forEachIndexed { choiceIndex, choice ->
                ChoiceBox(
                    label = choice.label,
                    text = choice.text,
                    state = choiceState(choiceIndex, picked, question.correctIndex),
                    onClick = {
                        if (picked == null) {
                            picked = choiceIndex
                            if (choiceIndex == question.correctIndex) solved++
                        }
                    },
                )
                Spacer(Modifier.height(BueffelShape.Gap))
            }

            Spacer(Modifier.height(12.dp))
        }

        if (picked != null) {
            ContinueBar(
                correct = picked == question.correctIndex,
                correctLabel = question.correctChoice.label,
                onClick = {
                    picked = null
                    index++
                },
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

/** How a single box should look right now */
private enum class ChoiceState { Untouched, Correct, Wrong, Dimmed }

private fun choiceState(
    index: Int,
    picked: Int?,
    correctIndex: Int,
): ChoiceState =
    when {
        picked == null -> ChoiceState.Untouched
        index == correctIndex -> ChoiceState.Correct
        index == picked -> ChoiceState.Wrong
        else -> ChoiceState.Dimmed
    }

/**
 * One answer, as a box carrying its own text.
 *
 * The whole box is the target rather than a letter in a footer, so the thing being chosen and
 * the thing being tapped are the same object.
 */
@Composable
private fun ChoiceBox(
    label: String,
    text: String,
    state: ChoiceState,
    onClick: () -> Unit,
) {
    val fill =
        when (state) {
            ChoiceState.Untouched -> BueffelColors.Surface
            ChoiceState.Correct -> BueffelColors.CorrectSurface
            ChoiceState.Wrong -> BueffelColors.WrongSurface
            ChoiceState.Dimmed -> BueffelColors.Surface
        }
    val stroke =
        when (state) {
            ChoiceState.Untouched -> BueffelColors.Border
            ChoiceState.Correct -> BueffelColors.Correct
            ChoiceState.Wrong -> BueffelColors.Wrong
            ChoiceState.Dimmed -> BueffelColors.Border
        }
    val textColor =
        when (state) {
            ChoiceState.Dimmed -> BueffelColors.TextMuted
            else -> BueffelColors.TextPrimary
        }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BueffelShape.Radius))
                .background(fill)
                .border(BorderStroke(1.dp, stroke), RoundedCornerShape(BueffelShape.Radius))
                .clickable(enabled = state == ChoiceState.Untouched, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 18.dp),
    ) {
        Badge(label = label, state = state)
        Spacer(Modifier.width(14.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            modifier = Modifier.weight(1f),
        )
    }
}

/** The A/B/C/D disc at the head of a box */
@Composable
private fun Badge(
    label: String,
    state: ChoiceState,
) {
    val background =
        when (state) {
            ChoiceState.Correct -> BueffelColors.Correct
            ChoiceState.Wrong -> BueffelColors.Wrong
            else -> BueffelColors.SurfaceRaised
        }
    val foreground =
        when (state) {
            ChoiceState.Correct, ChoiceState.Wrong -> BueffelColors.Background
            ChoiceState.Dimmed -> BueffelColors.TextMuted
            else -> BueffelColors.TextSecondary
        }
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(background),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = foreground,
        )
    }
}

/** Says how it went and moves on, on a tap rather than on a timer */
@Composable
private fun ContinueBar(
    correct: Boolean,
    correctLabel: String,
    onClick: () -> Unit,
) {
    val tint = if (correct) BueffelColors.Correct else BueffelColors.Wrong
    val message = if (correct) "Richtig" else "Falsch — richtig war $correctLabel"

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BueffelShape.Radius))
                .background(BueffelColors.SurfaceRaised)
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = tint,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Tippen für die nächste Frage",
            style = MaterialTheme.typography.labelSmall,
            color = BueffelColors.TextMuted,
        )
    }
}

/** "1 / 5" in the corner, the way the reference screens count a run */
@Composable
private fun RunCounter(
    position: Int,
    total: Int,
    solved: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "$position / $total",
            style = MaterialTheme.typography.labelSmall,
            color = BueffelColors.TextSecondary,
        )
        Text(
            text = "$solved richtig",
            style = MaterialTheme.typography.labelSmall,
            color = BueffelColors.TextMuted,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0F)
@Composable
private fun StudyScreenPreview() {
    BueffelTheme {
        StudyScreen()
    }
}
