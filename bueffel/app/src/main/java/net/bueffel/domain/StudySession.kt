package net.bueffel.domain

import net.bueffel.model.Card

/**
 * The study rotation.
 *
 * A question leaves the rotation once it has been answered correctly [Card.LEARNED_BOX] times in
 * a row; a wrong answer halves its count. What keeps this bearable is the spacing: a question
 * that just came up is put back far enough down the queue that other questions are seen before
 * it returns, and the further along a question is, the further back it goes.
 *
 * Only [WORKING_SET] questions are in that rotation at a time. Drilling a whole set at once
 * teaches nothing - the gaps are meaningless when there are two hundred questions between every
 * repeat - so the rest wait in reserve and are mixed in as the session goes.
 *
 * This class holds no Android types, so the whole rotation is unit testable.
 */
class StudySession(
    cards: List<Card>,
) {
    /** In rotation now */
    private val queue: ArrayDeque<Card> = ArrayDeque()

    /** Not yet asked in this session, waiting to be mixed in */
    private val reserve: ArrayDeque<Card> = ArrayDeque()

    private val learned: MutableList<Card> = mutableListOf()

    /** Answers given since the last new question came in */
    private var sinceNew = 0

    /** Everything as it was before the last answer, for [undo] */
    private var previous: State? = null

    private data class State(
        val queue: List<Card>,
        val reserve: List<Card>,
        val learned: List<Card>,
        val sinceNew: Int,
    )

    init {
        for (card in cards) {
            when {
                card.isLearned -> learned += card
                // one already under way goes straight into the rotation; a fresh one waits
                card.box > 0 -> queue.addLast(card)
                else -> reserve.addLast(card)
            }
        }
        topUp()
    }

    /** Questions not yet learned, whether in the rotation or still in reserve */
    val remaining: Int get() = queue.size + reserve.size

    val learnedCount: Int get() = learned.size

    /** The total this session started from, so progress can be shown as "done of total" */
    val total: Int = cards.size

    val isFinished: Boolean get() = queue.isEmpty() && reserve.isEmpty()

    /**
     * How much of the work is done, 0f..1f.
     *
     * Counts boxes rather than finished questions: a set of forty needs three hundred and twenty
     * right answers, so "questions learned" sits at zero for a long while and tells the learner
     * nothing. Boxes move on every single correct answer.
     */
    val progress: Float
        get() {
            if (total == 0) return 1f
            val filled = snapshot().sumOf { it.box }
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
        val card = queue.removeFirstOrNull() ?: return null
        previous = state().copy(queue = listOf(card) + queue.toList())
        val updated = card.answered(correct)
        if (updated.isLearned) {
            learned += updated
        } else {
            queue.add(gapFor(updated).coerceAtMost(queue.size), updated)
        }
        sinceNew++
        mixInNew()
        topUp()
        return updated
    }

    /** Marks the current question as one the learner keeps getting wrong, or unmarks it */
    fun flag(hard: Boolean): Card? {
        val card = queue.removeFirstOrNull() ?: return null
        val updated = card.copy(hard = hard)
        queue.addFirst(updated)
        return updated
    }

    /**
     * Takes back the last answer, for the mis-tap that every list of buttons produces.
     *
     * @return true if there was something to take back
     */
    fun undo(): Boolean {
        val before = previous ?: return false
        queue.clear()
        queue.addAll(before.queue)
        reserve.clear()
        reserve.addAll(before.reserve)
        learned.clear()
        learned.addAll(before.learned)
        sinceNew = before.sinceNew
        previous = null
        return true
    }

    /** Everything the session knows about, for writing back to storage */
    fun snapshot(): List<Card> = queue.toList() + reserve.toList() + learned

    private fun state() = State(queue.toList(), reserve.toList(), learned.toList(), sinceNew)

    /** Keeps the rotation full while there is anything left in reserve */
    private fun topUp() {
        while (reserve.isNotEmpty() && queue.size < WORKING_SET) {
            queue.addLast(reserve.removeFirst())
        }
    }

    /**
     * Brings a new question in every so often, whether or not the rotation has room.
     *
     * Without this a session is the same handful of questions until one of them is finally
     * learned, which is what makes a large set feel like a short one on repeat.
     */
    private fun mixInNew() {
        if (reserve.isEmpty() || sinceNew < NEW_EVERY) return
        queue.add(NEW_POSITION.coerceAtMost(queue.size), reserve.removeFirst())
        sinceNew = 0
    }

    /**
     * How many other questions should come before this one returns.
     *
     * The gap grows with the box, so a question you are unsure about comes back soon and one you
     * have nearly learned waits a long time. One marked hard comes back twice as often as its
     * box would say. It is never zero: the same question twice running tests nothing but short
     * term memory.
     */
    private fun gapFor(card: Card): Int {
        val gap = GAPS.getOrElse(card.box) { GAPS.last() }
        return if (card.hard) (gap / 2).coerceAtLeast(GAPS.first()) else gap
    }

    companion object {
        /**
         * Questions to place in front of a returning question, indexed by its box.
         *
         * One entry per box, so the wait grows all the way to the last one. Four right in a row
         * is the point where a question stops being shaky, so from there it goes a full twenty
         * back rather than the eight it used to.
         */
        val GAPS = listOf(2, 4, 8, 13, 20, 28, 38, 50)

        /** Questions in the rotation at once. More than this and the gaps stop meaning anything. */
        const val WORKING_SET = 12

        /** Answers between one new question coming in and the next */
        const val NEW_EVERY = 4

        /** Where a new question lands: soon, but not under a finger that is already moving */
        const val NEW_POSITION = 1
    }
}
