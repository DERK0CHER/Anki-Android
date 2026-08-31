package net.bueffel.model

/**
 * A multiple choice question.
 *
 * The answers are plain strings. There is no A/B/C/D label: the answer itself is written on the
 * pill the reader taps, so a letter beside it would name something already in front of them.
 *
 * @param correctIndex index into [answers] of the single right one
 * @param topic which part of the set this belongs to, as the source named it; null when the
 *   source did not say, in which case everything lands in one part
 */
data class Question(
    val prompt: String,
    val answers: List<String>,
    val correctIndex: Int,
    val topic: String? = null,
) {
    val correctAnswer: String get() = answers[correctIndex]
}
