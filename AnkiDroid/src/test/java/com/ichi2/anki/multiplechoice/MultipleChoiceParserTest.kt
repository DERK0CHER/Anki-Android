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

package com.ichi2.anki.multiplechoice

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

/** Tests [MultipleChoiceParser] */
class MultipleChoiceParserTest {
    // region fields

    @Test
    fun `fields with a label as the answer`() {
        val question =
            MultipleChoiceParser.parseFields(
                mapOf(
                    "Question" to "What is the capital of France?",
                    "A" to "Berlin",
                    "B" to "Madrid",
                    "C" to "Paris",
                    "D" to "Rome",
                    "Answer" to "C",
                ),
            )

        assertOptions(question, "Berlin", "Madrid", "Paris", "Rome")
        assertCorrect(question, "C", "Paris")
    }

    @Test
    fun `fields with the full option text as the answer`() {
        val question =
            MultipleChoiceParser.parseFields(
                mapOf(
                    "Question" to "What is the capital of France?",
                    "A" to "Berlin",
                    "B" to "Madrid",
                    "C" to "Paris",
                    "Answer" to "paris",
                ),
            )

        assertOptions(question, "Berlin", "Madrid", "Paris")
        assertCorrect(question, "C", "Paris")
    }

    @Test
    fun `fields with a decorated label as the answer`() {
        val question =
            MultipleChoiceParser.parseFields(
                mapOf(
                    "A" to "Berlin",
                    "B" to "Madrid",
                    "C" to "Paris",
                    "Correct Answer" to "(C)",
                ),
            )

        assertCorrect(question, "C", "Paris")
    }

    @Test
    fun `fields with a label and text as the answer`() {
        val question =
            MultipleChoiceParser.parseFields(
                mapOf(
                    "A" to "Berlin",
                    "B" to "Madrid",
                    "C" to "Paris",
                    "Solution" to "C) Paris",
                ),
            )

        assertCorrect(question, "C", "Paris")
    }

    @Test
    fun `fields with only two options`() {
        val question =
            MultipleChoiceParser.parseFields(
                mapOf(
                    "Question" to "AnkiDroid is free software",
                    "A" to "True",
                    "B" to "False",
                    "Answer" to "A",
                ),
            )

        assertOptions(question, "True", "False")
        assertCorrect(question, "A", "True")
    }

    @Test
    fun `fields with numbered option names`() {
        val question =
            MultipleChoiceParser.parseFields(
                mapOf(
                    "Option 1" to "Berlin",
                    "Option 2" to "Madrid",
                    "opt_3" to "Paris",
                    "Key" to "2",
                ),
            )

        assertOptions(question, "Berlin", "Madrid", "Paris")
        assertCorrect(question, "B", "Madrid")
    }

    @Test
    fun `german field names`() {
        val question =
            MultipleChoiceParser.parseFields(
                mapOf(
                    "Frage" to "Was ist die Hauptstadt von Deutschland?",
                    "Antwort A" to "Berlin",
                    "Antwort B" to "Hamburg",
                    "Antwort C" to "Muenchen",
                    "Lösung" to "A",
                ),
            )

        assertOptions(question, "Berlin", "Hamburg", "Muenchen")
        assertCorrect(question, "A", "Berlin")
    }

    @Test
    fun `an option field named 'Answer A' is not the correct-answer field`() {
        val question =
            MultipleChoiceParser.parseFields(
                mapOf(
                    "Answer A" to "Berlin",
                    "Answer B" to "Paris",
                    "Answer" to "B",
                ),
            )

        assertOptions(question, "Berlin", "Paris")
        assertCorrect(question, "B", "Paris")
    }

    @Test
    fun `html is stripped from option fields`() {
        val question =
            MultipleChoiceParser.parseFields(
                mapOf(
                    "A" to "<b>Berlin</b>",
                    "B" to "Paris &amp; Rome",
                    "Answer" to "B",
                ),
            )

        assertOptions(question, "Berlin", "Paris & Rome")
        assertCorrect(question, "B", "Paris & Rome")
    }

    @Test
    fun `fields without a correct-answer field are not multiple choice`() {
        val question =
            MultipleChoiceParser.parseFields(
                mapOf(
                    "Question" to "What is the capital of France?",
                    "A" to "Berlin",
                    "B" to "Paris",
                ),
            )

        assertThat(question, nullValue())
    }

    @Test
    fun `fields with an uninterpretable answer are not multiple choice`() {
        val question =
            MultipleChoiceParser.parseFields(
                mapOf(
                    "A" to "Berlin",
                    "B" to "Paris",
                    "Answer" to "Madrid",
                ),
            )

        assertThat(question, nullValue())
    }

    @Test
    fun `option fields must start at A and have no gaps`() {
        val question =
            MultipleChoiceParser.parseFields(
                mapOf(
                    "A" to "Berlin",
                    "C" to "Paris",
                    "Answer" to "A",
                ),
            )

        assertThat(question, nullValue())
    }

    @Test
    fun `a single option is not multiple choice`() {
        val question =
            MultipleChoiceParser.parseFields(
                mapOf(
                    "A" to "Berlin",
                    "Answer" to "A",
                ),
            )

        assertThat(question, nullValue())
    }

    @Test
    fun `a plain question and answer note is not multiple choice`() {
        val question =
            MultipleChoiceParser.parseFields(
                mapOf(
                    "Front" to "What is the capital of France?",
                    "Back" to "Paris",
                ),
            )

        assertThat(question, nullValue())
    }

    // endregion

    // region text

    @Test
    fun `text with the question repeated on the answer side`() {
        val questionText =
            """
            What is the capital of France?
            A) Berlin
            B) Madrid
            C) Paris
            """.trimIndent()
        val answerText = "$questionText\nC) Paris"

        val question = MultipleChoiceParser.parseText(questionText, answerText)

        assertOptions(question, "Berlin", "Madrid", "Paris")
        assertCorrect(question, "C", "Paris")
    }

    @Test
    fun `text with a bare label as the answer`() {
        val questionText =
            """
            What is the capital of France?
            A) Berlin
            B) Madrid
            C) Paris
            """.trimIndent()

        val question = MultipleChoiceParser.parseText(questionText, "$questionText\nC")

        assertCorrect(question, "C", "Paris")
    }

    @Test
    fun `text with the option text as the answer`() {
        val questionText =
            """
            What is the capital of France?
            A) Berlin
            B) Madrid
            C) Paris
            """.trimIndent()

        val question = MultipleChoiceParser.parseText(questionText, "$questionText\nThe answer is Paris.")

        assertCorrect(question, "C", "Paris")
    }

    @Test
    fun `text with numeric labels`() {
        val questionText =
            """
            Pick the second letter
            1) Alpha
            2) Beta
            3) Gamma
            """.trimIndent()

        val question = MultipleChoiceParser.parseText(questionText, "2")

        assertOptions(question, "Alpha", "Beta", "Gamma")
        assertCorrect(question, "B", "Beta")
    }

    @Test
    fun `text with bulleted options`() {
        val question = MultipleChoiceParser.parseText("Pick one\n- A) foo\n- B) bar", "B")

        assertOptions(question, "foo", "bar")
        assertCorrect(question, "B", "bar")
    }

    @Test
    fun `html with br separators`() {
        val questionText = "What is 2 + 2?<br>A) 3<br>B) 4<br>C) 5"
        val answerText = "$questionText<br>B) 4"

        val question = MultipleChoiceParser.parseText(questionText, answerText)

        assertOptions(question, "3", "4", "5")
        assertCorrect(question, "B", "4")
    }

    @Test
    fun `a plain question and answer card is not multiple choice`() {
        val question = MultipleChoiceParser.parseText("What is the capital of France?", "Paris")

        assertThat(question, nullValue())
    }

    @Test
    fun `five options are not supported`() {
        val questionText =
            """
            Pick one
            A) one
            B) two
            C) three
            D) four
            E) five
            """.trimIndent()

        val question = MultipleChoiceParser.parseText(questionText, "$questionText\nE) five")

        assertThat(question, nullValue())
    }

    @Test
    fun `options which do not start at A are ignored`() {
        val question = MultipleChoiceParser.parseText("Pick one\nB) foo\nC) bar", "B")

        assertThat(question, nullValue())
    }

    @Test
    fun `text without an identifiable answer is not multiple choice`() {
        val questionText =
            """
            What is the capital of France?
            A) Berlin
            B) Madrid
            C) Paris
            """.trimIndent()

        val question = MultipleChoiceParser.parseText(questionText, "$questionText\nnone of these")

        assertThat(question, nullValue())
    }

    // endregion

    // region parse

    @Test
    fun `parse prefers the note fields`() {
        val question =
            MultipleChoiceParser.parse(
                fields = mapOf("A" to "Berlin", "B" to "Paris", "Answer" to "B"),
                questionText = "Pick one\nA) foo\nB) bar\nC) baz",
                answerText = "C",
            )

        assertOptions(question, "Berlin", "Paris")
        assertCorrect(question, "B", "Paris")
    }

    @Test
    fun `parse falls back to the card text`() {
        val questionText = "Pick one\nA) foo\nB) bar\nC) baz"

        val question =
            MultipleChoiceParser.parse(
                fields = mapOf("Front" to questionText, "Back" to "C) baz"),
                questionText = questionText,
                answerText = "$questionText\nC) baz",
            )

        assertOptions(question, "foo", "bar", "baz")
        assertCorrect(question, "C", "baz")
    }

    @Test
    fun `parse returns null for a card which is not multiple choice`() {
        val question =
            MultipleChoiceParser.parse(
                fields = mapOf("Front" to "What is the capital of France?", "Back" to "Paris"),
                questionText = "What is the capital of France?",
                answerText = "What is the capital of France?\nParis",
            )

        assertThat(question, nullValue())
    }

    // endregion

    // region stripHtml

    @Test
    fun `stripHtml removes tags and decodes entities`() {
        assertThat(
            MultipleChoiceParser.stripHtml("<div>Hello &amp; <b>world</b></div>"),
            equalTo("Hello & world"),
        )
        assertThat(
            MultipleChoiceParser.stripHtml("&lt;tag&gt; &quot;quoted&quot; &#39;single&#39; &#65;&#x42;"),
            equalTo("""<tag> "quoted" 'single' AB"""),
        )
    }

    @Test
    fun `stripHtml removes script and style content`() {
        assertThat(
            MultipleChoiceParser.stripHtml("<style>.card { color: red }</style><script>var x = 1;</script>Text"),
            equalTo("Text"),
        )
    }

    @Test
    fun `stripHtml converts line breaking tags to newlines`() {
        assertThat(MultipleChoiceParser.stripHtml("one<br>two<br/>three<br />four"), equalTo("one\ntwo\nthree\nfour"))
        assertThat(MultipleChoiceParser.stripHtml("<ul><li>one</li><li>two</li></ul>"), equalTo("one\ntwo"))
        assertThat(MultipleChoiceParser.stripHtml("<p>one</p><div>two</div>"), equalTo("one\ntwo"))
    }

    @Test
    fun `stripHtml normalises whitespace`() {
        assertThat(MultipleChoiceParser.stripHtml("  a  \t b<br>   <p>c</p>  "), equalTo("a b\nc"))
        assertThat(MultipleChoiceParser.stripHtml("a&nbsp;&nbsp;b"), equalTo("a b"))
    }

    // endregion

    // region helpers

    /** Asserts that [question] was parsed, and has options labelled `A`, `B`, ... with [texts] */
    private fun assertOptions(
        question: MultipleChoiceQuestion?,
        vararg texts: String,
    ) {
        assertThat("expected a multiple choice question", question, notNullValue())
        val options = question!!.options
        assertThat(options.map { it.text }, equalTo(texts.toList()))
        assertThat(options.map { it.label }, equalTo(texts.indices.map { ('A' + it).toString() }))
    }

    /** Asserts that the correct answer of [question] is the option [label] with text [text] */
    private fun assertCorrect(
        question: MultipleChoiceQuestion?,
        label: String,
        text: String,
    ) {
        assertThat("expected a multiple choice question", question, notNullValue())
        assertThat(question!!.correctOption, equalTo(MultipleChoiceOption(label, text)))
        assertThat(question.correctIndex, equalTo(label[0] - 'A'))
    }

    // endregion
}
