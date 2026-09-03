package net.bueffel.model

import kotlin.math.ln

/**
 * A question together with how well it is known.
 *
 * [box] counts correct answers in a row. A wrong answer halves it rather than clearing it:
 * getting one question wrong on the eighth pass should not throw away seven passes of work, and
 * a learner who is nearly there stays nearly there.
 */
data class Card(
    val task: Task,
    val box: Int = 0,
    /** Marked by the learner as one they keep getting wrong; it then comes back twice as often */
    val hard: Boolean = false,
    /**
     * How many times a code task's lines have been put in the right order with nothing out of
     * place. Sorting is the easier half of writing code, so a card starts there and moves up to
     * writing it out once it has been sorted cleanly [SORTS_TO_WRITE] times.
     */
    val sorted: Int = 0,
) {
    /** How this card should be asked now */
    val mode: CardMode
        get() =
            when {
                task !is CodeTask -> CardMode.Choose
                sorted >= SORTS_TO_WRITE -> CardMode.Write
                else -> CardMode.Sort
            }

    /** Where this question sits on the run from not known to known, 0f..1f */
    val strength: Float get() = strengthOf(box)

    val isLearned: Boolean get() = box >= LEARNED_BOX

    /** One box up on a right answer; half the way back on a wrong one */
    fun answered(correct: Boolean): Card = copy(box = if (correct) (box + 1).coerceAtMost(LEARNED_BOX) else box / 2)

    companion object {
        /** Correct answers in a row before a question drops out of the rotation */
        const val LEARNED_BOX = 8

        /** Clean sorts before a code task stops being sorted and has to be written out */
        const val SORTS_TO_WRITE = 2
    }
}

/** How a card is being asked at the moment */
enum class CardMode {
    /** Pick one of several answers */
    Choose,

    /** Drag the model answer's lines into order */
    Sort,

    /** Type it out and mark it yourself */
    Write,
}

/**
 * How much one question is worth, 0f..1f, on a curve rather than a straight line.
 *
 * Counted straight, four right in a row is worth exactly half of eight - which is not how it
 * feels and not how it works. The first few passes are where a question goes from unknown to
 * roughly known; the last few only make it safe. So the curve is steep at the start and flat at
 * the end: four in a row reads as 60 %, six as 82 %, and the last two carry the remaining 18 %.
 *
 * That also means getting a whole set to four is worth far more than getting a handful to eight,
 * which is exactly the order the rounds ask for.
 */
fun strengthOf(box: Int): Float {
    val within = box.coerceIn(0, Card.LEARNED_BOX).toDouble() / Card.LEARNED_BOX
    return (ln(1 + CURVE * within) / ln(1 + CURVE)).toFloat()
}

/** How sharply the curve bends. Higher is steeper at the start; 1.25 puts four in a row at 60 %. */
private const val CURVE = 1.25

/**
 * How far a set of questions has come, 0f..1f.
 *
 * The average of what each question is worth, so it moves on every correct answer rather than
 * standing still until a question is finally done.
 */
fun progressOf(cards: List<Card>): Float {
    if (cards.isEmpty()) return 0f
    return cards.map { strengthOf(it.box) }.average().toFloat()
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
        // Each studied card is handed out once. Matching with a plain map keyed on the question
        // would keep only the last of any repeated one, so a question that appears in two parts
        // would get the same result written into both and one part's work would be lost.
        val waiting = cards.groupBy { it.task.prompt }.mapValues { ArrayDeque(it.value) }
        return copy(
            subtopics =
                subtopics.map { subtopic ->
                    subtopic.copy(
                        cards = subtopic.cards.map { waiting[it.task.prompt]?.removeFirstOrNull() ?: it },
                    )
                },
        )
    }
}
