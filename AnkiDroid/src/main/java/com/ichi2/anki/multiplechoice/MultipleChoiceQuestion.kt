/*
 *  Copyright (c) 2026 AnkiDroid Open Source Team
 *
 *  This program is free software; you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation; either version 3 of the License, or (at your option) any later
 *  version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Detection and parsing of "multiple choice" cards.
 *
 * A multiple choice card offers between 2 and [MultipleChoiceParser.MAX_OPTIONS] answers, exactly
 * one of which is correct. Anki has no first class support for such cards, so decks in the wild
 * encode them in one of two ways, both of which are supported here:
 *
 * **1. Dedicated note fields** (handled by [MultipleChoiceParser.parseFields])
 *
 * ```
 * Question: What is the capital of France?
 * A:        Berlin
 * B:        Madrid
 * C:        Paris
 * D:        Rome
 * Answer:   C
 * ```
 *
 * Field names are matched case-insensitively and a number of aliases are accepted, so
 * `A`, `a`, `Choice A`, `Option 1`, `Antwort B` and `opt_3` all denote an option, while
 * `Answer`, `Correct`, `Solution`, `Key`, `Richtig` or `Loesung` denote the correct one.
 * The correct answer may be given as a label (`C`, `3`, `(C)`, `C)`) or as the full text of
 * the correct option.
 *
 * **2. Options embedded in the rendered card text** (handled by [MultipleChoiceParser.parseText])
 *
 * ```
 * What is the capital of France?
 * A) Berlin
 * B) Madrid
 * C) Paris
 * ```
 *
 * with an answer side of (Anki renders the question again, followed by `<hr id=answer>`):
 *
 * ```
 * What is the capital of France?
 * A) Berlin
 * B) Madrid
 * C) Paris
 * C) Paris
 * ```
 *
 * Labels may be letters (`A`..`D`) or digits (`1`..`4`), may be preceded by a bullet and may be
 * followed by any of `)`, `.`, `:`, `-` or `]`. The labels must start at `A`/`1` and run in
 * order, which keeps false positives (such as a card which merely happens to contain a line
 * starting with "B.") to a minimum.
 *
 * This file deliberately contains no Android dependencies so that it can be unit tested on the JVM.
 */

package com.ichi2.anki.multiplechoice

/**
 * A single answer which the user may pick.
 *
 * @param label The label shown to the user: `"A"`, `"B"`, `"C"` or `"D"`
 * @param text The plain text of the option: HTML stripped and trimmed
 */
data class MultipleChoiceOption(
    val label: String,
    val text: String,
)

/**
 * A parsed multiple choice card.
 *
 * @param options The available answers, between 2 and [MultipleChoiceParser.MAX_OPTIONS] of them
 * @param correctIndex The index into [options] of the single correct answer
 */
data class MultipleChoiceQuestion(
    val options: List<MultipleChoiceOption>,
    val correctIndex: Int,
) {
    /** The correct answer */
    val correctOption: MultipleChoiceOption get() = options[correctIndex]
}

/** Parses [MultipleChoiceQuestion]s from note fields, or from rendered card text. */
object MultipleChoiceParser {
    /** The maximum number of options which the reviewer is able to display */
    const val MAX_OPTIONS = 4

    // region HTML handling

    /** `<script>`/`<style>` elements: their content is not user visible text */
    private val SCRIPT_OR_STYLE_ELEMENT =
        Regex(
            """<(script|style)\b[^>]*>.*?</\1\s*>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )

    /** Tags which visually introduce a line break, and are therefore converted to `\n` */
    private val LINE_BREAK_TAG = Regex("""<br\s*/?>|</\s*(?:div|p|li|tr)\s*>""", RegexOption.IGNORE_CASE)

    /** Any remaining tag: removed outright */
    private val ANY_TAG = Regex("""<[^>]*>""")

    /** `&#65;` or `&#x41;` */
    private val NUMERIC_ENTITY = Regex("""&#(x?)([0-9a-fA-F]+);""", RegexOption.IGNORE_CASE)

    /** Named entities which are common in decks. `&amp;` is handled last, see [stripHtml] */
    private val NAMED_ENTITIES =
        listOf(
            "&nbsp;" to " ",
            "&lt;" to "<",
            "&gt;" to ">",
            "&quot;" to "\"",
            "&#39;" to "'",
            "&apos;" to "'",
        )

    /** Runs of horizontal whitespace, including the non-breaking space */
    private val HORIZONTAL_WHITESPACE = Regex("""[ \t\u00A0]+""")

    /** Any whitespace, used to normalise text before comparing it */
    private val ANY_WHITESPACE = Regex("""\s+""")

    // endregion

    // region field names

    /**
     * A field which holds one of the options.
     *
     * Matches `a`, `4`, `choice a`, `option 1`, `antwort b`, `opt_3`, ...
     * (the name is lowercased and whitespace-normalised before matching).
     */
    private val OPTION_FIELD_NAME = Regex("""^(?:choice|option|answer|antwort|opt|auswahl)?\s*[_-]?\s*([a-d1-4])$""")

    /** Fields which hold the correct answer, either as a label or as the option's text */
    private val CORRECT_FIELD_NAMES =
        setOf(
            "answer",
            "correct",
            "correct answer",
            "correctanswer",
            "solution",
            "key",
            "richtig",
            "richtige antwort",
            "lösung",
            "losung",
            "loesung",
        )

    // endregion

    // region option/answer lines

    /**
     * An option inside the rendered card text: `A) Paris`, `(b). Rome`, `3 - Berlin`, ...
     *
     * A wider range of labels than [MAX_OPTIONS] allows is accepted on purpose: a card with 5
     * options must be detected so that it can be rejected, rather than silently truncated.
     */
    private val OPTION_LINE = Regex("""^\(?\s*([A-Ja-j1-9])\s*[).:\-\]]\s*(.+)$""")

    /** A leading bullet before an option label: `- A) foo`, `* B. bar` */
    private val LEADING_BULLET = Regex("""^[-*•·]\s*""")

    /** An answer side line which is nothing but a label: `C`, `(C)`, `3.` */
    private val LABEL_ONLY_LINE = Regex("""^\(?\s*([A-Da-d1-4])\s*[).:\-\]]?\s*$""")

    /** An answer side line which is a label followed by text */
    private val LABELLED_LINE = Regex("""^\(?\s*([A-Da-d1-4])\s*[).:\-\]]\s*(.+)$""")

    /** A value such as `C)`, `C.`, `C:` or `(C)` at the start of a correct-answer field */
    private val LEADING_LABEL = Regex("""^\(?\s*([A-Da-d1-4])\s*[).:\-\]]""")

    // endregion

    /**
     * Parses a multiple choice card, preferring dedicated note fields over the rendered text.
     *
     * @param fields The note's fields, keyed by field name
     * @param questionText The rendered question side (HTML or plain text)
     * @param answerText The rendered answer side (HTML or plain text)
     * @return the parsed card, or `null` if this is not a multiple choice card
     */
    fun parse(
        fields: Map<String, String>,
        questionText: String,
        answerText: String,
    ): MultipleChoiceQuestion? = parseFields(fields) ?: parseText(questionText, answerText)

    /**
     * Parses a multiple choice card from note fields, e.g. `Question`, `A`, `B`, `C`, `D`, `Answer`.
     *
     * @return the parsed card, or `null` if the fields do not describe a multiple choice card
     */
    fun parseFields(fields: Map<String, String>): MultipleChoiceQuestion? {
        val optionTexts = mutableMapOf<Int, String>()
        var correctValue: String? = null

        for ((rawName, rawValue) in fields) {
            val name = normalise(rawName)
            val value = stripHtml(rawValue)

            val optionMatch = OPTION_FIELD_NAME.find(name)
            if (optionMatch != null) {
                // an option field: an empty option is simply not offered to the user
                if (value.isBlank()) continue
                val index = labelToIndex(optionMatch.groupValues[1][0]) ?: continue
                // the first field to claim an index wins, e.g. 'A' beats a later 'Choice A'
                optionTexts.getOrPut(index) { value }
                continue
            }

            // 'Answer A' is an option, not the correct answer: we only get here if it wasn't one
            if (name in CORRECT_FIELD_NAMES && value.isNotBlank() && correctValue == null) {
                correctValue = value
            }
        }

        val options = toPrefixRun(optionTexts) ?: return null
        val correctIndex = findCorrectIndex(correctValue ?: return null, options) ?: return null
        return MultipleChoiceQuestion(options, correctIndex)
    }

    /**
     * Parses a multiple choice card from the rendered question and answer sides.
     *
     * @return the parsed card, or `null` if the text does not describe a multiple choice card
     */
    fun parseText(
        questionText: String,
        answerText: String,
    ): MultipleChoiceQuestion? {
        val question = stripHtml(questionText)

        // the labels have to start at A/1 and run in order, without gaps or repetition
        val labelled = question.lineSequence().mapNotNull(::matchOptionLine).toList()
        if (labelled.size < 2 || labelled.size > MAX_OPTIONS) return null
        if (labelled.withIndex().any { (position, option) -> option.first != position }) return null

        val options = labelled.map { (index, text) -> MultipleChoiceOption(indexToLabel(index), text) }
        val correctIndex = findCorrectIndexInAnswer(question, stripHtml(answerText), options) ?: return null
        return MultipleChoiceQuestion(options, correctIndex)
    }

    /**
     * Converts HTML to plain text: tags and entities are removed and whitespace is normalised.
     *
     * Tags which introduce a line break are converted to `\n` first, so that the result may be
     * parsed line by line.
     */
    fun stripHtml(html: String): String {
        var text = html.replace("\r\n", "\n").replace('\r', '\n')

        // the content of these elements is not visible to the user
        text = SCRIPT_OR_STYLE_ELEMENT.replace(text, "")
        // keep the visual line structure before the tags are thrown away
        text = LINE_BREAK_TAG.replace(text, "\n")
        text = ANY_TAG.replace(text, "")

        // entities: '&amp;' is decoded last so that '&amp;lt;' does not become '<'
        for ((entity, replacement) in NAMED_ENTITIES) {
            text = text.replace(entity, replacement, ignoreCase = true)
        }
        text =
            NUMERIC_ENTITY.replace(text) { match ->
                decodeNumericEntity(match.groupValues[1], match.groupValues[2]) ?: match.value
            }
        text = text.replace("&amp;", "&", ignoreCase = true)

        return text
            .lineSequence()
            .map { HORIZONTAL_WHITESPACE.replace(it, " ").trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
    }

    // region implementation

    /** Decodes the body of a numeric entity, or `null` if it is not a valid code point */
    private fun decodeNumericEntity(
        hexMarker: String,
        digits: String,
    ): String? {
        val codePoint = digits.toIntOrNull(if (hexMarker.isEmpty()) 10 else 16) ?: return null
        if (codePoint <= 0 || codePoint > Character.MAX_CODE_POINT) return null
        return String(Character.toChars(codePoint))
    }

    /** Lowercases, trims and collapses whitespace, for case-insensitive comparisons */
    private fun normalise(text: String): String = ANY_WHITESPACE.replace(text.trim(), " ").lowercase()

    /** `a`/`A`/`1` to `0`, `b`/`B`/`2` to `1`, ... or `null` if [label] is not a label */
    private fun labelToIndex(label: Char): Int? =
        when (label) {
            in 'a'..'j' -> label - 'a'
            in 'A'..'J' -> label - 'A'
            in '1'..'9' -> label - '1'
            else -> null
        }

    /** `0` to `"A"`, `1` to `"B"`, ... */
    private fun indexToLabel(index: Int): String = ('A' + index).toString()

    /**
     * Converts indexed option texts to a list, requiring that they form a run which starts at `A`:
     * `A+B`, `A+B+C` or `A+B+C+D`.
     *
     * @return the options, or `null` if there are gaps, or fewer than two options
     */
    private fun toPrefixRun(optionTexts: Map<Int, String>): List<MultipleChoiceOption>? {
        if (optionTexts.size < 2 || optionTexts.size > MAX_OPTIONS) return null
        return (0 until optionTexts.size).map { index ->
            val text = optionTexts[index] ?: return null
            MultipleChoiceOption(indexToLabel(index), text)
        }
    }

    /** Matches a line of the question text against [OPTION_LINE], returning `index to text` */
    private fun matchOptionLine(line: String): Pair<Int, String>? {
        val withoutBullet = LEADING_BULLET.replace(line.trim(), "")
        val match = OPTION_LINE.find(withoutBullet) ?: return null
        val index = labelToIndex(match.groupValues[1][0]) ?: return null
        val text = match.groupValues[2].trim()
        if (text.isEmpty()) return null
        return index to text
    }

    /**
     * Interprets the value of a correct-answer field, which may be a label (`C`, `3`, `(C)`) or
     * the text of the correct option.
     *
     * @return the index of the correct option, or `null` if the value could not be interpreted
     */
    private fun findCorrectIndex(
        value: String,
        options: List<MultipleChoiceOption>,
    ): Int? {
        // 1. a bare label, possibly decorated: 'C', '(C)', 'C.'
        val bare = value.trim().trim { it == '(' || it == ')' || it == '.' || it.isWhitespace() }
        if (bare.length == 1) {
            val index = labelToIndex(bare[0])
            if (index != null && index in options.indices) return index
        }

        // 2. the full text of one of the options
        indexOfOptionWithText(options, value)?.let { return it }

        // 3. a label followed by something else: 'C) Paris', 'C: Paris'
        val leading = LEADING_LABEL.find(value.trim())
        if (leading != null) {
            val index = labelToIndex(leading.groupValues[1][0])
            if (index != null && index in options.indices) return index
        }

        return null
    }

    /**
     * Finds the correct option on the answer side of a card.
     *
     * Anki renders the answer side as the question, `<hr id=answer>`, then the answer, so the
     * repeated question is removed before the remainder is searched.
     *
     * @return the index of the correct option, or `null` if the answer could not be interpreted
     */
    private fun findCorrectIndexInAnswer(
        strippedQuestion: String,
        strippedAnswer: String,
        options: List<MultipleChoiceOption>,
    ): Int? {
        val remainder =
            strippedAnswer
                .removePrefix(strippedQuestion)
                .trim()
                .ifBlank { strippedAnswer }

        val lines = remainder.lines().map { it.trim() }.filter { it.isNotEmpty() }

        // precise matches, line by line: the first line which identifies an option wins
        for (line in lines) {
            // 1. a line which is nothing but a label
            LABEL_ONLY_LINE.find(line)?.let { match ->
                val index = labelToIndex(match.groupValues[1][0])
                if (index != null && index in options.indices) return index
            }

            // 2. a label followed by text. The label alone identifies the option; if it is out of
            //    range the text may still match an option (e.g. '4) Paris' on a 3 option card)
            LABELLED_LINE.find(line)?.let { match ->
                val index = labelToIndex(match.groupValues[1][0])
                if (index != null && index in options.indices) return index
                indexOfOptionWithText(options, match.groupValues[2])?.let { return it }
            }

            // 3. a line which is exactly the text of one of the options
            indexOfOptionWithText(options, line)?.let { return it }
        }

        // 4. finally, a line which merely contains the text of exactly one option
        for (line in lines) {
            val normalisedLine = normalise(line)
            val matches =
                options.indices.filter { index ->
                    val text = normalise(options[index].text)
                    text.length >= 3 && normalisedLine.contains(text)
                }
            if (matches.size == 1) return matches.single()
        }

        return null
    }

    /** The index of the option whose text equals [text], ignoring case and whitespace */
    private fun indexOfOptionWithText(
        options: List<MultipleChoiceOption>,
        text: String,
    ): Int? {
        val normalisedText = normalise(text)
        val index = options.indexOfFirst { normalise(it.text) == normalisedText }
        return if (index == -1) null else index
    }

    // endregion
}
