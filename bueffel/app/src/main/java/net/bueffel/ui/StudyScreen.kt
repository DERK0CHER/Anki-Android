package net.bueffel.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.bueffel.audio.Feedback
import net.bueffel.domain.StudySession
import net.bueffel.model.Card
import net.bueffel.ui.theme.BueffelColors
import net.bueffel.ui.theme.BueffelMotion
import net.bueffel.ui.theme.BueffelShape

/**
 * The study loop: a question, one box per answer, and nothing else on screen.
 *
 * The question hangs high with air around it and the answers gather at the bottom, under the
 * thumb that has to hit them; the space between the two is the screen breathing, not leftovers.
 * Rounds slide in from the right and out to the left, so answering visibly moves the set along.
 *
 * Picking a box reveals the outcome at once and the screen then waits. Nothing advances on a
 * timer, so how long the answer stays up is the reader's decision.
 *
 * @param key identifies the set being studied; a new one starts a new session
 */
@Composable
fun StudyScreen(
    key: String,
    cards: List<Card>,
    soundOn: Boolean,
    onFinished: (List<Card>) -> Unit,
    onLeave: (List<Card>) -> Unit,
) {
    val session = remember(key) { StudySession(cards) }
    var picked by remember { mutableStateOf<Int?>(null) }
    // bumped after each answer so the screen recomposes off the session's new state
    var round by remember { mutableIntStateOf(0) }

    // A fresh order every time the question comes round: with a fixed order the answer that
    // gets remembered is "the second one from the top" rather than the answer itself.
    val view =
        remember(round) {
            session.current()?.let { card ->
                val order =
                    card.question.answers.indices
                        .shuffled()
                RoundView(
                    round = round,
                    prompt = card.question.prompt,
                    answers = order.map { card.question.answers[it] },
                    correctPosition = order.indexOf(card.question.correctIndex),
                    remaining = session.remaining,
                    hard = card.hard,
                )
            }
        }
    val chosen = picked

    // Flagging must not bump the round: that would redraw the answers in a new order under a
    // finger mid-question, and any already picked position would then point at the wrong one.
    // The session is told straight away; this only keeps the button in step until the next round.
    var flagged by remember(round) { mutableStateOf<Boolean?>(null) }

    val feedback = remember { Feedback() }
    DisposableEffect(Unit) { onDispose { feedback.release() } }

    // the session lives here, so leaving has to be handled here too: anywhere else would write
    // back the cards as they were before any of this was answered
    BackHandler { onLeave(session.snapshot()) }

    fun advance() {
        val position = picked ?: return
        val current = view ?: return
        session.answer(correct = position == current.correctPosition)
        picked = null
        round++
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BueffelColors.Background)
                // once an answer is showing the whole screen moves on: the finger is already in
                // the middle of the screen, so making it travel to a bar at the bottom is a
                // second act of aiming for no reason
                .clickable(
                    enabled = chosen != null,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = { advance() },
                ).systemBarsPadding()
                .padding(horizontal = BueffelShape.Gutter),
    ) {
        TopBar(
            progress = session.progress,
            hard = flagged ?: (view?.hard == true),
            canUndo = session.canUndo && chosen == null,
            onHard = {
                session.flag(it)
                flagged = it
            },
            onUndo = {
                if (session.undo()) {
                    picked = null
                    round++
                }
            },
            onLeave = { onLeave(session.snapshot()) },
        )

        AnimatedContent(
            targetState = view,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                (
                    slideInHorizontally(tween(BueffelMotion.Settle)) { it / 3 } +
                        fadeIn(tween(BueffelMotion.Settle))
                ) togetherWith
                    (
                        slideOutHorizontally(tween(BueffelMotion.Settle)) { -it / 3 } +
                            fadeOut(tween(BueffelMotion.Quick))
                    )
            },
            label = "round",
        ) { target ->
            if (target == null) {
                FinishedPanel(total = session.total, onDone = { onFinished(session.snapshot()) })
            } else {
                Round(
                    view = target,
                    picked = if (target.round == round) chosen else null,
                    onPick = { position ->
                        if (picked == null) {
                            picked = position
                            if (soundOn) feedback.play(correct = position == target.correctPosition)
                        }
                    },
                )
            }
        }
    }
}

/** Everything one round shows, captured so entering and leaving rounds can animate side by side */
private data class RoundView(
    val round: Int,
    val prompt: String,
    val answers: List<String>,
    val correctPosition: Int,
    val remaining: Int,
    val hard: Boolean,
)

/** One question with its answers: the question up in the air, the answers down at the thumb */
@Composable
private fun Round(
    view: RoundView,
    picked: Int?,
    onPick: (Int) -> Unit,
) {
    // Everything scrolls together. Scrolling only the question left a long set of answers
    // clipped at the bottom with no way to reach them - the air between the two blocks is worth
    // having, but not at the price of an answer you cannot tap.
    //
    // The column is at least as tall as the screen, so SpaceBetween holds the question up and
    // the answers down while they fit. Once they do not, the column simply grows past the
    // screen, the free space is gone, and the whole thing scrolls.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Spacer(Modifier.height(6.dp))
                Caption(
                    text = if (view.remaining == 1) "Letzte Frage" else "Noch ${view.remaining} Fragen",
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = view.prompt,
                    style = MaterialTheme.typography.displaySmall,
                    color = BueffelColors.TextPrimary,
                )
            }

            Column {
                Spacer(Modifier.height(24.dp))
                view.answers.forEachIndexed { position, answer ->
                    AnswerCard(
                        text = answer,
                        state = answerState(position, picked, view.correctPosition),
                        onClick = { onPick(position) },
                    )
                    Spacer(Modifier.height(BueffelShape.Gap))
                }

                // the strip keeps its height whether or not an answer is showing, so the boxes
                // above never shift under a finger that is about to tap one
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth().height(VERDICT_STRIP_HEIGHT),
                ) {
                    // the reveal is driven by a value rather than by AnimatedVisibility: inside a
                    // Box the ColumnScope overload is the one in scope, and it cannot be called
                    val appear by animateFloatAsState(
                        targetValue = if (picked == null) 0f else 1f,
                        animationSpec = tween(BueffelMotion.Quick),
                        label = "verdict",
                    )
                    if (picked != null) {
                        Verdict(
                            correct = picked == view.correctPosition,
                            modifier =
                                Modifier.graphicsLayer {
                                    alpha = appear
                                    translationY = (1f - appear) * VERDICT_RISE.toPx()
                                },
                        )
                    }
                }
            }
        }
    }
}

/** Kept clear of the answer boxes so revealing a verdict never moves them */
private val VERDICT_STRIP_HEIGHT = 76.dp

/** How far the verdict rises into place as it appears */
private val VERDICT_RISE = 10.dp

/**
 * One row across the top: a way out, the bar, and the flag.
 *
 * Everything lives on one line so the questions start as high as they can. The bar takes what
 * the controls leave, which is why those are single glyphs rather than words.
 */
@Composable
private fun TopBar(
    progress: Float,
    hard: Boolean,
    canUndo: Boolean,
    onHard: (Boolean) -> Unit,
    onUndo: () -> Unit,
    onLeave: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlyphAction(glyph = "\u00d7", description = "Schluss", onClick = onLeave)
        if (canUndo) {
            Spacer(Modifier.width(8.dp))
            GlyphAction(glyph = "\u2190", description = "Zurück", onClick = onUndo)
        }
        ProgressBar(
            fraction = progress,
            modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
            height = 10.dp,
        )
        HardFlag(hard = hard, onChange = onHard)
    }
}

/**
 * Marks the question on screen as one that keeps going wrong.
 *
 * A flagged question comes back twice as often as its box would otherwise say, which is the
 * whole point of being able to say so: the schedule is an average, and the one question that
 * refuses to stick should not have to wait its turn with the rest.
 */
@Composable
private fun HardFlag(
    hard: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val background by animateColorAsState(
        targetValue = if (hard) BueffelColors.Almost else BueffelColors.Surface,
        animationSpec = tween(BueffelMotion.Quick),
        label = "flagFill",
    )
    val foreground by animateColorAsState(
        targetValue = if (hard) BueffelColors.Background else BueffelColors.TextMuted,
        animationSpec = tween(BueffelMotion.Quick),
        label = "flagText",
    )
    Text(
        text = "schwer",
        style = MaterialTheme.typography.labelSmall,
        color = foreground,
        modifier =
            Modifier
                .clip(RoundedCornerShape(BueffelShape.Pill))
                .background(background)
                .clickable { onChange(!hard) }
                .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

/** A single character on a round target, big enough to hit without looking */
@Composable
private fun GlyphAction(
    glyph: String,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(BueffelColors.Surface)
                .clickable(onClickLabel = description, onClick = onClick),
    ) {
        Text(
            text = glyph,
            style = MaterialTheme.typography.titleMedium,
            color = BueffelColors.TextSecondary,
        )
    }
}

private enum class AnswerState { Untouched, Correct, Wrong, Dimmed }

private fun answerState(
    index: Int,
    picked: Int?,
    correctIndex: Int,
): AnswerState =
    when {
        picked == null -> AnswerState.Untouched
        index == correctIndex -> AnswerState.Correct
        index == picked -> AnswerState.Wrong
        else -> AnswerState.Dimmed
    }

/**
 * One answer, as a rounded box carrying its own text.
 *
 * A box rather than a stadium: full pill ends eat into the first and last line of a two-line
 * answer, and four tall pills in a stack read as four separate blobs instead of one list.
 *
 * There is no letter on it. The answer is written right there, so a badge saying "C" beside it
 * would only name something the reader is already looking at.
 */
@Composable
private fun AnswerCard(
    text: String,
    state: AnswerState,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && state == AnswerState.Untouched) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "answerPress",
    )
    val fill by animateColorAsState(
        targetValue =
            when (state) {
                AnswerState.Correct -> BueffelColors.CorrectSurface
                AnswerState.Wrong -> BueffelColors.WrongSurface
                else -> BueffelColors.Surface
            },
        animationSpec = tween(BueffelMotion.Quick),
        label = "answerFill",
    )
    val stroke by animateColorAsState(
        targetValue =
            when (state) {
                AnswerState.Correct -> BueffelColors.Correct
                AnswerState.Wrong -> BueffelColors.Wrong
                AnswerState.Dimmed -> BueffelColors.Surface
                else -> BueffelColors.Border
            },
        animationSpec = tween(BueffelMotion.Quick),
        label = "answerStroke",
    )
    val textColor by animateColorAsState(
        targetValue =
            when (state) {
                AnswerState.Dimmed -> BueffelColors.TextMuted
                AnswerState.Correct -> BueffelColors.Correct
                AnswerState.Wrong -> BueffelColors.Wrong
                else -> BueffelColors.TextPrimary
            },
        animationSpec = tween(BueffelMotion.Quick),
        label = "answerText",
    )

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier =
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }.clip(RoundedCornerShape(BueffelShape.Radius))
                .background(fill)
                .border(BorderStroke(1.dp, stroke), RoundedCornerShape(BueffelShape.Radius))
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = state == AnswerState.Untouched,
                    onClick = onClick,
                ).padding(horizontal = 22.dp, vertical = 16.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
        )
    }
}

/**
 * Says how it went. Tapping anywhere on the screen moves on.
 *
 * It does not repeat the right answer: that answer is on screen already, outlined in green, and
 * spelling it out again ran to three lines and off the bottom of the strip.
 */
@Composable
private fun Verdict(
    correct: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(
            text = if (correct) "Richtig" else "Falsch",
            style = MaterialTheme.typography.titleLarge,
            color = if (correct) BueffelColors.Correct else BueffelColors.Wrong,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Caption(text = "Egal wo tippen für weiter")
    }
}

/** Shown once every question has reached the last box */
@Composable
private fun FinishedPanel(
    total: Int,
    onDone: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.weight(1f))
        Text(
            text = "durch",
            style = MaterialTheme.typography.displayLarge,
            color = BueffelColors.TextPrimary,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Alle $total Fragen sitzen — jede ${Card.LEARNED_BOX}-mal hintereinander richtig.",
            style = MaterialTheme.typography.bodyLarge,
            color = BueffelColors.TextSecondary,
        )
        Spacer(Modifier.height(36.dp))
        BueffelButton(text = "Fertig", onClick = onDone)
        Spacer(Modifier.height(20.dp))
    }
}
