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
import androidx.compose.ui.unit.dp
import net.bueffel.domain.StudySession
import net.bueffel.model.Card
import net.bueffel.model.Deck
import net.bueffel.ui.theme.BueffelColors
import net.bueffel.ui.theme.BueffelShape

/**
 * The study loop: a question, one box per answer, and nothing else on screen.
 *
 * Picking a box reveals the outcome at once and the screen then waits. Nothing advances on a
 * timer, so how long the answer stays up is the reader's decision.
 */
@Composable
fun StudyScreen(
    deck: Deck,
    onFinished: (List<Card>) -> Unit,
    onLeave: (List<Card>) -> Unit,
) {
    val session = remember(deck.id) { StudySession(deck) }
    var picked by remember { mutableStateOf<Int?>(null) }
    // bumped after each answer so the screen recomposes off the session's new state
    var round by remember { mutableIntStateOf(0) }

    val card = remember(round) { session.current() }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BueffelColors.Background)
                .systemBarsPadding()
                .padding(horizontal = BueffelShape.Gutter),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Caption(text = "${session.learnedCount} / ${session.total} gelernt", color = BueffelColors.TextSecondary)
            Text(
                text = "Schluss",
                style = MaterialTheme.typography.labelSmall,
                color = BueffelColors.TextMuted,
                modifier = Modifier.clickable { onLeave(session.snapshot()) },
            )
        }

        if (card == null) {
            FinishedPanel(total = session.total, onDone = { onFinished(session.snapshot()) })
            return@Column
        }

        val question = card.question

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = question.prompt,
                style = MaterialTheme.typography.displaySmall,
                color = BueffelColors.TextPrimary,
            )
            Spacer(Modifier.height(10.dp))
            BoxDots(box = card.box)

            Spacer(Modifier.height(26.dp))

            question.choices.forEachIndexed { index, choice ->
                ChoiceBox(
                    label = choice.label,
                    text = choice.text,
                    state = choiceState(index, picked, question.correctIndex),
                    onClick = { if (picked == null) picked = index },
                )
                Spacer(Modifier.height(BueffelShape.Gap))
            }
            Spacer(Modifier.height(12.dp))
        }

        val chosen = picked
        if (chosen != null) {
            ContinueBar(
                correct = chosen == question.correctIndex,
                correctLabel = question.correctChoice.label,
                onClick = {
                    session.answer(correct = chosen == question.correctIndex)
                    picked = null
                    round++
                },
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

/** How far this question has come: one dot per box it has passed */
@Composable
private fun BoxDots(box: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(Card.LEARNED_BOX) { index ->
            Box(
                modifier =
                    Modifier
                        .size(if (index < box) 7.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (index < box) BueffelColors.Correct else BueffelColors.Border),
            )
        }
    }
}

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
 * One answer, as a box carrying its own text: the thing being chosen and the thing being
 * tapped are the same object.
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
            ChoiceState.Correct -> BueffelColors.CorrectSurface
            ChoiceState.Wrong -> BueffelColors.WrongSurface
            else -> BueffelColors.Surface
        }
    val stroke =
        when (state) {
            ChoiceState.Correct -> BueffelColors.Correct
            ChoiceState.Wrong -> BueffelColors.Wrong
            else -> BueffelColors.Border
        }
    val textColor = if (state == ChoiceState.Dimmed) BueffelColors.TextMuted else BueffelColors.TextPrimary

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
        modifier = Modifier.size(30.dp).clip(CircleShape).background(background),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = foreground)
    }
}

/** Says how it went and moves on, on a tap rather than on a timer */
@Composable
private fun ContinueBar(
    correct: Boolean,
    correctLabel: String,
    onClick: () -> Unit,
) {
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
            text = if (correct) "Richtig" else "Falsch — richtig war $correctLabel",
            style = MaterialTheme.typography.titleMedium,
            color = if (correct) BueffelColors.Correct else BueffelColors.Wrong,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Caption(text = "Tippen für die nächste Frage")
    }
}

/** Shown once every question has reached the last box */
@Composable
private fun FinishedPanel(
    total: Int,
    onDone: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "durch",
            style = MaterialTheme.typography.displayLarge,
            color = BueffelColors.TextPrimary,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Alle $total Fragen sitzen — jede viermal hintereinander richtig.",
            style = MaterialTheme.typography.bodyLarge,
            color = BueffelColors.TextSecondary,
        )
        Spacer(Modifier.weight(1f))
        BueffelButton(text = "Fertig", onClick = onDone)
        Spacer(Modifier.height(20.dp))
    }
}
