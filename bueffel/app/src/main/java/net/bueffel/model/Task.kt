package net.bueffel.model

/**
 * What a card asks. Multiple choice was the only kind; writing code is the other one that
 * carries marks in an exam, so the card no longer assumes it holds a question with options.
 *
 * The front is two things rather than one: the task in prose, and whatever code is already
 * written on the card. They are kept apart because they are read differently - prose in the
 * text face, code in a monospaced one, where a `*` is a pointer and the indentation means
 * something.
 */
sealed interface Task {
    /** The task text shown on the front, in prose. May be empty when the card is only code. */
    val prompt: String

    /**
     * The code already on the front: a signature to fill in, or a program whose output is
     * asked for. Null when the card has none.
     */
    val given: String? get() = null

    /** Which part of the set this belongs to */
    val topic: String?

    /** Free labels: the exam it came from, the kind of exercise, whatever is worth filtering on */
    val tags: List<String>

    /**
     * What tells this card apart from the others.
     *
     * The prompt did that job alone until a card was allowed to be nothing but code: such a
     * card has no prompt, and two of them would look like the same card to anything matching on
     * it - which is how a finished session would write one card's progress into another.
     */
    val identity: String get() = prompt + "\n" + given.orEmpty()

    /** One line naming the card, for a list or a preview */
    val label: String get() = firstLine(prompt).ifEmpty { firstLine(given.orEmpty()) }
}

/** The first line with anything on it, trimmed; empty when there is none */
private fun firstLine(text: String): String = text.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()

/**
 * A multiple choice question.
 *
 * The answers are plain strings. There is no A/B/C/D label: the answer itself is written on the
 * box the reader taps, so a letter beside it would name something already in front of them.
 *
 * @param given a program whose output the answers offer, for the trace questions an exam asks
 *   with exactly three options
 * @param correctIndex index into [answers] of the single right one
 */
data class Question(
    override val prompt: String,
    val answers: List<String>,
    val correctIndex: Int,
    override val given: String? = null,
    override val topic: String? = null,
    override val tags: List<String> = emptyList(),
) : Task {
    val correctAnswer: String get() = answers[correctIndex]
}

/**
 * Write the code yourself.
 *
 * The front is the task and usually a signature or a body with [GAP] where the work goes, held
 * in [given]. The back is the model answer, kept as text with its line breaks: the lines are the
 * unit every mode works in - dragged into order in the easiest one, typed against on one line in
 * the shortest, compared line by line in the hardest.
 *
 * @param alternatives other model answers that count as right, for the same thing written
 *   differently
 */
data class CodeTask(
    override val prompt: String,
    val solution: String,
    override val given: String? = null,
    val alternatives: List<String> = emptyList(),
    override val topic: String? = null,
    override val tags: List<String> = emptyList(),
) : Task {
    /** The model answer as lines, with blank lines at either end dropped */
    val solutionLines: List<String> get() = lines(solution)

    /** Every accepted answer, the main one first */
    val accepted: List<String> get() = listOf(solution) + alternatives

    /** One line to write out, so the whole editor and its marking would be overkill */
    val isOneLiner: Boolean get() = solutionLines.size == 1

    // the solution belongs to the identity as well: two cards may ask the same thing of two
    // different functions, and the front alone would not tell them apart
    override val identity: String get() = prompt + "\n" + given.orEmpty() + "\n" + solution

    companion object {
        /** What a gap in the front looks like, so the task can say where the work goes */
        const val GAP = ">>> Hier fehlt was"

        fun lines(text: String): List<String> =
            text
                .replace("\r\n", "\n")
                .split("\n")
                .dropWhile { it.isBlank() }
                .dropLastWhile { it.isBlank() }
    }
}
