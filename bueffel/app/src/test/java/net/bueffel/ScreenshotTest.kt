package net.bueffel

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import net.bueffel.model.Card
import net.bueffel.model.Deck
import net.bueffel.model.Question
import net.bueffel.ui.DeckListScreen
import net.bueffel.ui.ImportScreen
import net.bueffel.ui.StudyScreen
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

    private fun sampleDeck(learned: Int = 0) =
        Deck(
            id = "sample",
            name = "Theorieprüfung Klasse B",
            cards =
                listOf(
                    Card(
                        Question(
                            prompt = "Wie verhältst du dich bei einer Panne auf der Autobahn?",
                            answers =
                                listOf(
                                    "Warnblinkanlage einschalten und Warnweste anlegen",
                                    "Auf der Fahrbahn stehen bleiben und winken",
                                    "Das Fahrzeug verlassen und auf dem Standstreifen warten",
                                    "Den Motor laufen lassen und sitzen bleiben",
                                ),
                            correctIndex = 0,
                        ),
                        box = 5,
                    ),
                    Card(
                        Question(
                            prompt = "Was bedeutet ein durchgezogener Mittelstreifen?",
                            answers =
                                listOf(
                                    "Überholen ist erlaubt",
                                    "Er darf nicht überfahren werden",
                                    "Er markiert eine Baustelle",
                                ),
                            correctIndex = 1,
                        ),
                    ),
                ).mapIndexed { index, card ->
                    if (index < learned) card.copy(box = Card.LEARNED_BOX) else card
                },
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
    fun deckListWithDecks() {
        composeRule.setContent {
            BueffelTheme {
                DeckListScreen(
                    decks = listOf(sampleDeck(learned = 1)),
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
            BueffelTheme { StudyScreen(deck = sampleDeck(), soundOn = false, onFinished = {}, onLeave = {}) }
        }
        capture("04-study-question")
    }

    @Test
    fun studyAnsweredWrong() {
        composeRule.setContent {
            BueffelTheme { StudyScreen(deck = sampleDeck(), soundOn = false, onFinished = {}, onLeave = {}) }
        }
        // the order is shuffled on every presentation, so pick by the answer's own text
        composeRule.onNodeWithText("Auf der Fahrbahn stehen bleiben und winken").performClick()
        capture("05-study-wrong")
    }

    /**
     * The case that broke: more question and answer than fits.
     *
     * Only the question used to scroll, so a set of long answers ran off the bottom with no way
     * to reach the last one. This renders that state, which is the only way to see it here.
     */
    @Test
    fun studyLongQuestion() {
        val deck =
            Deck(
                id = "long",
                name = "Lange Fragen",
                cards =
                    listOf(
                        Card(
                            Question(
                                prompt =
                                    "Du näherst dich bei Nacht einer unbeschrankten Bahnübergang" +
                                        "stelle und siehst das Andreaskreuz. Wie verhältst du dich?",
                                answers =
                                    listOf(
                                        "Mit mäßiger Geschwindigkeit heranfahren, auf Signale achten " +
                                            "und notfalls vor dem Andreaskreuz anhalten",
                                        "Zügig über den Übergang fahren, damit du ihn schnell " +
                                            "wieder verlässt und niemanden aufhältst",
                                        "Anhalten, aussteigen und in beide Richtungen die Strecke " +
                                            "absuchen, bevor du weiterfährst",
                                        "Hupen und die Lichthupe betätigen, um auf dich aufmerksam " +
                                            "zu machen, dann weiterfahren",
                                    ),
                                correctIndex = 0,
                            ),
                        ),
                    ),
            )
        composeRule.setContent {
            BueffelTheme { StudyScreen(deck = deck, soundOn = false, onFinished = {}, onLeave = {}) }
        }
        capture("07-study-long")
    }

    @Test
    fun studyFinished() {
        // one question, one box short of learned: a single right answer finishes the deck
        val deck =
            Deck(
                id = "last",
                name = "Theorieprüfung Klasse B",
                cards =
                    listOf(
                        Card(
                            Question(
                                prompt = "Was bedeutet ein durchgezogener Mittelstreifen?",
                                answers =
                                    listOf(
                                        "Überholen ist erlaubt",
                                        "Er darf nicht überfahren werden",
                                        "Er markiert eine Baustelle",
                                    ),
                                correctIndex = 1,
                            ),
                            box = Card.LEARNED_BOX - 1,
                        ),
                    ),
            )
        composeRule.setContent {
            BueffelTheme { StudyScreen(deck = deck, soundOn = false, onFinished = {}, onLeave = {}) }
        }
        composeRule.onNodeWithText("Er darf nicht überfahren werden").performClick()
        // tapping anywhere moves on; the meter's caption is a target outside every answer box
        composeRule.onNodeWithText("Lern-O-Meter").performClick()
        capture("06-study-finished")
    }

    private companion object {
        const val OUTPUT_DIR = "build/outputs/roborazzi"
    }
}
