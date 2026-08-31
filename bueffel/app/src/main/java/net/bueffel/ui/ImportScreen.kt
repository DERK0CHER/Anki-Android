package net.bueffel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import net.bueffel.importer.QuestionParser
import net.bueffel.model.Question
import net.bueffel.ui.theme.BueffelColors
import net.bueffel.ui.theme.BueffelShape

/**
 * Getting questions in, without a keyboard.
 *
 * The questions are written in a chat with a language model, which means the text is already on
 * the clipboard. So this screen hands out the prompt to paste into that chat, then reads the
 * answer back off the clipboard. Nobody types a question set on a phone, and the paragraph-sized
 * text box this screen used to have could not scroll inside a scrolling page, so it clipped.
 *
 * The way out is pinned to the bottom edge, where a cancel belongs, instead of trailing the
 * content into the middle of the screen.
 */
@Composable
fun ImportScreen(
    onCancel: () -> Unit,
    onImport: (name: String, questions: List<Question>) -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    var parsed by remember { mutableStateOf<QuestionParser.ImportResult?>(null) }
    var promptCopied by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }

    val found = parsed?.questions.orEmpty()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BueffelColors.Background)
                // safeDrawing, not systemBars plus ime: the keyboard's inset already covers the
                // navigation bar, so adding both pushes the screen up by the keyboard AND the
                // bar again. safeDrawing takes whichever is larger.
                .safeDrawingPadding()
                .padding(horizontal = BueffelShape.Gutter),
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(28.dp))
            Text(
                text = "einfügen",
                style = MaterialTheme.typography.displayMedium,
                color = BueffelColors.TextPrimary,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Lass dir die Fragen von einer KI schreiben. Den Prompt dafür gibt es hier.",
                style = MaterialTheme.typography.bodyLarge,
                color = BueffelColors.TextSecondary,
            )

            Spacer(Modifier.height(36.dp))
            StepLabel(number = "1", text = "Prompt kopieren, in einen KI-Chat einfügen")
            Spacer(Modifier.height(14.dp))
            BueffelButton(
                text = if (promptCopied) "Prompt kopiert ✓" else "Prompt kopieren",
                onClick = {
                    clipboard.setText(AnnotatedString(AI_PROMPT))
                    promptCopied = true
                },
                filled = false,
            )

            Spacer(Modifier.height(28.dp))
            StepLabel(number = "2", text = "Antwort der KI kopieren, hier einlesen")
            Spacer(Modifier.height(14.dp))
            BueffelButton(
                text = "Aus Zwischenablage einlesen",
                onClick = { parsed = QuestionParser.parse(clipboard.getText()?.text.orEmpty()) },
            )

            parsed?.let { result ->
                Spacer(Modifier.height(24.dp))
                ResultPanel(result)
            }

            if (found.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Caption(text = "NAME")
                Spacer(Modifier.height(8.dp))
                NameField(value = name, onValueChange = { name = it })
                Spacer(Modifier.height(20.dp))
                BueffelButton(
                    text = "${found.size} Fragen übernehmen",
                    onClick = { onImport(name.ifBlank { defaultName(found) }, found) },
                )
            }

            Spacer(Modifier.height(24.dp))
        }

        BueffelButton(text = "Abbrechen", onClick = onCancel, filled = false)
        Spacer(Modifier.height(20.dp))
    }
}

/** What the clipboard turned out to contain */
@Composable
private fun ResultPanel(result: QuestionParser.ImportResult) {
    val found = result.questions.size
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BueffelShape.Radius))
                .background(BueffelColors.Surface)
                .padding(22.dp),
    ) {
        Text(
            text =
                when {
                    found == 0 -> "Nichts erkannt"
                    result.skipped == 0 -> "$found Fragen erkannt"
                    else -> "$found Fragen erkannt, ${result.skipped} übersprungen"
                },
            style = MaterialTheme.typography.titleLarge,
            color = if (found == 0) BueffelColors.Wrong else BueffelColors.Correct,
        )
        Spacer(Modifier.height(10.dp))
        if (found == 0) {
            Text(
                text =
                    "Liegt die Antwort der KI wirklich in der Zwischenablage? Erwartet wird " +
                        "das JSON aus dem Prompt oben.",
                style = MaterialTheme.typography.bodyMedium,
                color = BueffelColors.TextSecondary,
            )
        } else {
            Caption(text = "ERSTE FRAGE")
            Spacer(Modifier.height(4.dp))
            Text(
                text = result.questions.first().prompt,
                style = MaterialTheme.typography.bodyMedium,
                color = BueffelColors.TextSecondary,
            )
        }
    }
}

/** One line only: a single line stays usable with the keyboard up */
@Composable
private fun NameField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = BueffelColors.TextPrimary),
        cursorBrush = SolidColor(BueffelColors.TextPrimary),
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BueffelShape.Radius))
                .background(BueffelColors.Surface)
                .padding(horizontal = 18.dp, vertical = 18.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = "leer lassen für automatisch",
                        style = MaterialTheme.typography.bodyLarge,
                        color = BueffelColors.TextMuted,
                    )
                }
                inner()
            }
        },
    )
}

/** Names a set after its first question, so nothing ends up called just "Fragen" */
private fun defaultName(questions: List<Question>): String {
    val first =
        questions
            .firstOrNull()
            ?.prompt
            .orEmpty()
            .trim()
    if (first.isEmpty()) return "Fragen"
    return first.take(28).trimEnd() + if (first.length > 28) "…" else ""
}

/** Handed to the user's chat of choice, so the answer comes back in a shape the parser reads */
private const val AI_PROMPT =
    """Schreibe mir 60 Multiple-Choice-Fragen zum Thema: <HIER DEIN THEMA>

Teile sie in 4 bis 8 Unterbereiche auf und schreibe zu jeder Frage dazu, in welchen sie gehoert.

Antworte ausschliesslich mit JSON in genau dieser Form, ohne weiteren Text:

[
  {
    "topic": "Name des Unterbereichs",
    "question": "Worum geht es hier?",
    "answers": ["Erste Antwort", "Zweite Antwort", "Dritte Antwort"],
    "correct": 1
  }
]

Regeln:
- "topic" ist der Unterbereich, immer gesetzt, gleich geschrieben fuer alle Fragen darin
- "correct" ist der Index der richtigen Antwort, gezaehlt ab 0
- genau eine richtige Antwort pro Frage
- drei oder vier Antworten
- die falschen Antworten muessen plausibel sein, keine offensichtlichen Fuellsel
- keine Erklaerungen, keine Ueberschriften, kein Text vor oder nach dem JSON"""
