package net.bueffel

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import net.bueffel.model.Card
import net.bueffel.model.Deck
import net.bueffel.model.Question
import net.bueffel.ui.StudyScreen
import net.bueffel.ui.theme.BueffelTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Drives the study screen the way a finger does, to check the shuffle against the grading.
 *
 * The pills are drawn in a fresh random order every round, so what the screen knows is a
 * position and what the question knows is an index. Every answer here is picked by its own
 * text, never by where it sits: if the two ever came apart, the right answer would start
 * scoring as wrong and the deck would never finish.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class StudyScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun deck() =
        Deck(
            id = "d",
            name = "Farben",
            cards =
                listOf(
                    Card(
                        Question(
                            prompt = "Welche Farbe hat der Himmel?",
                            answers = listOf("blau", "grün", "gelb", "violett"),
                            correctIndex = 0,
                        ),
                    ),
                ),
        )

    private fun show() {
        composeRule.setContent {
            BueffelTheme {
                StudyScreen(deck = deck(), soundOn = false, onFinished = {}, onLeave = {})
            }
        }
    }

    /** Tapping anywhere moves on; the meter's caption is the one target outside every pill */
    private fun advance() = composeRule.onNodeWithText("Lern-O-Meter").performClick()

    @Test
    fun `the right answer counts as right wherever the shuffle puts it`() {
        show()

        repeat(Card.LEARNED_BOX) {
            composeRule.onNodeWithText("blau").performClick()
            composeRule.onNodeWithText("Richtig").assertIsDisplayed()
            advance()
        }

        composeRule.onNodeWithText("durch").assertIsDisplayed()
    }

    @Test
    fun `a wrong answer counts as wrong wherever the shuffle puts it`() {
        show()

        // more rounds than there are answers, so a mapping that only happens to line up once
        // does not get away with it
        repeat(12) {
            composeRule.onNodeWithText("violett").performClick()
            composeRule.onNodeWithText("Falsch").assertIsDisplayed()
            advance()
        }
    }

    @Test
    fun `the question never finishes on wrong answers alone`() {
        show()

        repeat(Card.LEARNED_BOX) {
            composeRule.onNodeWithText("grün").performClick()
            advance()
        }

        composeRule.onNodeWithText("Welche Farbe hat der Himmel?").assertIsDisplayed()
    }
}
