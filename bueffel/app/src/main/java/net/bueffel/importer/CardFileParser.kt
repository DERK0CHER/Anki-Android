package net.bueffel.importer

import net.bueffel.model.CodeTask
import net.bueffel.model.Question
import net.bueffel.model.Task

/**
 * Reads cards written at a desk rather than on a phone.
 *
 * Code does not survive being hand-written into JSON: every brace has to be escaped and every
 * line break becomes a `\n`, which is unreadable and unwritable. So the format is plain text with
 * fenced blocks, which is what code already looks like everywhere else.
 *
 * ```
 * type: code
 * topic: Verkettete Listen
 * tags: WS24, Node_Delete
 * front:
 * ```c
 * void node_delete(node_t *n) {
 * >>> Hier fehlt was
 * }
 * ```
 * back:
 * ```c
 *     free(n->data);
 *     free(n);
 * ```
 * ---
 * type: choice
 * front: Was ergibt 1 << 3?
 * - 4
 * - *8
 * - 16
 * ```
 *
 * A field's value is the rest of its line, or - when that is empty - the fenced block that
 * follows. Cards are separated by a line holding nothing but `---`. `alt:` may appear more than
 * once, for the same answer written differently. A block that cannot be read is skipped and
 * counted rather than failing the whole file.
 */
object CardFileParser {
    data class Found(
        val tasks: List<Task>,
        val skipped: Int,
    )

    fun parse(text: String): Found {
        val tasks = mutableListOf<Task>()
        var skipped = 0
        for (block in split(text)) {
            val task = readCard(block)
            if (task != null) tasks += task else skipped++
        }
        return Found(tasks, skipped)
    }

    /** Splits on a lone `---`, but never inside a fenced block */
    private fun split(text: String): List<List<String>> {
        val blocks = mutableListOf<List<String>>()
        var current = mutableListOf<String>()
        var fenced = false
        for (raw in text.replace("\r\n", "\n").split("\n")) {
            val line = raw.trimEnd()
            if (line.trimStart().startsWith(FENCE)) fenced = !fenced
            if (!fenced && line.trim() == SEPARATOR) {
                if (current.any { it.isNotBlank() }) blocks += current
                current = mutableListOf()
            } else {
                current += line
            }
        }
        if (current.any { it.isNotBlank() }) blocks += current
        return blocks
    }

    private fun readCard(lines: List<String>): Task? {
        val fields = mutableMapOf<String, String>()
        val alternatives = mutableListOf<String>()
        val options = mutableListOf<String>()
        var correct = -1

        var index = 0
        while (index < lines.size) {
            val line = lines[index]
            val option = OPTION.find(line)
            if (option != null) {
                val body = option.groupValues[1].trim()
                if (body.startsWith("*")) correct = options.size
                options += body.removePrefix("*").trim()
                index++
                continue
            }

            val field = FIELD.find(line)
            if (field == null) {
                index++
                continue
            }
            val key = field.groupValues[1].lowercase()
            val inline = field.groupValues[2].trim()
            if (inline.isNotEmpty()) {
                if (key == ALT) alternatives += inline else fields[key] = inline
                index++
                continue
            }

            // an empty value means the fenced block underneath is the value
            val (block, next) = readBlock(lines, index + 1)
            if (key == ALT) alternatives += block else fields[key] = block
            index = next
        }

        val prompt = fields[FRONT]?.takeIf { it.isNotBlank() } ?: return null
        val topic = fields[TOPIC]?.takeIf { it.isNotBlank() }
        val tags =
            fields[TAGS]
                .orEmpty()
                .split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }

        val back = fields[BACK]?.takeIf { it.isNotBlank() }
        // the type may be left out: a card with options is a question, one with a back is code
        val type = fields[TYPE]?.lowercase() ?: if (back != null) CODE else CHOICE
        return when (type) {
            CODE ->
                if (back == null) {
                    null
                } else {
                    CodeTask(
                        prompt = prompt,
                        solution = back,
                        alternatives = alternatives,
                        topic = topic,
                        tags = tags,
                    )
                }

            CHOICE ->
                if (options.size < 2 || correct !in options.indices) {
                    null
                } else {
                    Question(
                        prompt = prompt,
                        answers = options,
                        correctIndex = correct,
                        topic = topic,
                        tags = tags,
                    )
                }

            else -> null
        }
    }

    /** Reads a fenced block starting at [from], and says where it ended */
    private fun readBlock(
        lines: List<String>,
        from: Int,
    ): Pair<String, Int> {
        var index = from
        while (index < lines.size && lines[index].isBlank()) index++
        if (index >= lines.size || !lines[index].trimStart().startsWith(FENCE)) return "" to from
        index++
        val body = mutableListOf<String>()
        while (index < lines.size && !lines[index].trimStart().startsWith(FENCE)) {
            body += lines[index]
            index++
        }
        // step over the closing fence, if the file bothered to write one
        if (index < lines.size) index++
        return body.joinToString("\n").trim('\n') to index
    }

    private val FIELD = Regex("""^\s*([A-Za-zÄÖÜäöü]+)\s*:(.*)$""")
    private val OPTION = Regex("""^\s*[-*]\s+(.+)$""")

    private const val FENCE = "```"
    private const val SEPARATOR = "---"
    private const val TYPE = "type"
    private const val FRONT = "front"
    private const val BACK = "back"
    private const val ALT = "alt"
    private const val TAGS = "tags"
    private const val TOPIC = "topic"
    private const val CODE = "code"
    private const val CHOICE = "choice"
}
