package net.bueffel.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import net.bueffel.model.Deck
import net.bueffel.ui.theme.BueffelColors
import net.bueffel.ui.theme.BueffelShape

/** The home screen: what there is to learn, and how far along each set is */
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

        Spacer(Modifier.height(20.dp))

        if (decks.isEmpty()) {
            Text(
                text =
                    "Noch nichts zu lernen. Lass dir von einer KI Multiple-Choice-Fragen " +
                        "schreiben und füge sie hier ein.",
                style = MaterialTheme.typography.bodyLarge,
                color = BueffelColors.TextSecondary,
            )
            Spacer(Modifier.weight(1f))
        } else {
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(BueffelShape.Gap),
            ) {
                items(decks, key = { it.id }) { deck ->
                    DeckRow(deck = deck, onClick = { onOpen(deck) })
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        PillToggle(checked = soundOn, label = "Ton", onCheckedChange = onSoundChange)
        Spacer(Modifier.height(14.dp))
        BueffelButton(text = "Fragen einfügen", onClick = onImport)
        Spacer(Modifier.height(20.dp))
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
                .padding(20.dp),
    ) {
        Text(
            text = deck.name,
            style = MaterialTheme.typography.titleLarge,
            color = BueffelColors.TextPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Caption(text = "${deck.cards.size} Fragen")
            Caption(
                text = "${deck.learnedCount} von ${deck.cards.size} sitzt",
                color = BueffelColors.progressColor(deck.progress),
            )
        }
        Spacer(Modifier.height(14.dp))
        ProgressLine(fraction = deck.progress)
    }
}

/** How much of a set has reached the last box */
@Composable
private fun ProgressLine(fraction: Float) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(BueffelShape.Pill))
                .background(BueffelColors.SurfaceRaised),
    ) {
        if (fraction > 0f) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .height(4.dp)
                        .clip(RoundedCornerShape(BueffelShape.Pill))
                        .background(BueffelColors.progressColor(fraction)),
            )
        }
    }
}
