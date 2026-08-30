package net.bueffel.data

import android.content.Context
import net.bueffel.model.Card
import net.bueffel.model.Deck
import net.bueffel.model.Question
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
            val cards = JSONArray()
            for (card in deck.cards) {
                val answers = JSONArray()
                for (answer in card.question.answers) {
                    answers.put(answer)
                }
                cards.put(
                    JSONObject()
                        .put("prompt", card.question.prompt)
                        .put("answers", answers)
                        .put("correctIndex", card.question.correctIndex)
                        .put("box", card.box),
                )
            }
            array.put(JSONObject().put("id", deck.id).put("name", deck.name).put("cards", cards))
        }
        return JSONObject().put("version", 1).put("decks", array).toString()
    }

    private fun decode(text: String): List<Deck> {
        val decks = mutableListOf<Deck>()
        val array = JSONObject(text).optJSONArray("decks") ?: return decks
        for (i in 0 until array.length()) {
            val deckJson = array.optJSONObject(i) ?: continue
            val cardsJson = deckJson.optJSONArray("cards") ?: JSONArray()
            val cards = mutableListOf<Card>()
            for (j in 0 until cardsJson.length()) {
                val cardJson = cardsJson.optJSONObject(j) ?: continue
                val answersJson = cardJson.optJSONArray("answers") ?: continue
                val answers = mutableListOf<String>()
                for (k in 0 until answersJson.length()) {
                    answers += answersJson.optString(k)
                }
                val correctIndex = cardJson.optInt("correctIndex", -1)
                if (answers.size < 2 || correctIndex !in answers.indices) continue
                cards +=
                    Card(
                        question = Question(cardJson.optString("prompt"), answers, correctIndex),
                        box = cardJson.optInt("box", 0),
                    )
            }
            decks += Deck(deckJson.optString("id"), deckJson.optString("name"), cards)
        }
        return decks
    }

    companion object {
        private const val FILE_NAME = "decks.json"
        private const val TAG = "DeckStore"
    }
}
