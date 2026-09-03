package net.bueffel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import net.bueffel.model.CodeTask
import net.bueffel.ui.SortRound
import net.bueffel.ui.theme.BueffelTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Drags a row, which is the one thing about this screen a still picture cannot show.
 *
 * This was written down as "unverified, needs a device" and that was wrong: a long press and a
 * drag are events like any other, and the same JVM test that draws the screen can send them. An
 * emulator would only have added a window to look at.
 *
 * The rows are shuffled on every presentation, so nothing here names a row by its content: the
 * test finds the top two by where they are and checks that they have changed places.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class SortRoundTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val lines = listOf("erste();", "zweite();", "dritte();")

    private val task =
        CodeTask(
            prompt = "Bring die Zeilen in die richtige Reihenfolge",
            solution = lines.joinToString("\n"),
        )

    private fun yOf(line: String): Float = composeRule.onNodeWithText(line).fetchSemanticsNode().positionInRoot.y

    private fun heightOf(line: String): Float =
        composeRule
            .onNodeWithText(line)
            .fetchSemanticsNode()
            .size.height
            .toFloat()

    private fun show() {
        composeRule.setContent {
            BueffelTheme { SortRound(task = task, round = "Runde 1 · 4× richtig", onSubmit = {}) }
        }
    }

    @Test
    fun `a row dragged past the one under it changes places with it`() {
        show()
        val order = lines.sortedBy { yOf(it) }
        val top = order[0]
        val second = order[1]
        // far enough to cross one row, not so far as to depend on the gap between them
        val travel = heightOf(top) * 2f

        composeRule.onNodeWithText(top).performTouchInput {
            down(center)
            // the gesture only starts after a long press, so the finger has to wait
            advanceEventTime(700)
            moveBy(Offset(0f, travel))
            advanceEventTime(50)
            up()
        }
        composeRule.mainClock.advanceTimeBy(500)

        assertTrue("the row that was picked up stayed where it was", yOf(top) > yOf(second))
    }

    @Test
    fun `a row that is only tapped stays where it is`() {
        show()
        val before = lines.sortedBy { yOf(it) }

        composeRule.onNodeWithText(before[0]).performClick()
        composeRule.mainClock.advanceTimeBy(500)

        // a tap is not a drag: a list that reordered itself on a stray touch would be unusable
        assertEquals(before, lines.sortedBy { yOf(it) })
    }

    @Test
    fun `checking the order gives a verdict and a way on`() {
        show()

        composeRule.onNodeWithText("Prüfen").performClick()

        // whichever way the shuffle fell, the round has to be finishable
        composeRule.onNodeWithText("Weiter").assertIsDisplayed()
    }
}
