# Roadmap

Die zehn Punkte in der Reihenfolge, in der sie gestellt wurden — Reihenfolge ist Priorität.
Erledigtes steht mit dabei, damit klar ist, worauf aufgesetzt wird.

Stand: Kopf von `claude/anki-multiple-choice-buttons-xss2q6`. Was hier als erledigt steht, ist
gebaut und durch Tests belegt; was als
**ungeprüft** markiert ist, ließ sich in der Cloud-Session nicht verifizieren (kein Gerät, kein
Emulator, kein Ton) und ist beim ersten echten Lauf zu kontrollieren.

---

## 1. Code-Editor-Karte — **erledigt**

> Vorderseite: Aufgabentext plus Funktionssignatur oder Rumpf mit Lücke (`>>> Hier fehlt was`).
> Rückseite: mehrzeiliges Monospace-Eingabefeld, Tab-Einrückung, Autokorrektur und
> Auto-Capitalisierung hart abgeschaltet, Sonderzeichenleiste
> (`{ } ( ) [ ] ; * & -> == != < >`). Nach Absenden: Musterlösung daneben, zeilenweiser Diff mit
> farbiger Markierung. Selbstbewertung pro Zeile: richtig / Syntaxfehler (−0,25) / Semantikfehler
> (−0,5), App rechnet Punkte wie in der Klausur.

`ui/CodeRound.kt`, `domain/LineDiff.kt`, `domain/Marking.kt`, `model/Task.kt`.

- Lücke ist `CodeTask.GAP` = `>>> Hier fehlt was`.
- Tastatur: `KeyboardCapitalization.None`, `autoCorrectEnabled = false`,
  `KeyboardType.Ascii` — alle drei, weil einzeln keine reicht.
- Tab: `⇥` in der Leiste fügt vier Leerzeichen ein. Zusätzlich übernimmt Return die Einrückung
  der Vorzeile (`autoIndent`), was in der Praxis mehr bringt als die Tab-Taste.
- Leiste enthält über die gewünschten hinaus `= " %` und das `⇥`.
- Diff ist LCS, nicht Index gegen Index: eine vergessene Zeile kostet **eine** Diff-Zeile, nicht
  alle danach. Einrückung zählt beim Vergleich nicht mit, wird aber angezeigt.
- Bewertung durch Antippen der Zeile, zyklisch richtig → Syntax → Semantik. Vorbelegt: passende
  Zeilen `richtig`, alles andere `Semantik` — die ehrliche Voreinstellung, Hochreden soll die
  bewusste Handlung sein.
- Punkte: jede Zeile der Musterlösung ist einen Punkt wert, Abzüge 0,25 / 0,5, Anzeige `8,25 / 10`.
- **Entscheidung, die du kippen kannst:** als *richtig* für die Box zählt nur ein Durchgang
  **ohne jeden Abzug**. Ein −0,25 in zehn Zeilen halbiert also die Box. `Marking.clean`.

## 2. Zeilen-Sortieren-Modus — **erledigt**

> Musterlösung in Zeilen zerlegt, gemischt, per Drag in Reihenfolge bringen. Die App soll eine
> Karte automatisch vom Sortier- in den Schreibmodus hochstufen, sobald sie zweimal fehlerfrei
> sortiert wurde.

`ui/SortRound.kt`, `Card.mode`, `Card.SORTS_TO_WRITE = 2`, `StudySession.sorted(clean)`.

- Greifen per Langdruck, Zeilen tauschen sobald der Finger eine Zeilenhöhe überschritten hat.
- Zeilen haben feste Höhe (`ROW_HEIGHT = 46.dp`) und scrollen seitwärts statt umzubrechen —
  unterschiedlich hohe Zeilen würden die Index-Rechnung des Drags kaputt machen.
- Eine vermurkste Sortierung setzt den Zähler auf 0 zurück.
- **Ungeprüft:** die Drag-Geste selbst. Screenshots sind Standbilder.

## 3. Tipp-Antwort mit exaktem Vergleich — **halb erledigt**

> Normalisiert Whitespace, sonst exakt. Alternative Musterlösungen pro Karte erlaubt
> (`d=[3 6 2 5 9]'` und `d=[3;6;2;5;9]`).

Die Alternativen gibt es schon: `CodeTask.alternatives`, `alt:` im Importformat, verglichen wird
gegen die passendste. `LineDiff.same` normalisiert Whitespace und Leerzeilen.

Was fehlt: ein eigener **Einzeiler-Modus** ohne Diff und ohne Selbstbewertung — einzeiliges Feld,
exakter Vergleich, sofort richtig/falsch. Aktuell landet ein Einzeiler im vollen Editor mit
Zeilenbewertung, was für `d=[3;6;2;5;9]` Overkill ist. Vorschlag: `CardMode.Type` als vierter
Modus, ausgelöst wenn `solutionLines.size == 1`.

## 4. Generator-Karten für Zahlensysteme — **offen**

> Kartentyp mit Parametern (Basis von, Basis nach, Bitbreite, Operation AND/OR/XOR/Shift), App
> würfelt Operanden bei jedem Aufruf neu und berechnet die Lösung selbst. Gleiches für
> `printf("%4x", a*b)`-Ausgaben.

Braucht einen dritten `Task`-Typ, z. B. `GeneratedTask(kind, params)`, der bei jedem Aufruf
Operanden würfelt und die Lösung selbst rechnet. Wichtig fürs Datenmodell: eine solche Karte hat
keine feste Lösung, der Fortschritt hängt also an der Karte, nicht an der Instanz — `Card.box`
passt weiter, aber der Vergleich muss die gewürfelte Instanz kennen.

## 5. Trace-Karten — **offen**

> Programm vorne, Ausgabe wird getippt. Zusätzlich ein Single-Choice-Modus mit genau drei
> Optionen, weil die Klausur so aussieht.

Die Ausgabe-tippen-Hälfte ist Punkt 3 mit Programm auf der Vorderseite. Drei-Optionen-SC ist der
bestehende `Question` mit `answers.size == 3` — funktioniert heute schon, es fehlt nur der
Prompt/Import-Hinweis, dass genau drei gewollt sind.

## 6. Bild auf Vorder- und Rückseite — **offen**

> Damit "Activity Chart → C" (Bild vorne, Editor hinten) und "C → Activity Chart" (Code vorne,
> Lösungsbild hinten, Zeichnen auf Papier, Selbstbewertung) funktionieren.

Braucht Bilder im Speicher. Vorschlag: Bilder als Dateien in `filesDir/images/`, in der Karte nur
der Dateiname; im Importformat `image:` bzw. `backimage:` mit einem Pfad relativ zur Kartendatei.
Das ist der erste Punkt, der den Import von der Zwischenablage auf echte Dateien zwingt — siehe
Punkt 10.

## 7. Klausurmodus — **offen**

> Gewichtete Ziehung nach Subsection (z. B. 25 SC, 4 Theorie, 5 Programmieraufgaben, 20 MATLAB),
> 120-Minuten-Countdown, kein Umdrehen bis zur Abgabe, Auswertung mit Punkten pro Block.

Umgeht `StudySession` komplett: eigene Ziehung, kein Box-System, keine Runden. Sinnvoll als
zweite Domänenklasse neben `StudySession` statt als Flag darin.

## 8. Spaced Repetition — **offen**

> FSRS oder SM-2 mit Statistik pro Subsection, Leech-Erkennung (Karte 5× falsch → separate
> Liste), und Zeit pro Karte protokollieren.

Ersetzt `StudySession.GAPS` und die Runden-Logik durch echte Intervalle mit Datum. Das ist der
größte Eingriff der Liste: aktuell ist alles sitzungslokal und kennt keine Uhr. Braucht
`Card.due`, `Card.stability`, `Card.lapses` und eine Historie pro Antwort.

## 9. Tags als Filter — **halb erledigt**

> Klausurjahr und Aufgabentyp, damit "alle Node_Delete-Varianten" oder "alles aus WS24"
> filterbar ist.

`Task.tags` existiert, wird importiert (`tags: WS24, Node_Delete`) und gespeichert. Was fehlt: die
Filter-UI und eine Lernsitzung über eine Tag-Auswahl statt über einen Bereich.

## 10. Import aus Markdown/CSV — **halb erledigt**

> Ich schreibe die Karten am Desktop, nicht auf dem Handy. Eine Datei pro Subsection, Karten
> getrennt durch `---`, Felder `type:`, `front:`, `back:`, `alt:`, `tags:`.

Das Format ist gebaut und getestet (`importer/CardFileParser.kt`), inklusive `topic:`, Codeblöcken
mit Fences und `---`, das innerhalb eines Blocks nicht trennt. `importer/CardImport.kt` erkennt
selbst, ob Kartendatei oder JSON in der Zwischenablage liegt. Format steht im README.

Was fehlt: **Einlesen von der Platte** statt über die Zwischenablage. Der Dateidialog ist für
`sichern`/`laden` schon verdrahtet (`ActivityResultContracts.OpenDocument` in `MainActivity`) —
dieselbe Mechanik auf den Import zu legen ist wenig Arbeit und nimmt das Copy-Paste-Gefummel raus.
CSV ist bewusst nicht gebaut: Code in CSV-Zellen ist dasselbe Escaping-Problem wie in JSON.

---

## Was sonst noch ungeprüft ist

- **Töne** (`audio/Feedback.kt`): synthetisierte Sinustöne, aufsteigende Quinte für richtig,
  fallende Terz für falsch. Frequenzen und Längen sind vier Zahlen in `RIGHT`/`WRONG`. Dass die
  Lautstärketasten sie erreichen, ist über `USAGE_MEDIA` plus `volumeControlStream` gelöst — die
  Ursache ist belegt, die Wirkung nicht.
- **Farbblitz** (`FLASH_PEAK = 0.16f`, `FLASH_IN = 110`, `FLASH_OUT = 420` in `StudyScreen.kt`).
  Roborazzi fotografiert erst nach Ende der Animation, der Blitz ist auf keinem Screenshot.
- **Tastatur-Insets** im Import-Screen: `safeDrawingPadding()` statt gestapeltem
  `systemBarsPadding() + imePadding()`, plus `adjustResize` im Manifest.

## Lokal weiterarbeiten

```sh
cd bueffel
./gradlew :app:assembleDebug          # APK
./gradlew :app:testDebugUnitTest      # Tests
./gradlew :app:recordRoborazziDebug   # Screens nach bueffel/screenshots/ rendern
```

Es gibt keinen Emulator in der CI, deshalb sind die Screenshots die einzige Sichtprüfung dort —
lokal mit Gerät ist das überflüssig, aber die Bilder sind der Grund, warum mehrere Layoutfehler
gefunden wurden, bevor sie auf ein Handy kamen.

ktlint läuft nicht als Gradle-Task; Stil wurde mit einer heruntergeladenen ktlint-1.8.0-JAR
geprüft. Wenn du es dauerhaft willst, lohnt das ktlint-Gradle-Plugin.

## Entscheidungen, die auf Widerspruch warten

- Kein Abzug = richtig (Punkt 1). Eventuell zu streng.
- `ROUNDS = listOf(4, 6, 8)` und `CURVE = 1.25` — die Kurve legt 4× richtig auf 60 %, 6× auf 82 %.
- `WORKING_SET = 12` Fragen in Rotation, `NEW_EVERY = 4` Antworten bis eine neue eingemischt wird.
  Die Gap-Leiter läuft bis 50, aber alles über 11 ist durch die Rotationsgröße gekappt — die
  hinteren Werte sind Absichtserklärung, kein Abstand.
- Sichern/Laden **merged** statt zu überschreiben, damit ein Fehlgriff die App nicht leerräumt.
