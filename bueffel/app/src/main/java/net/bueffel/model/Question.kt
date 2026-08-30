package net.bueffel.model

/** One answer a question offers */
data class Choice(
    val label: String,
    val text: String,
)

/**
 * A multiple choice question.
 *
 * @param correctIndex index into [choices] of the single right answer
 */
data class Question(
    val prompt: String,
    val choices: List<Choice>,
    val correctIndex: Int,
) {
    val correctChoice: Choice get() = choices[correctIndex]
}
