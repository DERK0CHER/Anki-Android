package net.bueffel.model

/**
 * A multiple choice question.
 *
 * The answers are plain strings. There is no A/B/C/D label: the answer itself is written on the
 * pill the reader taps, so a letter beside it would name something already in front of them.
 *
 * @param correctIndex index into [answers] of the single right one
 */
data class Question(
    val prompt: String,
    val answers: List<String>,
    val correctIndex: Int,
) {
    val correctAnswer: String get() = answers[correctIndex]
}
