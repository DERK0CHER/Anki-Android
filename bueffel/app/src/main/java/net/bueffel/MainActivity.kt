package net.bueffel

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import net.bueffel.data.DeckStore
import net.bueffel.data.Settings
import net.bueffel.importer.DeckBuilder
import net.bueffel.model.Card
import net.bueffel.model.Deck
import net.bueffel.ui.DeckListScreen
import net.bueffel.ui.ImportScreen
import net.bueffel.ui.StudyScreen
import net.bueffel.ui.SubtopicScreen
import net.bueffel.ui.theme.BueffelTheme
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val store = DeckStore(applicationContext)
        val settings = Settings(applicationContext)
        setContent {
            BueffelTheme {
                BueffelApp(store, settings)
            }
        }
    }
}

/** Where in the app the user is. Four screens do not need a navigation library. */
private sealed interface Screen {
    data object Decks : Screen

    data object Import : Screen

    data class Subtopics(
        val deckId: String,
    ) : Screen

    /** [subtopicId] null means the whole topic, every part mixed together */
    data class Study(
        val deckId: String,
        val subtopicId: String?,
    ) : Screen
}

@Composable
private fun BueffelApp(
    store: DeckStore,
    settings: Settings,
) {
    var decks by remember { mutableStateOf(store.load()) }
    var screen by remember { mutableStateOf<Screen>(Screen.Decks) }
    var soundOn by remember { mutableStateOf(settings.soundOn) }

    fun persist(updated: List<Deck>) {
        decks = updated
        store.save(updated)
    }

    /** Writes back how far each question has come, and steps back out */
    fun finishStudying(
        deckId: String,
        subtopicId: String?,
        cards: List<Card>,
    ) {
        persist(
            decks.map { deck ->
                when {
                    deck.id != deckId -> deck
                    subtopicId != null -> deck.withCards(subtopicId, cards)
                    else -> deck.withMixedCards(cards)
                }
            },
        )
        val deck = decks.firstOrNull { it.id == deckId }
        // one part is not worth a screen of its own, so that topic goes straight back to the list
        screen = if (deck != null && deck.subtopics.size > 1) Screen.Subtopics(deckId) else Screen.Decks
    }

    // Backup and restore go through the system file picker, so the file lands wherever the
    // learner keeps things and no permission is needed for any of it.
    val context = LocalContext.current
    val exportFile =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BACKUP_TYPE)) { uri ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(store.export(decks).toByteArray())
                    }
                }.onFailure { Log.w(TAG, "could not write the backup", it) }
            }
        }
    val restoreFile =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                runCatching {
                    val text =
                        context.contentResolver
                            .openInputStream(uri)
                            ?.bufferedReader()
                            ?.use { it.readText() }
                    if (text != null) persist(store.restore(decks, text))
                }.onFailure { Log.w(TAG, "could not read the backup", it) }
            }
        }

    /** A topic with a single part has nothing to choose between: open it and start */
    fun open(deck: Deck) {
        screen =
            if (deck.subtopics.size <= 1) {
                Screen.Study(deck.id, deck.subtopics.firstOrNull()?.id)
            } else {
                Screen.Subtopics(deck.id)
            }
    }

    when (val current = screen) {
        Screen.Decks ->
            DeckListScreen(
                decks = decks,
                soundOn = soundOn,
                onSoundChange = {
                    soundOn = it
                    settings.soundOn = it
                },
                onOpen = { open(it) },
                onImport = { screen = Screen.Import },
                onExport = { exportFile.launch("bueffel-${LocalDate.now()}.json") },
                onRestore = { restoreFile.launch(arrayOf(BACKUP_TYPE, "text/plain", "*/*")) },
            )

        Screen.Import -> {
            BackHandler { screen = Screen.Decks }
            ImportScreen(
                onCancel = { screen = Screen.Decks },
                onImport = { name, questions ->
                    if (questions.isNotEmpty()) {
                        persist(
                            decks +
                                DeckBuilder.build(
                                    id = System.currentTimeMillis().toString(),
                                    name = name,
                                    questions = questions,
                                ),
                        )
                    }
                    screen = Screen.Decks
                },
            )
        }

        is Screen.Subtopics -> {
            val deck = decks.firstOrNull { it.id == current.deckId }
            if (deck == null) {
                // writing state during composition would risk looping, so step out afterwards
                LaunchedEffect(current.deckId) { screen = Screen.Decks }
            } else {
                BackHandler { screen = Screen.Decks }
                SubtopicScreen(
                    deck = deck,
                    onOpen = { screen = Screen.Study(deck.id, it.id) },
                    onStudyAll = { screen = Screen.Study(deck.id, null) },
                    onBack = { screen = Screen.Decks },
                )
            }
        }

        is Screen.Study -> {
            val deck = decks.firstOrNull { it.id == current.deckId }
            val cards =
                if (current.subtopicId == null) {
                    deck?.cards
                } else {
                    deck?.subtopics?.firstOrNull { it.id == current.subtopicId }?.cards
                }
            if (deck == null || cards == null) {
                LaunchedEffect(current) { screen = Screen.Decks }
            } else {
                // no BackHandler here: only the screen holds the session, so only it knows what
                // to write back. Passing these cards would restore the state before studying.
                StudyScreen(
                    key = "${deck.id}/${current.subtopicId}",
                    cards = cards,
                    soundOn = soundOn,
                    onFinished = { finishStudying(deck.id, current.subtopicId, it) },
                    onLeave = { finishStudying(deck.id, current.subtopicId, it) },
                )
            }
        }
    }
}

private const val TAG = "Bueffel"

/** What a backup is written as, and the first thing offered when one is picked back up */
private const val BACKUP_TYPE = "application/json"
