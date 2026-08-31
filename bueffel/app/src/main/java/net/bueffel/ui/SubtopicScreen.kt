package net.bueffel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import net.bueffel.model.Deck
import net.bueffel.model.Subtopic
import net.bueffel.ui.theme.BueffelColors
import net.bueffel.ui.theme.BueffelShape

/**
 * The parts of one topic, each with its own progress.
 *
 * A theory paper is not one subject but a dozen, and knowing the signs are through while right
 * of way is still red is the reason to split them at all. The bar at the top is all of them
 * together, weighted by how many questions each part holds.
 */
@Composable
fun SubtopicScreen(
    deck: Deck,
    onOpen: (Subtopic) -> Unit,
    onStudyAll: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BueffelColors.Background)
                .systemBarsPadding()
                .padding(horizontal = BueffelShape.Gutter),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(BueffelColors.Surface)
                        .clickable(onClickLabel = "Zurück", onClick = onBack),
            ) {
                Text(
                    text = "←",
                    style = MaterialTheme.typography.titleMedium,
                    color = BueffelColors.TextSecondary,
                )
            }
            Spacer(Modifier.width(16.dp))
            ProgressBar(fraction = deck.progress, modifier = Modifier.weight(1f), height = 10.dp)
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = deck.name,
            style = MaterialTheme.typography.displayMedium,
            color = BueffelColors.TextPrimary,
        )
        Spacer(Modifier.height(8.dp))
        Caption(text = "${deck.cards.size} Fragen · ${deck.learnedCount} sicher")

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(BueffelShape.Gap, Alignment.CenterVertically),
            contentPadding = PaddingValues(top = 24.dp, bottom = 4.dp),
        ) {
            items(deck.subtopics, key = { it.id }) { subtopic ->
                SubtopicRow(subtopic = subtopic, onClick = { onOpen(subtopic) })
            }
        }

        Spacer(Modifier.height(16.dp))
        BueffelButton(text = "Alles gemischt lernen", onClick = onStudyAll)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun SubtopicRow(
    subtopic: Subtopic,
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
            text = subtopic.name,
            style = MaterialTheme.typography.titleMedium,
            color = BueffelColors.TextPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Caption(text = "${subtopic.cards.size} Fragen · ${subtopic.learnedCount} sicher")
        Spacer(Modifier.height(14.dp))
        ProgressBar(fraction = subtopic.progress, height = 10.dp)
    }
}
