package net.bueffel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import net.bueffel.ui.LernOMeter
import net.bueffel.ui.theme.BueffelColors
import net.bueffel.ui.theme.BueffelTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/**
 * Reads the pixels of the drawn bar.
 *
 * The gradient has now been wrong twice in a row in a way no other test could see: once squeezed
 * into the drawn part, once laid across the whole track but centred on the drawn part, which
 * slides the red off the left end. Both times the bar looked plausible and both times the colour
 * lied about how well the set was known. So this reads the rendering itself.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class LernOMeterTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val fractions = listOf(0.15f, 0.4f, 0.7f, 1f)

    /** The colour of every bar at its left end and at its tip, in the order of [fractions] */
    private fun readBars(): List<Pair<Color, Color>> {
        composeRule.setContent {
            BueffelTheme {
                Column(modifier = Modifier.width(BAR_WIDTH).background(Color.Black)) {
                    fractions.forEach { LernOMeter(fraction = it, height = BAR_HEIGHT) }
                }
            }
        }

        val pixels = composeRule.onRoot().captureToImage().toPixelMap()
        val band = pixels.height / fractions.size
        // half a bar's height in from either end is the middle of the rounded cap, which is the
        // nearest fully opaque pixel to the edge
        val inset = band / 2
        return fractions.mapIndexed { index, fraction ->
            val y = index * band + band / 2
            val tipX = (pixels.width * fraction).toInt() - inset
            pixels[inset, y] to pixels[tipX, y]
        }
    }

    @Test
    fun `every bar starts at the red end, whatever it has earned`() {
        readBars().forEachIndexed { index, (left, _) ->
            assertClose("left end of the ${fractions[index]} bar", BueffelColors.Wrong, left)
        }
    }

    @Test
    fun `the tip moves along the run as the bar fills`() {
        val greenness = readBars().map { (_, tip) -> tip.green - tip.red }

        greenness.zipWithNext().forEachIndexed { index, (shorter, longer) ->
            assertTrue(
                "the ${fractions[index + 1]} bar should be further along than the " +
                    "${fractions[index]} one, but read $longer against $shorter",
                longer > shorter,
            )
        }
    }

    @Test
    fun `a finished bar is green at the tip`() {
        val (_, tip) = readBars().last()

        assertClose("tip of the finished bar", BueffelColors.LearnedGreen, tip)
    }

    private fun assertClose(
        what: String,
        expected: Color,
        actual: Color,
    ) {
        val off =
            maxOf(
                abs(expected.red - actual.red),
                abs(expected.green - actual.green),
                abs(expected.blue - actual.blue),
            )
        assertTrue("$what should be $expected but was $actual", off < TOLERANCE)
    }

    private companion object {
        val BAR_WIDTH = 300.dp
        val BAR_HEIGHT = 24.dp

        /** Room for anti-aliasing at the rounded cap, not for a different colour */
        const val TOLERANCE = 0.06f
    }
}
