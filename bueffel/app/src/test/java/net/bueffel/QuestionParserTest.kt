package net.bueffel

import net.bueffel.importer.QuestionParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Tests [QuestionParser] against the shapes a language model actually produces */
class QuestionParserTest {
    @Test
    fun `lettered options with a lettered solution`() {
        val result =
            QuestionParser.parse(
                """
                Was bedeutet ein durchgezogener Mittelstreifen?
                A) Überholen erlaubt
                B) Er darf nicht überfahren werden
                C) Baustelle
                Lösung: B
                """.trimIndent(),
            )

        assertEquals(1, result.questions.size)
        assertEquals(0, result.skipped)
        val question = result.questions.first()
        assertEquals("Was bedeutet ein durchgezogener Mittelstreifen?", question.prompt)
        assertEquals(3, question.choices.size)
        assertEquals("B", question.correctChoice.label)
        assertEquals("Er darf nicht überfahren werden", question.correctChoice.text)
    }

    @Test
    fun `numbered options and a numbered solution`() {
        val result =
            QuestionParser.parse(
                """
                Frage: Wie hoch ist die Regelgeschwindigkeit innerorts?
                1. 30 km/h
                2. 50 km/h
                3. 60 km/h
                Antwort: 2
                """.trimIndent(),
            )

        assertEquals(
            "B",
            result.questions
                .single()
                .correctChoice.label,
        )
        assertEquals(
            "50 km/h",
            result.questions
                .single()
                .correctChoice.text,
        )
    }

    @Test
    fun `the solution may be the full text of the option`() {
        val result =
            QuestionParser.parse(
                """
                Hauptstadt von Frankreich?
                A) Berlin
                B) Paris
                Richtig: paris
                """.trimIndent(),
            )

        assertEquals(1, result.questions.single().correctIndex)
    }

    @Test
    fun `several questions separated by blank lines`() {
        val result =
            QuestionParser.parse(
                """
                Erste Frage?
                A) eins
                B) zwei
                Lösung: A

                Zweite Frage?
                A) drei
                B) vier
                Lösung: B
                """.trimIndent(),
            )

        assertEquals(2, result.questions.size)
        assertEquals(0, result.questions[0].correctIndex)
        assertEquals(1, result.questions[1].correctIndex)
    }

    @Test
    fun `blocks separated by a rule`() {
        val result =
            QuestionParser.parse(
                """
                Erste Frage?
                A) eins
                B) zwei
                Lösung: A
                ---
                Zweite Frage?
                A) drei
                B) vier
                Lösung: B
                """.trimIndent(),
            )

        assertEquals(2, result.questions.size)
    }

    @Test
    fun `a numbered question is not mistaken for an option`() {
        val result =
            QuestionParser.parse(
                """
                1. Was gilt an dieser Kreuzung?
                A) rechts vor links
                B) Vorfahrt achten
                Lösung: A
                """.trimIndent(),
            )

        val question = result.questions.single()
        assertEquals("Was gilt an dieser Kreuzung?", question.prompt)
        assertEquals(2, question.choices.size)
    }

    @Test
    fun `a question spanning two lines`() {
        val result =
            QuestionParser.parse(
                """
                Du näherst dich einer Kreuzung.
                Wie verhältst du dich?
                A) bremsen
                B) beschleunigen
                Lösung: A
                """.trimIndent(),
            )

        assertEquals("Du näherst dich einer Kreuzung. Wie verhältst du dich?", result.questions.single().prompt)
    }

    @Test
    fun `a block without a solution is skipped rather than failing the paste`() {
        val result =
            QuestionParser.parse(
                """
                Gute Frage?
                A) eins
                B) zwei

                Zweite Frage?
                A) drei
                B) vier
                Lösung: B
                """.trimIndent(),
            )

        assertEquals(1, result.questions.size)
        assertEquals(1, result.skipped)
    }

    @Test
    fun `more options than there are boxes is skipped`() {
        val result =
            QuestionParser.parse(
                """
                Zu viele?
                A) 1
                B) 2
                C) 3
                D) 4
                E) 5
                Lösung: A
                """.trimIndent(),
            )

        assertEquals(0, result.questions.size)
        assertEquals(1, result.skipped)
    }

    @Test
    fun `empty input yields nothing`() {
        val result = QuestionParser.parse("   \n\n  ")
        assertEquals(0, result.questions.size)
        assertEquals(0, result.skipped)
    }

    @Test
    fun `prose without options is skipped`() {
        val result = QuestionParser.parse("Hier ist deine Fragensammlung, viel Erfolg!")
        assertEquals(0, result.questions.size)
        assertEquals(1, result.skipped)
        assertNull(result.questions.firstOrNull())
    }
}
