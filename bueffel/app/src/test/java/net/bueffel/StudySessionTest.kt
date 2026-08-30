package net.bueffel

import net.bueffel.domain.StudySession
import net.bueffel.model.Card
import net.bueffel.model.Choice
import net.bueffel.model.Deck
import net.bueffel.model.Question
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests [StudySession] */
class StudySessionTest {
    private fun deck(size: Int) =
        Deck(
            id = "d",
            name = "Deck",
            cards =
                (1..size).map {
                    Card(
                        Question(
                            prompt = "Frage $it",
                            choices = listOf(Choice("A", "eins"), Choice("B", "zwei")),
                            correctIndex = 0,
                        ),
                    )
                },
        )

    @Test
    fun `four correct answers in a row learn a question`() {
        val session = StudySession(deck(1))

        repeat(3) {
            session.answer(correct = true)
            assertEquals(0, session.learnedCount)
        }
        session.answer(correct = true)

        assertEquals(1, session.learnedCount)
        assertTrue(session.isFinished)
    }

    @Test
    fun `a wrong answer sends the question back to the first box`() {
        val session = StudySession(deck(1))
        repeat(3) { session.answer(correct = true) }

        val card = session.answer(correct = false)

        assertEquals(0, card?.box)
        assertEquals(0, session.learnedCount)
    }

    @Test
    fun `the streak has to be consecutive`() {
        val session = StudySession(deck(1))

        repeat(2) { session.answer(correct = true) }
        session.answer(correct = false)
        repeat(3) { session.answer(correct = true) }

        assertEquals(0, session.learnedCount)
        assertEquals(3, session.current()?.box)
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
        val session = StudySession(deck(8))
        val first = requireNonNull(session.current()).question.prompt

        session.answer(correct = true)
        val seenBefore = mutableListOf<String>()
        while (requireNonNull(session.current()).question.prompt != first) {
            seenBefore += requireNonNull(session.current()).question.prompt
            session.answer(correct = true)
        }

        assertEquals(StudySession.GAPS[1], seenBefore.size)
    }

    @Test
    fun `a question further along waits longer than a fresh one`() {
        assertTrue(StudySession.GAPS.zipWithNext().all { (a, b) -> b > a })
    }

    @Test
    fun `a deck of learned questions is finished from the start`() {
        val learned = deck(3).cards.map { it.copy(box = Card.LEARNED_BOX) }
        val session = StudySession(Deck("d", "Deck", learned))

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

    private fun <T> requireNonNull(value: T?): T = requireNotNull(value)
}
