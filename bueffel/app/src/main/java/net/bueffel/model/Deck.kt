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
    /** Marked by the learner as one they keep getting wrong; it then comes back twice as often */
    val hard: Boolean = false,
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

/**
 * How far a set of questions has come, 0f..1f.
 *
 * Counts boxes rather than finished questions, so it moves on every correct answer instead of
 * standing still until a question is finally done.
 */
fun progressOf(cards: List<Card>): Float {
    if (cards.isEmpty()) return 0f
    return cards.sumOf { it.box }.toFloat() / (cards.size * Card.LEARNED_BOX)
}

/**
 * One part of a topic, learned and tracked on its own.
 *
 * A driving theory paper is not one subject but a dozen - signs, right of way, first aid - and
 * knowing you are through the signs while the priority rules are still red is the whole point of
 * splitting them. Each part carries its own progress; the topic's is all of them together.
 */
data class Subtopic(
    val id: String,
    val name: String,
    val cards: List<Card>,
) {
    val learnedCount: Int get() = cards.count { it.isLearned }

    val progress: Float get() = progressOf(cards)
}

/** A named topic, made of one or more subtopics */
data class Deck(
    val id: String,
    val name: String,
    val subtopics: List<Subtopic>,
) {
    val cards: List<Card> get() = subtopics.flatMap { it.cards }

    val learnedCount: Int get() = cards.count { it.isLearned }

    /**
     * The topic's progress, which is its subtopics' progress put together.
     *
     * Counted over every card rather than averaged over the subtopics, so a part with forty
     * questions weighs forty times as much as a part with one. Averaging the bars would let a
     * tiny finished part make a large unfinished one look done.
     */
    val progress: Float get() = progressOf(cards)

    /** Replaces the cards of one subtopic, leaving the rest of the topic alone */
    fun withCards(
        subtopicId: String,
        cards: List<Card>,
    ): Deck = copy(subtopics = subtopics.map { if (it.id == subtopicId) it.copy(cards = cards) else it })

    /**
     * Spreads a studied set back over the subtopics it came from.
     *
     * Studying a whole topic mixes every part together, so what comes back is one list; each card
     * is matched to its part by the question it carries.
     */
    fun withMixedCards(cards: List<Card>): Deck {
        val byPrompt = cards.associateBy { it.question.prompt }
        return copy(
            subtopics =
                subtopics.map { subtopic ->
                    subtopic.copy(cards = subtopic.cards.map { byPrompt[it.question.prompt] ?: it })
                },
        )
    }
}
