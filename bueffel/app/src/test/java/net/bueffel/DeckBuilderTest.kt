package net.bueffel

import net.bueffel.importer.DeckBuilder
import net.bueffel.model.Card
import net.bueffel.model.Deck
import net.bueffel.model.Question
import net.bueffel.model.Subtopic
import org.junit.Assert.assertEquals
import org.junit.Test

/** Tests how imported questions become a topic with its parts, and how the parts add up */
class DeckBuilderTest {
    private fun question(
        prompt: String,
        topic: String? = null,
    ) = Question(prompt = prompt, answers = listOf("eins", "zwei"), correctIndex = 0, topic = topic)

    @Test
    fun `questions land in the part they name`() {
        val deck =
            DeckBuilder.build(
                id = "d",
                name = "Theorie",
                questions =
                    listOf(
                        question("Schild A", topic = "Verkehrszeichen"),
                        question("Vorfahrt A", topic = "Vorfahrt"),
                        question("Schild B", topic = "Verkehrszeichen"),
                    ),
            )

        assertEquals(listOf("Verkehrszeichen", "Vorfahrt"), deck.subtopics.map { it.name })
        assertEquals(2, deck.subtopics[0].cards.size)
        assertEquals(1, deck.subtopics[1].cards.size)
    }

    @Test
    fun `the parts keep the order the questions came in`() {
        val deck =
            DeckBuilder.build(
                id = "d",
                name = "Theorie",
                questions = listOf(question("a", "Zweitens"), question("b", "Erstens")),
            )

        assertEquals(listOf("Zweitens", "Erstens"), deck.subtopics.map { it.name })
    }

    @Test
    fun `a set that names no parts is one part, named after the set`() {
        val deck =
            DeckBuilder.build(id = "d", name = "Vokabeln", questions = listOf(question("a"), question("b")))

        assertEquals(1, deck.subtopics.size)
        assertEquals("Vokabeln", deck.subtopics.single().name)
        assertEquals(2, deck.cards.size)
    }

    @Test
    fun `questions that name no part gather in one of their own`() {
        val deck =
            DeckBuilder.build(
                id = "d",
                name = "Theorie",
                questions = listOf(question("a", "Vorfahrt"), question("b")),
            )

        assertEquals(listOf("Vorfahrt", DeckBuilder.UNSORTED), deck.subtopics.map { it.name })
    }

    @Test
    fun `the topic's progress is its parts put together, weighed by size`() {
        // one finished question in a part of its own, and forty untouched ones in another
        val deck =
            Deck(
                id = "d",
                name = "Theorie",
                subtopics =
                    listOf(
                        Subtopic("a", "Klein", listOf(Card(question("x"), box = Card.LEARNED_BOX))),
                        Subtopic("b", "Groß", (1..40).map { Card(question("y$it")) }),
                    ),
            )

        assertEquals(1f, deck.subtopics[0].progress, 0.0001f)
        assertEquals(0f, deck.subtopics[1].progress, 0.0001f)
        // averaging the two bars would call this half done; counting questions calls it 1 in 41
        assertEquals(1f / 41f, deck.progress, 0.0001f)
    }

    @Test
    fun `studying a whole topic writes each question back to its own part`() {
        val deck =
            Deck(
                id = "d",
                name = "Theorie",
                subtopics =
                    listOf(
                        Subtopic("a", "Erste", listOf(Card(question("x")))),
                        Subtopic("b", "Zweite", listOf(Card(question("y")))),
                    ),
            )

        val studied = deck.cards.map { if (it.question.prompt == "y") it.copy(box = 5) else it }
        val updated = deck.withMixedCards(studied)

        val untouched = updated.subtopics[0].cards.single()
        val studiedBack = updated.subtopics[1].cards.single()
        assertEquals(0, untouched.box)
        assertEquals(5, studiedBack.box)
    }

    @Test
    fun `a question that appears in two parts keeps both results`() {
        // the same wording in two parts is ordinary in a large set, and matching them with a
        // plain map keyed on the question silently wrote one part's result into both
        val deck =
            Deck(
                id = "d",
                name = "Theorie",
                subtopics =
                    listOf(
                        Subtopic("a", "Erste", listOf(Card(question("gleich"), box = 1))),
                        Subtopic("b", "Zweite", listOf(Card(question("gleich"), box = 2))),
                    ),
            )

        val studied = deck.cards.map { it.copy(box = it.box + 1) }
        val updated = deck.withMixedCards(studied)

        assertEquals(listOf(2, 3), updated.subtopics.map { it.cards.single().box }.sorted())
    }

    @Test
    fun `a part the studied set never touched is left as it was`() {
        val deck =
            Deck(
                id = "d",
                name = "Theorie",
                subtopics = listOf(Subtopic("a", "Erste", listOf(Card(question("x"), box = 4)))),
            )

        assertEquals(deck, deck.withMixedCards(emptyList()))
    }
}
