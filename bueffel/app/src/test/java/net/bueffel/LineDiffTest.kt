package net.bueffel

import net.bueffel.domain.LineChange
import net.bueffel.domain.LineDiff
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests lining up typed code against the model answer */
class LineDiffTest {
    private fun kinds(
        mine: String,
        theirs: String,
    ) = LineDiff.compare(mine.lines(), theirs.lines()).map { it.change }

    @Test
    fun `the same code is all the same`() {
        val code = "int a = 1;\nreturn a;"

        assertTrue(kinds(code, code).all { it == LineChange.Same })
    }

    @Test
    fun `indentation is not what is being tested`() {
        val kinds = kinds("if (a) {\n    return 1;\n}", "if (a) {\nreturn 1;\n}")

        assertTrue(kinds.all { it == LineChange.Same })
    }

    @Test
    fun `one forgotten line costs one row, not every row after it`() {
        // an index by index comparison would call all three of the last lines wrong
        val kinds = kinds("a();\nc();\nd();", "a();\nb();\nc();\nd();")

        assertEquals(
            listOf(LineChange.Same, LineChange.Missing, LineChange.Same, LineChange.Same),
            kinds,
        )
    }

    @Test
    fun `a line too many is reported as extra`() {
        val kinds = kinds("a();\nb();\nc();", "a();\nc();")

        assertEquals(listOf(LineChange.Same, LineChange.Extra, LineChange.Same), kinds)
    }

    @Test
    fun `a changed line reads as one gone and one arrived`() {
        val rows = LineDiff.compare(listOf("return 1;"), listOf("return 0;"))

        assertEquals(listOf(LineChange.Extra, LineChange.Missing), rows.map { it.change })
        assertEquals("return 1;", rows[0].mine)
        assertEquals("return 0;", rows[1].theirs)
    }

    @Test
    fun `an empty answer is the whole model answer missing`() {
        val rows = LineDiff.compare(emptyList(), listOf("a();", "b();"))

        assertEquals(listOf(LineChange.Missing, LineChange.Missing), rows.map { it.change })
    }

    @Test
    fun `same ignores indentation and blank lines`() {
        assertTrue(LineDiff.same(listOf("  a();", "", "b();"), listOf("a();", "b();")))
        assertTrue(!LineDiff.same(listOf("a();"), listOf("b();")))
    }

    @Test
    fun `the line as typed is what comes back, not the trimmed one`() {
        val rows = LineDiff.compare(listOf("    return 1;"), listOf("return 1;"))

        assertEquals("    return 1;", rows.single().mine)
    }
}
