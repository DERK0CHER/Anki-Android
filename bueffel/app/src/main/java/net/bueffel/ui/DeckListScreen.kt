package net.bueffel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import net.bueffel.model.Deck
import net.bueffel.ui.theme.BueffelColors
import net.bueffel.ui.theme.BueffelShape

/**
 * The home screen: what there is to learn, and how far along each set is.
 *
 * The composition is a masthead at the top and everything actionable gathered at the bottom,
 * within reach of a thumb: the deck cards sit directly above the buttons rather than hanging
 * off the wordmark with a void beneath them. The space in between is deliberate, not left over.
 */
@Composable
fun DeckListScreen(
    decks: List<Deck>,
    soundOn: Boolean,
    onSoundChange: (Boolean) -> Unit,
    onOpen: (Deck) -> Unit,
    onImport: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BueffelColors.Background)
                .systemBarsPadding()
                .padding(horizontal = BueffelShape.Gutter),
    ) {
        Spacer(Modifier.height(28.dp))
        Text(
            text = "büffeln",
            style = MaterialTheme.typography.displayLarge,
            color = BueffelColors.TextPrimary,
        )
        Spacer(Modifier.height(10.dp))
        Caption(text = summaryLine(decks))

        if (decks.isEmpty()) {
            EmptyState(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                // gravity towards the buttons below: a short list clusters at the bottom
                // instead of leaving a black gulf between itself and the actions
                verticalArrangement = Arrangement.spacedBy(BueffelShape.Gap, Alignment.Bottom),
                contentPadding = PaddingValues(top = 24.dp, bottom = 4.dp),
            ) {
                items(decks, key = { it.id }) { deck ->
                    DeckRow(deck = deck, onClick = { onOpen(deck) })
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        PillToggle(checked = soundOn, label = "Ton", onCheckedChange = onSoundChange)
        Spacer(Modifier.height(14.dp))
        BueffelButton(text = "Fragen einfügen", onClick = onImport)
        Spacer(Modifier.height(20.dp))
    }
}

/** One quiet line under the wordmark, so the header says what the app currently holds */
private fun summaryLine(decks: List<Deck>): String {
    if (decks.isEmpty()) return "Multiple Choice, bis es sitzt"
    val questions = decks.sumOf { it.cards.size }
    val themes = if (decks.size == 1) "1 Thema" else "${decks.size} Themen"
    val count = if (questions == 1) "1 Frage" else "$questions Fragen"
    return "$themes · $count"
}

/**
 * The first launch, laid out as an invitation rather than a shrug.
 *
 * The three steps stand where the decks will later stand - directly above the button that
 * starts them - so the empty screen already has the shape of the full one.
 */
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(
            text = "Noch leer.",
            style = MaterialTheme.typography.displayMedium,
            color = BueffelColors.TextPrimary,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Eine KI schreibt dir die Fragen. Büffel fragt sie ab, bis jede sitzt.",
            style = MaterialTheme.typography.bodyLarge,
            color = BueffelColors.TextSecondary,
        )
        Spacer(Modifier.height(30.dp))
        StepLabel(number = "1", text = "Prompt kopieren", textColor = BueffelColors.TextSecondary)
        Spacer(Modifier.height(14.dp))
        StepLabel(number = "2", text = "Von einer KI beantworten lassen", textColor = BueffelColors.TextSecondary)
        Spacer(Modifier.height(14.dp))
        StepLabel(number = "3", text = "Antwort hier einfügen", textColor = BueffelColors.TextSecondary)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DeckRow(
    deck: Deck,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BueffelShape.Radius))
                .background(BueffelColors.Surface)
                .clickable(onClick = onClick)
                .padding(22.dp),
    ) {
        Text(
            text = deck.name,
            style = MaterialTheme.typography.titleLarge,
            color = BueffelColors.TextPrimary,
        )
        Spacer(Modifier.height(6.dp))
        // "sicher" rather than "sitzen": the count is often one, and "1 sitzen" reads wrong
        Caption(text = "${deck.cards.size} Fragen · ${deck.learnedCount} sicher")
        Spacer(Modifier.height(18.dp))
        LernOMeter(fraction = deck.progress, label = "Lern-O-Meter", height = 12.dp)
    }
}
