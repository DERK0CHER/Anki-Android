package net.bueffel.model

/** One answer a question offers */
data class Choice(
    val label: String,
    val text: String,
)

/**
 * A multiple choice question.
 *
 * @param correctIndex index into [choices] of the single right answer
 */
data class Question(
    val prompt: String,
    val choices: List<Choice>,
    val correctIndex: Int,
) {
    val correctChoice: Choice get() = choices[correctIndex]
}

/** Sample content, so the study screen can be looked at before decks can be imported */
object SampleQuestions {
    val all =
        listOf(
            Question(
                prompt = "Wie verhältst du dich bei einer Panne auf der Autobahn?",
                choices =
                    listOf(
                        Choice("A", "Warnblinkanlage einschalten und Warnweste anlegen"),
                        Choice("B", "Auf der Fahrbahn stehen bleiben und winken"),
                        Choice("C", "Das Fahrzeug verlassen und auf dem Standstreifen warten"),
                        Choice("D", "Den Motor laufen lassen und im Auto sitzen bleiben"),
                    ),
                correctIndex = 0,
            ),
            Question(
                prompt = "Was bedeutet ein durchgezogener Mittelstreifen?",
                choices =
                    listOf(
                        Choice("A", "Überholen ist erlaubt"),
                        Choice("B", "Er darf nicht überfahren werden"),
                        Choice("C", "Er markiert eine Baustelle"),
                    ),
                correctIndex = 1,
            ),
            Question(
                prompt = "Wie groß ist der Sicherheitsabstand innerorts bei 50 km/h ungefähr?",
                choices =
                    listOf(
                        Choice("A", "Ein halber Tachowert in Metern"),
                        Choice("B", "Zwei Fahrzeuglängen"),
                        Choice("C", "15 Meter"),
                        Choice("D", "Der Weg von einer Sekunde"),
                    ),
                correctIndex = 0,
            ),
        )
}
