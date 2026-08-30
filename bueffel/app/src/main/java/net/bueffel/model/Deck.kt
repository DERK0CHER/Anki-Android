package net.bueffel.model

/**
 * A question together with how well it is known.
 *
 * [box] is the Leitner box it currently sits in: 0 is "just got it wrong or never seen",
 * [LEARNED_BOX] means it has been answered correctly that many times running and is done.
 */
data class Card(
    val question: Question,
    val box: Int = 0,
) {
    val isLearned: Boolean get() = box >= LEARNED_BOX

    /** Moves the card one box up on a right answer, and all the way back on a wrong one */
    fun answered(correct: Boolean): Card = copy(box = if (correct) (box + 1).coerceAtMost(LEARNED_BOX) else 0)

    companion object {
        /** Correct answers in a row before a question drops out of the rotation */
        const val LEARNED_BOX = 4
    }
}

/** A named set of questions */
data class Deck(
    val id: String,
    val name: String,
    val cards: List<Card>,
) {
    val learnedCount: Int get() = cards.count { it.isLearned }

    /** How far through the deck the learner is, 0f..1f */
    val progress: Float get() = if (cards.isEmpty()) 0f else learnedCount.toFloat() / cards.size
}
