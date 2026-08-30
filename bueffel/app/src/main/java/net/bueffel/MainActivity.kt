package net.bueffel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import net.bueffel.data.DeckStore
import net.bueffel.importer.QuestionParser
import net.bueffel.model.Card
import net.bueffel.model.Deck
import net.bueffel.ui.DeckListScreen
import net.bueffel.ui.ImportScreen
import net.bueffel.ui.StudyScreen
import net.bueffel.ui.theme.BueffelTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val store = DeckStore(applicationContext)
        setContent {
            BueffelTheme {
                BueffelApp(store)
            }
        }
    }
}

/** Where in the app the user is. Three screens do not need a navigation library. */
private sealed interface Screen {
    data object Decks : Screen

    data object Import : Screen

    data class Study(
        val deckId: String,
    ) : Screen
}

@Composable
private fun BueffelApp(store: DeckStore) {
    var decks by remember { mutableStateOf(store.load()) }
    var screen by remember { mutableStateOf<Screen>(Screen.Decks) }

    fun persist(updated: List<Deck>) {
        decks = updated
        store.save(updated)
    }

    /** Writes back how far each question has come, and returns to the deck list */
    fun finishStudying(
        deckId: String,
        cards: List<Card>,
    ) {
        persist(decks.map { if (it.id == deckId) it.copy(cards = cards) else it })
        screen = Screen.Decks
    }

    when (val current = screen) {
        Screen.Decks ->
            DeckListScreen(
                decks = decks,
                onOpen = { screen = Screen.Study(it.id) },
                onImport = { screen = Screen.Import },
            )

        Screen.Import -> {
            BackHandler { screen = Screen.Decks }
            ImportScreen(
                onCancel = { screen = Screen.Decks },
                onImport = { name, text ->
                    val parsed = QuestionParser.parse(text)
                    if (parsed.questions.isNotEmpty()) {
                        val deck =
                            Deck(
                                id = System.currentTimeMillis().toString(),
                                name = name,
                                cards = parsed.questions.map { Card(it) },
                            )
                        persist(decks + deck)
                    }
                    screen = Screen.Decks
                },
            )
        }

        is Screen.Study -> {
            val deck = decks.firstOrNull { it.id == current.deckId }
            if (deck == null) {
                // writing state during composition would risk looping, so step out afterwards
                LaunchedEffect(current.deckId) { screen = Screen.Decks }
            } else {
                BackHandler { screen = Screen.Decks }
                StudyScreen(
                    deck = deck,
                    onFinished = { finishStudying(deck.id, it) },
                    onLeave = { finishStudying(deck.id, it) },
                )
            }
        }
    }
}
