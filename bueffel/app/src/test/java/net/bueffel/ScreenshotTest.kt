package net.bueffel

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import net.bueffel.model.Card
import net.bueffel.model.CodeTask
import net.bueffel.model.Deck
import net.bueffel.model.Question
import net.bueffel.model.Subtopic
import net.bueffel.ui.DeckListScreen
import net.bueffel.ui.ImportScreen
import net.bueffel.ui.StudyScreen
import net.bueffel.ui.SubtopicScreen
import net.bueffel.ui.theme.BueffelTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders each screen to a PNG.
 *
 * There is no emulator available for this project, so this is how the interface actually gets
 * looked at: the screens are drawn by the real Compose code in a JVM test and CI publishes the
 * images. It catches what unit tests cannot - text that overflows, a control pushed off screen,
 * spacing that reads wrong on a phone-sized window.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun card(
        prompt: String,
        answers: List<String>,
        correctIndex: Int,
        box: Int = 0,
    ) = Card(Question(prompt, answers, correctIndex), box = box)

    private val breakdown =
        card(
            prompt = "Wie verhältst du dich bei einer Panne auf der Autobahn?",
            answers =
                listOf(
                    "Warnblinkanlage einschalten und Warnweste anlegen",
                    "Auf der Fahrbahn stehen bleiben und winken",
                    "Das Fahrzeug verlassen und auf dem Standstreifen warten",
                    "Den Motor laufen lassen und sitzen bleiben",
                ),
            correctIndex = 0,
            box = 5,
        )

    private val centreLine =
        card(
            prompt = "Was bedeutet ein durchgezogener Mittelstreifen?",
            answers =
                listOf(
                    "Überholen ist erlaubt",
                    "Er darf nicht überfahren werden",
                    "Er markiert eine Baustelle",
                ),
            correctIndex = 1,
        )

    /** A topic in parts, each one at a different stage, which is what the bars are for */
    private fun sampleDeck() =
        Deck(
            id = "sample",
            name = "Theorieprüfung Klasse B",
            subtopics =
                listOf(
                    Subtopic("sample-0", "Verkehrszeichen", listOf(centreLine.copy(box = Card.LEARNED_BOX))),
                    Subtopic("sample-1", "Verhalten im Verkehr", listOf(breakdown, centreLine)),
                    Subtopic("sample-2", "Erste Hilfe", listOf(centreLine.copy(box = 2), breakdown.copy(box = 0))),
                ),
        )

    private fun capture(name: String) {
        composeRule.onRoot().captureRoboImage("$OUTPUT_DIR/$name.png")
    }

    @Test
    fun deckListEmpty() {
        composeRule.setContent {
            BueffelTheme { DeckListScreen(decks = emptyList(), soundOn = true, onSoundChange = {}, onOpen = {}, onImport = {}) }
        }
        capture("01-decks-empty")
    }

    @Test
    fun deckList() {
        composeRule.setContent {
            BueffelTheme {
                DeckListScreen(
                    decks = listOf(sampleDeck()),
                    soundOn = true,
                    onSoundChange = {},
                    onOpen = {},
                    onImport = {},
                )
            }
        }
        capture("02-decks")
    }

    @Test
    fun importScreen() {
        composeRule.setContent {
            BueffelTheme { ImportScreen(onCancel = {}, onImport = { _, _ -> }) }
        }
        capture("03-import")
    }

    @Test
    fun studyQuestion() {
        composeRule.setContent {
            BueffelTheme {
                StudyScreen(key = "s", cards = sampleDeck().cards, soundOn = false, onFinished = {}, onLeave = {})
            }
        }
        capture("04-study-question")
    }

    @Test
    fun studyAnsweredWrong() {
        composeRule.setContent {
            BueffelTheme {
                StudyScreen(key = "s", cards = listOf(breakdown), soundOn = false, onFinished = {}, onLeave = {})
            }
        }
        // the order is shuffled on every presentation, so pick by the answer's own text
        composeRule.onNodeWithText("Auf der Fahrbahn stehen bleiben und winken").performClick()
        capture("05-study-wrong")
    }

    @Test
    fun studyFinished() {
        // one question, one box short of learned: a single right answer finishes the set
        val nearly = listOf(centreLine.copy(box = Card.LEARNED_BOX - 1))
        composeRule.setContent {
            BueffelTheme {
                StudyScreen(key = "s", cards = nearly, soundOn = false, onFinished = {}, onLeave = {})
            }
        }
        composeRule.onNodeWithText("Er darf nicht überfahren werden").performClick()
        // tapping anywhere moves on, and the question is the one target outside every answer
        composeRule.onNodeWithText("Was bedeutet ein durchgezogener Mittelstreifen?").performClick()
        capture("06-study-finished")
    }

    /**
     * The case that broke: more question and answer than fits.
     *
     * Only the question used to scroll, so a set of long answers ran off the bottom with no way
     * to reach the last one. This renders that state, which is the only way to see it here.
     */
    @Test
    fun studyLongQuestion() {
        val long =
            listOf(
                card(
                    prompt =
                        "Du näherst dich bei Nacht einer unbeschrankten Bahnübergangstelle und " +
                            "siehst das Andreaskreuz. Wie verhältst du dich?",
                    answers =
                        listOf(
                            "Mit mäßiger Geschwindigkeit heranfahren, auf Signale achten und " +
                                "notfalls vor dem Andreaskreuz anhalten",
                            "Zügig über den Übergang fahren, damit du ihn schnell wieder " +
                                "verlässt und niemanden aufhältst",
                            "Anhalten, aussteigen und in beide Richtungen die Strecke absuchen, " +
                                "bevor du weiterfährst",
                            "Hupen und die Lichthupe betätigen, um auf dich aufmerksam zu " +
                                "machen, dann weiterfahren",
                        ),
                    correctIndex = 0,
                ),
            )
        composeRule.setContent {
            BueffelTheme {
                StudyScreen(key = "s", cards = long, soundOn = false, onFinished = {}, onLeave = {})
            }
        }
        capture("07-study-long")
    }

    /** The parts of one topic, each with its own bar */
    @Test
    fun subtopics() {
        composeRule.setContent {
            BueffelTheme {
                SubtopicScreen(deck = sampleDeck(), onOpen = {}, onStudyAll = {}, onBack = {})
            }
        }
        capture("08-subtopics")
    }

    private val nodeDelete =
        CodeTask(
            prompt = "Vervollständige node_delete:\n\nvoid node_delete(node_t *n) {\n${CodeTask.GAP}\n}",
            solution = "    free(n->data);\n    free(n);\n    n = NULL;",
            topic = "Verkettete Listen",
            tags = listOf("WS24"),
        )

    /** Sorting the model answer's lines, which is where a code card starts */
    @Test
    fun sortCode() {
        composeRule.setContent {
            BueffelTheme {
                StudyScreen(
                    key = "sort",
                    cards = listOf(Card(nodeDelete)),
                    soundOn = false,
                    onFinished = {},
                    onLeave = {},
                )
            }
        }
        capture("09-sort-code")
    }

    /** The editor, once the card has been sorted cleanly twice */
    @Test
    fun writeCode() {
        composeRule.setContent {
            BueffelTheme {
                StudyScreen(
                    key = "write",
                    cards = listOf(Card(nodeDelete, sorted = Card.SORTS_TO_WRITE)),
                    soundOn = false,
                    onFinished = {},
                    onLeave = {},
                )
            }
        }
        capture("10-write-code")
    }

    private companion object {
        const val OUTPUT_DIR = "build/outputs/roborazzi"
    }
}
