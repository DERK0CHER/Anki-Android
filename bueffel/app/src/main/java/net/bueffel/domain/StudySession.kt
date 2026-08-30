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

    /** The queue and the learned pile as they were before the last answer, for [undo] */
    private var previous: Pair<List<Card>, List<Card>>? = null

    /** Questions still in the rotation */
    val remaining: Int get() = queue.size

    /** Questions which reached the last box */
    val learnedCount: Int get() = learned.size

    /** The total this session started from, so progress can be shown as "done of total" */
    val total: Int = deck.cards.size

    val isFinished: Boolean get() = queue.isEmpty()

    /**
     * How much of the work is done, 0f..1f.
     *
     * Counts boxes rather than finished questions: a set of forty needs a hundred and sixty
     * right answers, so "questions learned" sits at zero for a long while and tells the learner
     * nothing. Boxes move on every single correct answer.
     */
    val progress: Float
        get() {
            if (total == 0) return 1f
            val filled = (queue + learned).sumOf { it.box }
            return (filled.toFloat() / (total * Card.LEARNED_BOX)).coerceIn(0f, 1f)
        }

    /** Whether the last answer can still be taken back */
    val canUndo: Boolean get() = previous != null

    /** The question to ask now, or `null` once everything is learned */
    fun current(): Card? = queue.firstOrNull()

    /**
     * Records an answer for the current question and moves it along.
     *
     * @return the card in its new state, or `null` if there was nothing to answer
     */
    fun answer(correct: Boolean): Card? {
        if (queue.isEmpty()) return null
        previous = queue.toList() to learned.toList()
        val card = queue.removeFirst()
        val updated = card.answered(correct)
        if (updated.isLearned) {
            learned += updated
        } else {
            queue.add(gapFor(updated.box).coerceAtMost(queue.size), updated)
        }
        return updated
    }

    /**
     * Takes back the last answer, for the mis-tap that every list of buttons produces.
     *
     * @return true if there was something to take back
     */
    fun undo(): Boolean {
        val (queueBefore, learnedBefore) = previous ?: return false
        queue.clear()
        queue.addAll(queueBefore)
        learned.clear()
        learned.addAll(learnedBefore)
        previous = null
        return true
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
        /**
         * Questions to place in front of a returning question, indexed by its box.
         *
         * One entry per box, so the wait grows all the way to the last one: a question seen
         * seven times running should not come back as quickly as one seen twice.
         */
        val GAPS = listOf(2, 3, 4, 6, 8, 11, 15, 20)
    }
}
