package net.bueffel.data

import android.content.Context
import net.bueffel.model.Card
import net.bueffel.model.Deck
import net.bueffel.model.Question
import net.bueffel.model.Subtopic
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Decks on disk, as one JSON file.
 *
 * A question set is a few hundred kilobytes at most and is written once per session, so a plain
 * file is enough; it keeps the app free of a database and the build free of another plugin.
 */
class DeckStore(
    private val file: File,
) {
    constructor(context: Context) : this(File(context.filesDir, FILE_NAME))

    fun load(): List<Deck> {
        if (!file.exists()) return emptyList()
        return try {
            decode(file.readText())
        } catch (e: Exception) {
            // a corrupt file must not make the app unusable: start over rather than crash
            android.util.Log.w(TAG, "could not read decks, starting empty", e)
            emptyList()
        }
    }

    fun save(decks: List<Deck>) {
        try {
            file.writeText(encode(decks))
        } catch (e: Exception) {
            android.util.Log.w(TAG, "could not write decks", e)
        }
    }

    private fun encode(decks: List<Deck>): String {
        val array = JSONArray()
        for (deck in decks) {
            val subtopics = JSONArray()
            for (subtopic in deck.subtopics) {
                subtopics.put(
                    JSONObject()
                        .put("id", subtopic.id)
                        .put("name", subtopic.name)
                        .put("cards", encodeCards(subtopic.cards)),
                )
            }
            array.put(
                JSONObject()
                    .put("id", deck.id)
                    .put("name", deck.name)
                    .put("subtopics", subtopics),
            )
        }
        return JSONObject().put("version", VERSION).put("decks", array).toString()
    }

    private fun encodeCards(cards: List<Card>): JSONArray {
        val array = JSONArray()
        for (card in cards) {
            val answers = JSONArray()
            for (answer in card.question.answers) {
                answers.put(answer)
            }
            array.put(
                JSONObject()
                    .put("prompt", card.question.prompt)
                    .put("answers", answers)
                    .put("correctIndex", card.question.correctIndex)
                    .put("box", card.box)
                    .put("hard", card.hard),
            )
        }
        return array
    }

    private fun decode(text: String): List<Deck> {
        val decks = mutableListOf<Deck>()
        val array = JSONObject(text).optJSONArray("decks") ?: return decks
        for (i in 0 until array.length()) {
            val deckJson = array.optJSONObject(i) ?: continue
            val id = deckJson.optString("id")
            val name = deckJson.optString("name")
            val subtopicsJson = deckJson.optJSONArray("subtopics")
            val subtopics =
                if (subtopicsJson != null) {
                    decodeSubtopics(subtopicsJson)
                } else {
                    // written before topics existed: the whole deck was one flat list of cards
                    listOf(Subtopic(id = "$id-all", name = name, cards = decodeCards(deckJson.optJSONArray("cards"))))
                }
            decks += Deck(id = id, name = name, subtopics = subtopics)
        }
        return decks
    }

    private fun decodeSubtopics(array: JSONArray): List<Subtopic> {
        val subtopics = mutableListOf<Subtopic>()
        for (i in 0 until array.length()) {
            val json = array.optJSONObject(i) ?: continue
            subtopics +=
                Subtopic(
                    id = json.optString("id"),
                    name = json.optString("name"),
                    cards = decodeCards(json.optJSONArray("cards")),
                )
        }
        return subtopics
    }

    private fun decodeCards(array: JSONArray?): List<Card> {
        if (array == null) return emptyList()
        val cards = mutableListOf<Card>()
        for (i in 0 until array.length()) {
            val json = array.optJSONObject(i) ?: continue
            val answersJson = json.optJSONArray("answers") ?: continue
            val answers = mutableListOf<String>()
            for (k in 0 until answersJson.length()) {
                answers += answersJson.optString(k)
            }
            val correctIndex = json.optInt("correctIndex", -1)
            if (answers.size < 2 || correctIndex !in answers.indices) continue
            cards +=
                Card(
                    question = Question(json.optString("prompt"), answers, correctIndex),
                    box = json.optInt("box", 0),
                    hard = json.optBoolean("hard", false),
                )
        }
        return cards
    }

    companion object {
        private const val FILE_NAME = "decks.json"
        private const val TAG = "DeckStore"

        /** 1 was a flat list of cards per deck; 2 groups them into subtopics */
        private const val VERSION = 2
    }
}
