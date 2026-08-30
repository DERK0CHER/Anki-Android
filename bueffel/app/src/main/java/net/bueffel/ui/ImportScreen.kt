package net.bueffel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.bueffel.importer.QuestionParser
import net.bueffel.ui.theme.BueffelColors
import net.bueffel.ui.theme.BueffelShape

/**
 * Paste questions in.
 *
 * The questions are written elsewhere - a chat with a language model is the expected source -
 * so all this screen has to do is take the text and say honestly how much of it it understood.
 */
@Composable
fun ImportScreen(
    onCancel: () -> Unit,
    onImport: (name: String, text: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }

    val preview = remember(text) { QuestionParser.parse(text) }
    val found = preview.questions.size

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BueffelColors.Background)
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = BueffelShape.Gutter)
                .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(28.dp))
        Text(
            text = "einfügen",
            style = MaterialTheme.typography.displayMedium,
            color = BueffelColors.TextPrimary,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text =
                "Frage, darunter die Antworten als A) B) C), darunter eine Zeile " +
                    "„Lösung: B\". Mehrere Fragen mit einer Leerzeile trennen.",
            style = MaterialTheme.typography.bodyLarge,
            color = BueffelColors.TextSecondary,
        )

        Spacer(Modifier.height(28.dp))
        Caption(text = "NAME")
        Spacer(Modifier.height(8.dp))
        InputBox(
            value = name,
            onValueChange = { name = it },
            placeholder = "z. B. Theorieprüfung",
            minHeight = 56.dp,
            singleLine = true,
        )

        Spacer(Modifier.height(20.dp))
        Caption(text = "FRAGEN")
        Spacer(Modifier.height(8.dp))
        InputBox(
            value = text,
            onValueChange = { text = it },
            placeholder = "Hier den Text einfügen",
            minHeight = 220.dp,
            singleLine = false,
        )

        Spacer(Modifier.height(16.dp))
        Text(
            text = importSummary(found, preview.skipped),
            style = MaterialTheme.typography.bodyMedium,
            color =
                when {
                    found > 0 -> BueffelColors.Correct
                    text.isBlank() -> BueffelColors.TextMuted
                    else -> BueffelColors.Wrong
                },
        )

        Spacer(Modifier.height(24.dp))
        BueffelButton(
            text = if (found > 0) "$found Fragen übernehmen" else "Übernehmen",
            onClick = { onImport(name.ifBlank { "Fragen" }, text) },
            enabled = found > 0,
        )
        Spacer(Modifier.height(12.dp))
        BueffelButton(text = "Abbrechen", onClick = onCancel, filled = false)
        Spacer(Modifier.height(28.dp))
    }
}

private fun importSummary(
    found: Int,
    skipped: Int,
): String =
    when {
        found == 0 && skipped == 0 -> "Noch nichts eingefügt."
        found == 0 -> "Nichts erkannt. Fehlt vielleicht die Lösungszeile?"
        skipped == 0 -> "$found Fragen erkannt."
        else -> "$found Fragen erkannt, $skipped Blöcke übersprungen."
    }

/** A text field drawn the way the rest of the app is drawn */
@Composable
private fun InputBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minHeight: Dp,
    singleLine: Boolean,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = BueffelColors.TextPrimary),
        cursorBrush = SolidColor(BueffelColors.TextPrimary),
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = minHeight)
                .clip(RoundedCornerShape(BueffelShape.Radius))
                .background(BueffelColors.Surface)
                .padding(16.dp),
        decorationBox = { inner ->
            // a Box so the placeholder sits behind the text rather than above it
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = BueffelColors.TextMuted,
                    )
                }
                inner()
            }
        },
    )
}
