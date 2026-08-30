package net.bueffel

import net.bueffel.domain.StudySession
import net.bueffel.model.Card
import net.bueffel.model.Deck
import net.bueffel.model.Question
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests [StudySession] */
class StudySessionTest {
    private fun deck(
        size: Int,
        box: Int = 0,
    ) = Deck(
        id = "d",
        name = "Deck",
        cards =
            (1..size).map {
                Card(
                    Question(prompt = "Frage $it", answers = listOf("eins", "zwei"), correctIndex = 0),
                    box = box,
                )
            },
    )

    @Test
    fun `eight correct answers in a row learn a question`() {
        val session = StudySession(deck(1))

        repeat(Card.LEARNED_BOX - 1) {
            session.answer(correct = true)
            assertEquals(0, session.learnedCount)
        }
        session.answer(correct = true)

        assertEquals(1, session.learnedCount)
        assertTrue(session.isFinished)
    }

    @Test
    fun `a wrong answer halves the progress rather than clearing it`() {
        val session = StudySession(deck(1))
        repeat(6) { session.answer(correct = true) }

        val card = session.answer(correct = false)

        assertEquals(3, card?.box)
    }

    @Test
    fun `halving rounds down, and one box away from nothing is nothing`() {
        val session = StudySession(deck(1))
        session.answer(correct = true)

        assertEquals(0, session.answer(correct = false)?.box)
    }

    @Test
    fun `a wrong answer near the end still leaves most of the work standing`() {
        val session = StudySession(deck(1, box = 7))

        assertEquals(3, session.answer(correct = false)?.box)
    }

    @Test
    fun `progress counts boxes, so every right answer moves it`() {
        val session = StudySession(deck(2))
        val before = session.progress

        session.answer(correct = true)

        assertTrue(session.progress > before)
        assertEquals(1f / (2 * Card.LEARNED_BOX), session.progress, 0.0001f)
    }

    @Test
    fun `a question never comes back immediately`() {
        val session = StudySession(deck(8))
        val first = session.current()

        session.answer(correct = true)

        assertNotEquals(first?.question?.prompt, session.current()?.question?.prompt)
    }

    @Test
    fun `other questions come in between before one returns`() {
        val session = StudySession(deck(12))
        val first = requireNotNull(session.current()).question.prompt

        session.answer(correct = true)
        var seenBefore = 0
        while (requireNotNull(session.current()).question.prompt != first) {
            seenBefore++
            session.answer(correct = true)
        }

        assertEquals(StudySession.GAPS[1], seenBefore)
    }

    @Test
    fun `four right in a row sends a question a full twenty back`() {
        // every card one answer away from the fourth box, so the first one crosses it here
        val session = StudySession(deck(30, box = 3))
        val first = requireNotNull(session.current()).question.prompt

        session.answer(correct = true)
        var seenBefore = 0
        while (requireNotNull(session.current()).question.prompt != first) {
            seenBefore++
            session.answer(correct = true)
        }

        assertEquals(20, seenBefore)
    }

    @Test
    fun `the wait grows with every box`() {
        assertEquals(Card.LEARNED_BOX, StudySession.GAPS.size)
        assertTrue(StudySession.GAPS.zipWithNext().all { (a, b) -> b > a })
    }

    @Test
    fun `undo takes back the last answer`() {
        val session = StudySession(deck(3))
        repeat(2) { session.answer(correct = true) }
        val before = session.progress

        session.answer(correct = true)
        assertTrue(session.undo())

        assertEquals(before, session.progress, 0.0001f)
    }

    @Test
    fun `there is nothing to undo before the first answer`() {
        val session = StudySession(deck(3))

        assertTrue(!session.canUndo)
        assertTrue(!session.undo())
    }

    @Test
    fun `a deck of learned questions is finished from the start`() {
        val session = StudySession(deck(3, box = Card.LEARNED_BOX))

        assertTrue(session.isFinished)
        assertNull(session.current())
        assertEquals(3, session.learnedCount)
    }

    @Test
    fun `the snapshot keeps every question`() {
        val session = StudySession(deck(5))
        repeat(9) { session.answer(correct = true) }

        assertEquals(5, session.snapshot().size)
    }

    @Test
    fun `answering an empty session does nothing`() {
        val session = StudySession(Deck("d", "Deck", emptyList()))

        assertNull(session.answer(correct = true))
    }
}
