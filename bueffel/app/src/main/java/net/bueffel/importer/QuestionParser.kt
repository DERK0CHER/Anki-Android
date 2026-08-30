package net.bueffel.importer

import net.bueffel.model.Choice
import net.bueffel.model.Question

/**
 * Reads multiple choice questions out of pasted text.
 *
 * The text is expected to have come from a chat with a language model, so the parser is
 * deliberately forgiving: blocks may be separated by blank lines or by a rule of dashes, the
 * question may or may not be prefixed, options may be lettered or numbered, and the solution may
 * be given as a letter, a number or the full text of the right option.
 *
 * A block that cannot be read is skipped rather than failing the whole paste, and reported in
 * [ImportResult.skipped] so the count is honest.
 */
object QuestionParser {
    /** `A) text`, `1. text`, `- b: text`, `(C) text` */
    private val OPTION_LINE = Regex("""^[-*•]?\s*\(?\s*([A-Ja-j1-9])\s*[).:\-\]]\s*(.+)$""")

    /** A line naming the right answer */
    private val SOLUTION_LINE =
        Regex(
            """^\s*(?:lösung|loesung|antwort|richtig(?:e antwort)?|solution|answer|correct)\s*[:\-]\s*(.+)$""",
            RegexOption.IGNORE_CASE,
        )

    /** An optional prefix on the question itself */
    private val QUESTION_PREFIX = Regex("""^\s*(?:frage|question|q)\s*\d*\s*[:.\-]\s*""", RegexOption.IGNORE_CASE)

    /** A numbered question, e.g. `3. Was bedeutet ...` */
    private val NUMBERED_QUESTION = Regex("""^\s*\d+\s*[).:]\s+(.+)$""")

    /** A rule some models put between questions */
    private val RULE = Regex("""^\s*([-=_*])\1{2,}\s*$""")

    data class ImportResult(
        val questions: List<Question>,
        val skipped: Int,
    )

    /** Splits [text] into blocks and reads each one */
    fun parse(text: String): ImportResult {
        val blocks = splitIntoBlocks(text)
        val questions = mutableListOf<Question>()
        var skipped = 0
        for (block in blocks) {
            val question = parseBlock(block)
            if (question != null) questions += question else skipped++
        }
        return ImportResult(questions, skipped)
    }

    /** Blocks are separated by a blank line or by a rule of dashes */
    private fun splitIntoBlocks(text: String): List<List<String>> {
        val blocks = mutableListOf<List<String>>()
        var current = mutableListOf<String>()
        for (raw in text.replace("\r\n", "\n").replace('\r', '\n').split("\n")) {
            val line = raw.trim()
            if (line.isEmpty() || RULE.matches(line)) {
                if (current.isNotEmpty()) {
                    blocks += current
                    current = mutableListOf()
                }
            } else {
                current += line
            }
        }
        if (current.isNotEmpty()) blocks += current
        return blocks
    }

    /** Reads one block: a question line, its options, and the solution */
    private fun parseBlock(lines: List<String>): Question? {
        val prompt = StringBuilder()
        val options = mutableListOf<Pair<Int, String>>()
        var solution: String? = null

        for (line in lines) {
            val solutionMatch = SOLUTION_LINE.find(line)
            if (solutionMatch != null) {
                if (solution == null) solution = solutionMatch.groupValues[1].trim()
                continue
            }

            val option = OPTION_LINE.find(line)
            // an option line only counts once the question has been read, otherwise a numbered
            // question such as "1. Was gilt hier?" would be mistaken for the first option
            if (option != null && prompt.isNotEmpty()) {
                val index = labelToIndex(option.groupValues[1][0])
                if (index != null) {
                    options += index to option.groupValues[2].trim()
                    continue
                }
            }
            if (options.isEmpty()) {
                val cleaned =
                    NUMBERED_QUESTION.find(line)?.groupValues?.get(1)
                        ?: QUESTION_PREFIX.replace(line, "")
                if (prompt.isNotEmpty()) prompt.append(' ')
                prompt.append(cleaned.trim())
            }
        }

        val choices = toChoices(options) ?: return null
        val text = prompt.toString().trim()
        if (text.isEmpty()) return null
        val correctIndex = resolveSolution(solution, choices) ?: return null
        return Question(prompt = text, choices = choices, correctIndex = correctIndex)
    }

    /** Requires 2..4 options, running in order from the first */
    private fun toChoices(options: List<Pair<Int, String>>): List<Choice>? {
        if (options.size < 2 || options.size > MAX_CHOICES) return null
        if (options.withIndex().any { (position, option) -> option.first != position }) return null
        if (options.any { it.second.isBlank() }) return null
        return options.mapIndexed { index, option -> Choice(indexToLabel(index), option.second) }
    }

    /** The solution may be a label, or the text of the right option */
    private fun resolveSolution(
        solution: String?,
        choices: List<Choice>,
    ): Int? {
        val value = solution?.trim()?.takeIf { it.isNotEmpty() } ?: return null

        val bare = value.trim { it == '(' || it == ')' || it == '.' || it.isWhitespace() }
        if (bare.length == 1) {
            labelToIndex(bare[0])?.let { if (it in choices.indices) return it }
        }

        val normalised = value.lowercase().replace(WHITESPACE, " ").trim()
        choices
            .indexOfFirst {
                it.text
                    .lowercase()
                    .replace(WHITESPACE, " ")
                    .trim() == normalised
            }.takeIf { it >= 0 }
            ?.let { return it }

        // "C) Paris" - the label leads, the text follows
        OPTION_LINE.find(value)?.let { match ->
            labelToIndex(match.groupValues[1][0])?.let { if (it in choices.indices) return it }
        }
        return null
    }

    private val WHITESPACE = Regex("""\s+""")

    private fun labelToIndex(label: Char): Int? =
        when (label) {
            in 'a'..'j' -> label - 'a'
            in 'A'..'J' -> label - 'A'
            in '1'..'9' -> label - '1'
            else -> null
        }

    private fun indexToLabel(index: Int): String = ('A' + index).toString()

    /** The study screen shows one box per option, and four is where that stops being readable */
    const val MAX_CHOICES = 4
}
