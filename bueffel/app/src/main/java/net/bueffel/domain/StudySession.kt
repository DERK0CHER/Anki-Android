package net.bueffel.domain

import net.bueffel.model.Card
import net.bueffel.model.Deck

/**
 * The study rotation.
 *
 * A question leaves the rotation once it has been answered correctly [Card.LEARNED_BOX] times in
 * a row; a wrong answer sends it back to the first box. What keeps this bearable is the spacing:
 * a question that just came up is put back far enough down the queue that other questions are
 * seen before it returns, and the further along a question is, the further back it goes.
 *
 * This class holds no Android types, so the whole rotation is unit testable.
 */
class StudySession(
    deck: Deck,
) {
    private val queue: ArrayDeque<Card> = ArrayDeque(deck.cards.filterNot { it.isLearned })
    private val learned: MutableList<Card> = deck.cards.filter { it.isLearned }.toMutableList()

    /** Questions still in the rotation */
    val remaining: Int get() = queue.size

    /** Questions which reached the last box */
    val learnedCount: Int get() = learned.size

    /** The total this session started from, so progress can be shown as "done of total" */
    val total: Int = deck.cards.size

    val isFinished: Boolean get() = queue.isEmpty()

    /** The question to ask now, or `null` once everything is learned */
    fun current(): Card? = queue.firstOrNull()

    /**
     * Records an answer for the current question and moves it along.
     *
     * @return the card in its new state, or `null` if there was nothing to answer
     */
    fun answer(correct: Boolean): Card? {
        val card = queue.removeFirstOrNull() ?: return null
        val updated = card.answered(correct)
        if (updated.isLearned) {
            learned += updated
        } else {
            queue.add(gapFor(updated.box).coerceAtMost(queue.size), updated)
        }
        return updated
    }

    /** Everything the session knows about, for writing back to storage */
    fun snapshot(): List<Card> = queue.toList() + learned

    /**
     * How many other questions should come before this one returns.
     *
     * The gap grows with the box, so a question you are unsure about comes back soon and one you
     * have nearly learned waits a long time. It is never zero: the same question twice in a row
     * tests nothing but short term memory.
     */
    private fun gapFor(box: Int): Int = GAPS.getOrElse(box) { GAPS.last() }

    companion object {
        /** Questions to place in front of a returning question, indexed by its box */
        val GAPS = listOf(2, 5, 10, 20)
    }
}
