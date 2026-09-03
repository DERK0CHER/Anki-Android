package net.bueffel.model

/**
 * What a card asks. Multiple choice was the only kind; writing code is the other one that
 * carries marks in an exam, so the card no longer assumes it holds a question with options.
 */
sealed interface Task {
    /** The task text shown on the front */
    val prompt: String

    /** Which part of the set this belongs to */
    val topic: String?

    /** Free labels: the exam it came from, the kind of exercise, whatever is worth filtering on */
    val tags: List<String>
}

/**
 * A multiple choice question.
 *
 * The answers are plain strings. There is no A/B/C/D label: the answer itself is written on the
 * box the reader taps, so a letter beside it would name something already in front of them.
 *
 * @param correctIndex index into [answers] of the single right one
 */
data class Question(
    override val prompt: String,
    val answers: List<String>,
    val correctIndex: Int,
    override val topic: String? = null,
    override val tags: List<String> = emptyList(),
) : Task {
    val correctAnswer: String get() = answers[correctIndex]
}

/**
 * Write the code yourself.
 *
 * The front is the task and usually a signature or a body with [GAP] where the work goes. The
 * back is the model answer, kept as text with its line breaks: the lines are the unit both modes
 * work in - dragged into order in the easier one, compared line by line in the harder one.
 *
 * @param alternatives other model answers that count as right, for the same thing written
 *   differently
 */
data class CodeTask(
    override val prompt: String,
    val solution: String,
    val alternatives: List<String> = emptyList(),
    override val topic: String? = null,
    override val tags: List<String> = emptyList(),
) : Task {
    /** The model answer as lines, with blank lines at either end dropped */
    val solutionLines: List<String> get() = lines(solution)

    /** Every accepted answer, the main one first */
    val accepted: List<String> get() = listOf(solution) + alternatives

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
