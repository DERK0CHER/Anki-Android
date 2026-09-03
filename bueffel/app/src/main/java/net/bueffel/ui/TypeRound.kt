package net.bueffel.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.bueffel.domain.LineDiff
import net.bueffel.model.CodeTask
import net.bueffel.ui.theme.BueffelColors
import net.bueffel.ui.theme.BueffelShape

/**
 * One line, typed out, compared against the model answer.
 *
 * The card that asks for `d = [3;6;2;5;9]` used to open the whole editor: a text area six lines
 * tall, a line by line comparison and three marks to choose between, for an answer that is
 * either right or it is not. So a card whose model answer is a single line is asked here
 * instead, where the app decides and the reader only reads the verdict.
 *
 * It can decide because there is nothing to judge: with one line there is no renamed variable to
 * argue about and no half-right thought to be generous with. The spacing is normalised - see
 * [LineDiff.sameLine] - and a card that accepts two spellings lists the other under `alt:`.
 */
@Composable
fun TypeRound(
    task: CodeTask,
    round: String,
    onSubmit: (correct: Boolean) -> Unit,
) {
    var typed by remember(task) { mutableStateOf(TextFieldValue("")) }
    var verdict by remember(task) { mutableStateOf<Boolean?>(null) }
    val settled = verdict

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(6.dp))
        Caption(text = round)
        Spacer(Modifier.height(14.dp))
        TaskFront(task)
        Spacer(Modifier.height(18.dp))

        OneLine(
            value = typed,
            readOnly = settled != null,
            outline =
                when (settled) {
                    null -> BueffelColors.Border
                    true -> BueffelColors.Correct
                    false -> BueffelColors.Wrong
                },
            onValueChange = { typed = it },
        )

        if (settled == null) {
            Spacer(Modifier.height(12.dp))
            SymbolBar(onInsert = { typed = typed.insert(it) })
            Spacer(Modifier.height(18.dp))
            BueffelButton(
                text = "Abgeben",
                onClick = { verdict = task.accepted.any { LineDiff.sameLine(typed.text, it) } },
            )
        } else {
            Spacer(Modifier.height(18.dp))
            Text(
                text = if (settled) "Richtig" else "Nicht ganz",
                style = MaterialTheme.typography.titleLarge,
                color = if (settled) BueffelColors.Correct else BueffelColors.Wrong,
            )
            // the model answer only when it is needed: after a right answer it is already there,
            // written by the reader, and repeating it says nothing
            if (!settled) {
                Spacer(Modifier.height(14.dp))
                Caption(text = "MUSTERLÖSUNG")
                Spacer(Modifier.height(8.dp))
                GivenCode(code = task.solution)
            }
            Spacer(Modifier.height(18.dp))
            BueffelButton(text = "Weiter", onClick = { onSubmit(settled) })
        }
        Spacer(Modifier.height(20.dp))
    }
}

/**
 * The editor's keyboard on one line.
 *
 * Same rules as the multi-line one, and for the same reason: autocorrect turns `int i` into
 * `Int i`. Return does not break the line here - there is only one - so it is labelled Done.
 */
@Composable
private fun OneLine(
    value: TextFieldValue,
    readOnly: Boolean,
    outline: Color,
    onValueChange: (TextFieldValue) -> Unit,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        readOnly = readOnly,
        singleLine = true,
        textStyle =
            TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                color = BueffelColors.TextPrimary,
            ),
        cursorBrush = SolidColor(BueffelColors.LearnedGreen),
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done,
            ),
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BueffelShape.Radius))
                .background(BueffelColors.Surface)
                .border(BorderStroke(1.dp, outline), RoundedCornerShape(BueffelShape.Radius))
                .padding(horizontal = 16.dp, vertical = 18.dp),
    )
}
