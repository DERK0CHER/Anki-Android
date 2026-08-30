package net.bueffel.model

/**
 * A question together with how well it is known.
 *
 * [box] counts correct answers in a row. A wrong answer halves it rather than clearing it:
 * getting one question wrong on the eighth pass should not throw away seven passes of work, and
 * a learner who is nearly there stays nearly there.
 */
data class Card(
    val question: Question,
    val box: Int = 0,
) {
    /** Where this question sits on the run from not known to known, 0f..1f */
    val strength: Float get() = box.toFloat() / LEARNED_BOX

    val isLearned: Boolean get() = box >= LEARNED_BOX

    /** One box up on a right answer; half the way back on a wrong one */
    fun answered(correct: Boolean): Card = copy(box = if (correct) (box + 1).coerceAtMost(LEARNED_BOX) else box / 2)

    companion object {
        /** Correct answers in a row before a question drops out of the rotation */
        const val LEARNED_BOX = 8
    }
}

/** A named set of questions */
data class Deck(
    val id: String,
    val name: String,
    val cards: List<Card>,
) {
    val learnedCount: Int get() = cards.count { it.isLearned }

    /**
     * How far through the deck the learner is, 0f..1f.
     *
     * Counts boxes rather than finished questions, so it moves on every correct answer instead
     * of standing still until a question is finally done.
     */
    val progress: Float
        get() {
            if (cards.isEmpty()) return 0f
            return cards.sumOf { it.box }.toFloat() / (cards.size * Card.LEARNED_BOX)
        }
}
