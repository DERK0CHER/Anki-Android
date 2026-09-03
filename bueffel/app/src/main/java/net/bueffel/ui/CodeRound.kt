package net.bueffel.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.bueffel.domain.DiffRow
import net.bueffel.domain.LineChange
import net.bueffel.domain.LineDiff
import net.bueffel.domain.LineMark
import net.bueffel.domain.Marking
import net.bueffel.model.CodeTask
import net.bueffel.ui.theme.BueffelColors
import net.bueffel.ui.theme.BueffelShape

/**
 * Write the code out, then mark it against the model answer.
 *
 * The marking is the learner's own, per line, at the exam's own rates: a slip of syntax costs a
 * quarter of a point and a wrong idea costs a half. No text comparison can tell a renamed
 * variable from a wrong one, so having the app decide would either wave real mistakes through or
 * fail correct code over a space. What the app does instead is line the two answers up, do the
 * arithmetic, and refuse to call an attempt right unless nothing was deducted.
 */
@Composable
fun CodeRound(
    task: CodeTask,
    round: String,
    onSubmit: (correct: Boolean) -> Unit,
) {
    var typed by remember(task) { mutableStateOf(TextFieldValue("")) }
    var rows by remember(task) { mutableStateOf<List<DiffRow>?>(null) }
    val marks = remember(task) { mutableStateListOf<LineMark>() }

    fun submit() {
        val mine = CodeTask.lines(typed.text)
        // the alternative that matches best is the one to be marked against, so a card with two
        // accepted spellings does not fail against the first one listed
        val against = task.accepted.map { CodeTask.lines(it) }
        val exact = against.firstOrNull { LineDiff.same(mine, it) }
        val chosen = exact ?: against.minByOrNull { LineDiff.compare(mine, it).count { row -> row.change != LineChange.Same } }
        val compared = LineDiff.compare(mine, chosen ?: emptyList())
        rows = compared
        marks.clear()
        marks += Marking.from(compared).marks
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(6.dp))
        Caption(text = round)
        Spacer(Modifier.height(14.dp))
        Text(
            text = task.prompt,
            style = MaterialTheme.typography.titleLarge,
            color = BueffelColors.TextPrimary,
        )
        Spacer(Modifier.height(18.dp))

        val compared = rows
        if (compared == null) {
            Editor(value = typed, onValueChange = { typed = it })
            Spacer(Modifier.height(12.dp))
            SymbolBar(onInsert = { typed = typed.insert(it) })
            Spacer(Modifier.height(18.dp))
            BueffelButton(text = "Abgeben", onClick = { submit() })
        } else {
            val marking = Marking(marks.toList(), maxPoints = compared.count { it.change != LineChange.Extra })
            Caption(text = "Zeile antippen, um sie anders zu bewerten")
            Spacer(Modifier.height(12.dp))
            compared.forEachIndexed { index, row ->
                MarkedRow(
                    row = row,
                    mark = marks.getOrElse(index) { LineMark.Right },
                    onCycle = { marks[index] = marks[index].next() },
                )
                Spacer(Modifier.height(6.dp))
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = marking.asScore(),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (marking.clean) BueffelColors.Correct else BueffelColors.Almost,
                )
                Spacer(Modifier.width(10.dp))
                Caption(text = "Punkte")
            }
            Spacer(Modifier.height(16.dp))
            BueffelButton(text = "Weiter", onClick = { onSubmit(marking.clean) })
        }
        Spacer(Modifier.height(20.dp))
    }
}

/** Inserts at the cursor, replacing whatever was selected */
private fun TextFieldValue.insert(text: String): TextFieldValue {
    val start = selection.min
    val end = selection.max
    val updated = this.text.replaceRange(start, end, text)
    return TextFieldValue(updated, TextRange(start + text.length))
}

/**
 * The one place in the app where a keyboard is wanted, and it has to behave.
 *
 * Autocorrect and automatic capitals turn `int i` into `Int i` and `printf` into `Print`, which
 * makes typing code on a phone useless. All three are off, and the keyboard is asked for ASCII
 * so the IME has nothing clever to offer in the first place.
 *
 * Return keeps the indentation of the line it came from, because reaching for a tab key on every
 * line of a function is what stops people typing it out at all.
 */
@Composable
private fun Editor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
) {
    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.autoIndent(value)) },
        textStyle =
            TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = BueffelColors.TextPrimary,
            ),
        cursorBrush = SolidColor(BueffelColors.LearnedGreen),
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Ascii,
            ),
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 190.dp)
                .clip(RoundedCornerShape(BueffelShape.Radius))
                .background(BueffelColors.Surface)
                .border(BorderStroke(1.dp, BueffelColors.Border), RoundedCornerShape(BueffelShape.Radius))
                .padding(16.dp),
    )
}

/**
 * Carries the previous line's leading whitespace onto a new one.
 *
 * Only when the change was a single newline typed at the cursor: anything else is left alone, so
 * pasting or deleting is never second-guessed.
 */
private fun TextFieldValue.autoIndent(before: TextFieldValue): TextFieldValue {
    val added = text.length - before.text.length
    if (added != 1) return this
    val at = selection.min
    if (at <= 0 || text.getOrNull(at - 1) != '\n') return this
    val previous = text.take(at - 1).substringAfterLast('\n')
    val indent = previous.takeWhile { it == ' ' || it == '\t' }
    if (indent.isEmpty()) return this
    return TextFieldValue(text.replaceRange(at, at, indent), TextRange(at + indent.length))
}

/** The characters a German phone keyboard hides three menus deep, on one scrolling row */
@Composable
private fun SymbolBar(onInsert: (String) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    ) {
        for (symbol in SYMBOLS) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BueffelColors.SurfaceRaised)
                        .clickable { onInsert(symbol.inserts) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = symbol.shows,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = BueffelColors.TextPrimary,
                )
            }
        }
    }
}

private data class Symbol(
    val shows: String,
    val inserts: String = shows,
)

private val SYMBOLS =
    listOf(
        Symbol("⇥", "    "),
        Symbol("{"),
        Symbol("}"),
        Symbol("("),
        Symbol(")"),
        Symbol("["),
        Symbol("]"),
        Symbol(";"),
        Symbol("*"),
        Symbol("&"),
        Symbol("->"),
        Symbol("=="),
        Symbol("!="),
        Symbol("<"),
        Symbol(">"),
        Symbol("="),
        Symbol("\""),
        Symbol("%"),
    )

/** One line of the comparison, tapped to change what it is worth */
@Composable
private fun MarkedRow(
    row: DiffRow,
    mark: LineMark,
    onCycle: () -> Unit,
) {
    val tint =
        when {
            row.change == LineChange.Missing -> BueffelColors.Wrong
            row.change == LineChange.Extra -> BueffelColors.Almost
            else -> BueffelColors.TextPrimary
        }
    val gutter =
        when (row.change) {
            LineChange.Same -> " "
            LineChange.Extra -> "+"
            LineChange.Missing -> "−"
        }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (mark == LineMark.Right) BueffelColors.Surface else BueffelColors.WrongSurface)
                .clickable(onClick = onCycle)
                .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row {
            Text(
                text = gutter,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = tint,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = row.text.ifBlank { " " },
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = tint,
                maxLines = 1,
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            )
        }
        // the model answer alongside, when the line was typed differently
        if (row.change == LineChange.Same && row.mine?.trim() != row.theirs?.trim()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = row.theirs.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = BueffelColors.TextMuted,
                maxLines = 1,
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            )
        }
        Spacer(Modifier.height(6.dp))
        Caption(text = markLabel(mark), color = markColour(mark))
    }
}

private fun LineMark.next(): LineMark =
    when (this) {
        LineMark.Right -> LineMark.Syntax
        LineMark.Syntax -> LineMark.Semantic
        LineMark.Semantic -> LineMark.Right
    }

private fun markLabel(mark: LineMark): String =
    when (mark) {
        LineMark.Right -> "richtig"
        LineMark.Syntax -> "Syntax −0,25"
        LineMark.Semantic -> "Semantik −0,5"
    }

private fun markColour(mark: LineMark): Color =
    when (mark) {
        LineMark.Right -> BueffelColors.Correct
        LineMark.Syntax -> BueffelColors.Almost
        LineMark.Semantic -> BueffelColors.Wrong
    }
