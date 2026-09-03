package net.bueffel

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import net.bueffel.data.DeckStore
import net.bueffel.data.ImageStore
import net.bueffel.data.Settings
import net.bueffel.importer.DeckBuilder
import net.bueffel.model.Card
import net.bueffel.model.Deck
import net.bueffel.ui.DeckListScreen
import net.bueffel.ui.ImportScreen
import net.bueffel.ui.LocalImages
import net.bueffel.ui.StudyScreen
import net.bueffel.ui.SubtopicScreen
import net.bueffel.ui.theme.BueffelTheme
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // without this the volume keys adjust the ringer while nothing happens to be playing,
        // so a two hundred millisecond tone can never be caught in time to turn it down
        volumeControlStream = AudioManager.STREAM_MUSIC
        val store = DeckStore(applicationContext)
        val images = ImageStore(applicationContext)
        val settings = Settings(applicationContext)
        setContent {
            BueffelTheme {
                // handed down rather than passed along: a picture can turn up on any card, and
                // threading the store through every round would say nothing about any of them
                CompositionLocalProvider(LocalImages provides images) {
                    BueffelApp(store, images, settings)
                }
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

    /**
     * [subtopicId] null means the whole topic, every part mixed together.
     *
     * [tags] narrows that to the cards carrying all of them; empty is everything. It only
     * applies to a whole topic, because a filter is a way of cutting across the parts and
     * cutting across one part is not a thing to ask for.
     */
    data class Study(
        val deckId: String,
        val subtopicId: String?,
        val tags: Set<String> = emptySet(),
    ) : Screen
}

@Composable
private fun BueffelApp(
    store: DeckStore,
    images: ImageStore,
    settings: Settings,
) {
    var decks by remember { mutableStateOf(store.load()) }
    var screen by remember { mutableStateOf<Screen>(Screen.Decks) }
    var soundOn by remember { mutableStateOf(settings.soundOn) }

    fun persist(updated: List<Deck>) {
        decks = updated
        store.save(updated)
    }

    /** Writes back how far each question has come, without going anywhere */
    fun keepProgress(
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
    }

    /** Writes it back and steps out of the study screen */
    fun finishStudying(
        deckId: String,
        subtopicId: String?,
        cards: List<Card>,
    ) {
        keepProgress(deckId, subtopicId, cards)
        val deck = decks.firstOrNull { it.id == deckId }
        // one part and no tags is nothing to choose between, so that topic goes straight back to
        // the list rather than to a screen holding a single row
        screen = if (deck != null && deck.hasChoices) Screen.Subtopics(deckId) else Screen.Decks
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
    // A card file is written at a desk and arrives as a file. Reading it here rather than
    // through the clipboard is the same picker one line further along, and it takes the
    // copy-and-paste fiddling out of importing a set of code cards.
    var importedText by remember { mutableStateOf<String?>(null) }
    val importFile =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                importedText =
                    runCatching {
                        context.contentResolver
                            .openInputStream(uri)
                            ?.bufferedReader()
                            ?.use { it.readText() }
                    }.onFailure { Log.w(TAG, "could not read the card file", it) }
                        .getOrNull()
            }
        }
    // The pictures come in on their own, several at a time, and are filed under their own names.
    // A path relative to the card file would be the obvious thing to write, and cannot work: the
    // dialog hands back a content:// URI with no way to reach the directory it came from.
    var picturesAdded by remember { mutableIntStateOf(0) }
    val addImages =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            var added = 0
            for (uri in uris) {
                runCatching {
                    val name = displayName(context, uri) ?: uri.lastPathSegment ?: return@runCatching
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes != null && images.save(name, bytes) != null) added++
                }.onFailure { Log.w(TAG, "could not read a picture", it) }
            }
            picturesAdded = added
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

    /** A topic with one part and no tags has nothing to choose between: open it and start */
    fun open(deck: Deck) {
        screen =
            if (deck.hasChoices) {
                Screen.Subtopics(deck.id)
            } else {
                Screen.Study(deck.id, deck.subtopics.firstOrNull()?.id)
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
            // the file that was read has to go when the screen does, or coming back would show
            // the last import again as though it had just been picked
            fun leaveImport() {
                importedText = null
                picturesAdded = 0
                screen = Screen.Decks
            }
            BackHandler { leaveImport() }
            ImportScreen(
                onCancel = { leaveImport() },
                onPickFile = { importFile.launch(CARD_FILE_TYPES) },
                fileText = importedText,
                onPickImages = { addImages.launch(arrayOf("image/*")) },
                picturesAdded = picturesAdded,
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
                    leaveImport()
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
                    onStudyTagged = { screen = Screen.Study(deck.id, null, it) },
                )
            }
        }

        is Screen.Study -> {
            val deck = decks.firstOrNull { it.id == current.deckId }
            val cards =
                if (current.subtopicId == null) {
                    deck?.cardsTagged(current.tags)
                } else {
                    deck?.subtopics?.firstOrNull { it.id == current.subtopicId }?.cards
                }
            if (deck == null || cards == null) {
                LaunchedEffect(current) { screen = Screen.Decks }
            } else {
                // no BackHandler here: only the screen holds the session, so only it knows what
                // to write back. Passing these cards would restore the state before studying.
                StudyScreen(
                    // the selection is part of what is being studied: picking other tags is a
                    // different set of cards and therefore a different session
                    key = "${deck.id}/${current.subtopicId}/${current.tags.sorted()}",
                    cards = cards,
                    soundOn = soundOn,
                    onFinished = { finishStudying(deck.id, current.subtopicId, it) },
                    onLeave = { finishStudying(deck.id, current.subtopicId, it) },
                    onProgress = { keepProgress(deck.id, current.subtopicId, it) },
                )
            }
        }
    }
}

/**
 * What the picker calls a file.
 *
 * That name is what the card file will refer to, so it is what the picture has to be filed
 * under; the URI's own last segment is a document id and means nothing to anybody.
 */
private fun displayName(
    context: Context,
    uri: Uri,
): String? =
    runCatching {
        context.contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    }.getOrNull()

private const val TAG = "Bueffel"

/** What a backup is written as, and the first thing offered when one is picked back up */
private const val BACKUP_TYPE = "application/json"

/**
 * What the picker offers for a card file.
 *
 * A `.txt` or `.md` written on a desktop and copied over often arrives as
 * `application/octet-stream`, so the catch-all has to be in the list or the file the learner
 * came to fetch would be greyed out.
 */
private val CARD_FILE_TYPES = arrayOf("text/*", BACKUP_TYPE, "*/*")
