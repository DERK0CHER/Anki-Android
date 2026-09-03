package net.bueffel

import net.bueffel.data.DeckStore
import net.bueffel.model.Card
import net.bueffel.model.CodeTask
import net.bueffel.model.Deck
import net.bueffel.model.Question
import net.bueffel.model.Subtopic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Tests saving, exporting and restoring.
 *
 * Robolectric is needed for org.json, which comes from the mockable android.jar under a plain
 * unit test and throws on the first call against anything it returns.
 */
@RunWith(RobolectricTestRunner::class)
class DeckStoreTest {
    private val file = File.createTempFile("decks", ".json").also { it.deleteOnExit() }
    private val store = DeckStore(file)

    private fun card(
        prompt: String,
        box: Int = 0,
        hard: Boolean = false,
    ) = Card(Question(prompt, listOf("eins", "zwei"), 0), box = box, hard = hard)

    private fun deck(
        id: String,
        vararg cards: Card,
    ) = Deck(id, "Thema $id", listOf(Subtopic("$id-0", "Teil", cards.toList())))

    @Test
    fun `a backup carries the boxes and the flags back`() {
        val decks = listOf(deck("a", card("x", box = 5, hard = true), card("y", box = 2)))

        val restored = store.restore(current = emptyList(), text = store.export(decks))

        val cards = restored.single().cards
        assertEquals(listOf(5, 2), cards.map { it.box })
        assertEquals(listOf(true, false), cards.map { it.hard })
        assertEquals("Thema a", restored.single().name)
    }

    @Test
    fun `restoring replaces a topic it knows and leaves the rest alone`() {
        val onDevice = listOf(deck("a", card("x", box = 0)), deck("b", card("z", box = 7)))
        val backup = store.export(listOf(deck("a", card("x", box = 6))))

        val restored = store.restore(current = onDevice, text = backup)

        assertEquals(listOf("a", "b"), restored.map { it.id })
        assertEquals(6, restored[0].cards.single().box)
        // the topic the backup never heard of keeps everything it had
        assertEquals(7, restored[1].cards.single().box)
    }

    @Test
    fun `a topic only the backup knows is added`() {
        val backup = store.export(listOf(deck("neu", card("x"))))

        val restored = store.restore(current = listOf(deck("alt", card("y"))), text = backup)

        assertEquals(listOf("alt", "neu"), restored.map { it.id })
    }

    @Test
    fun `a file that is not a backup changes nothing`() {
        val onDevice = listOf(deck("a", card("x", box = 3)))

        assertEquals(onDevice, store.restore(current = onDevice, text = "Guten Tag, ich bin kein JSON"))
        assertEquals(onDevice, store.restore(current = onDevice, text = ""))
    }

    @Test
    fun `saving and loading survives a round trip`() {
        val decks = listOf(deck("a", card("x", box = 4, hard = true)))

        store.save(decks)

        assertEquals(decks, store.load())
    }

    @Test
    fun `a file from before topics existed becomes one part`() {
        // version 1 wrote the cards straight onto the deck, with no subtopics and no hard flag
        file.writeText(
            """
            {"version":1,"decks":[{"id":"old","name":"Altes Thema","cards":[
              {"prompt":"Frage","answers":["eins","zwei"],"correctIndex":0,"box":3}
            ]}]}
            """.trimIndent(),
        )

        val loaded = store.load().single()

        assertEquals("Altes Thema", loaded.name)
        assertEquals(1, loaded.subtopics.size)
        assertEquals("Altes Thema", loaded.subtopics.single().name)
        assertEquals(3, loaded.cards.single().box)
        assertTrue(!loaded.cards.single().hard)
    }

    @Test
    fun `a code card survives being written and read back`() {
        val task =
            CodeTask(
                prompt = "Vervollständige f",
                given = "void f() {\n>>> Hier fehlt was\n}",
                solution = "    return;",
                alternatives = listOf("    return 0;"),
                topic = "Funktionen",
                tags = listOf("WS24"),
            )
        val decks = listOf(Deck("a", "Thema", listOf(Subtopic("a-0", "Teil", listOf(Card(task, box = 3, sorted = 2))))))

        store.save(decks)

        val loaded =
            store
                .load()
                .single()
                .cards
                .single()
        assertEquals(task, loaded.task)
        assertEquals(3, loaded.box)
        assertEquals(2, loaded.sorted)
    }

    @Test
    fun `a card written before there were card types is still a question`() {
        // version 2 wrote no "type" at all
        file.writeText(
            """
            {"version":2,"decks":[{"id":"old","name":"Alt","subtopics":[
              {"id":"old-0","name":"Teil","cards":[
                {"prompt":"Frage","answers":["eins","zwei"],"correctIndex":1,"box":2}
              ]}
            ]}]}
            """.trimIndent(),
        )

        val loaded =
            store
                .load()
                .single()
                .cards
                .single()

        assertTrue(loaded.task is Question)
        assertEquals("zwei", (loaded.task as Question).correctAnswer)
        assertEquals(2, loaded.box)
    }
}
