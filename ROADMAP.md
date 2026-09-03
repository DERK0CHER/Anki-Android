# Roadmap

Die zehn Punkte in der Reihenfolge, in der sie gestellt wurden — Reihenfolge ist Priorität.
Erledigtes steht mit dabei, damit klar ist, worauf aufgesetzt wird.

Stand: Kopf von `claude/anki-multiple-choice-buttons-xss2q6`. Was hier als erledigt steht, ist
gebaut und durch Tests belegt; was als **ungeprüft** markiert ist, ließ sich ohne Gerät nicht
verifizieren (kein Emulator, kein Ton) und ist beim ersten echten Lauf zu kontrollieren.

| Punkt | Stand |
| --- | --- |
| 1 Code-Editor-Karte | erledigt |
| 2 Zeilen-Sortieren + Hochstufung | erledigt |
| 3 Tipp-Antwort exakt | erledigt |
| 4 Generator-Karten | erledigt |
| 5 Trace-Karten | erledigt |
| 6 Bilder | offen |
| 7 Klausurmodus | offen |
| 8 FSRS/SM-2 | offen |
| 9 Tags als Filter | erledigt |
| 10 Import aus Datei | erledigt, CSV bewusst nicht |

---

## 1. Code-Editor-Karte — **erledigt**

> Vorderseite: Aufgabentext plus Funktionssignatur oder Rumpf mit Lücke (`>>> Hier fehlt was`).
> Rückseite: mehrzeiliges Monospace-Eingabefeld, Tab-Einrückung, Autokorrektur und
> Auto-Capitalisierung hart abgeschaltet, Sonderzeichenleiste
> (`{ } ( ) [ ] ; * & -> == != < >`). Nach Absenden: Musterlösung daneben, zeilenweiser Diff mit
> farbiger Markierung. Selbstbewertung pro Zeile: richtig / Syntaxfehler (−0,25) / Semantikfehler
> (−0,5), App rechnet Punkte wie in der Klausur.

`ui/CodeRound.kt`, `ui/CodeParts.kt`, `domain/LineDiff.kt`, `domain/Marking.kt`, `model/Task.kt`.

- Lücke ist `CodeTask.GAP` = `>>> Hier fehlt was`.
- Tastatur: `KeyboardCapitalization.None`, `autoCorrectEnabled = false`,
  `KeyboardType.Ascii` — alle drei, weil einzeln keine reicht.
- Tab: `⇥` in der Leiste fügt vier Leerzeichen ein. Zusätzlich übernimmt Return die Einrückung
  der Vorzeile (`autoIndent`), was in der Praxis mehr bringt als die Tab-Taste.
- Diff ist LCS, nicht Index gegen Index: eine vergessene Zeile kostet **eine** Diff-Zeile.
- Bewertung durch Antippen der Zeile, zyklisch richtig → Syntax → Semantik. Vorbelegt: passende
  Zeilen `richtig`, alles andere `Semantik`.
- Punkte: jede Zeile der Musterlösung ist einen Punkt wert, Anzeige `8,25 / 10`.
- **Entscheidung, die du kippen kannst:** als *richtig* für die Box zählt nur ein Durchgang
  **ohne jeden Abzug**. Ein −0,25 in zehn Zeilen halbiert also die Box. `Marking.clean`.
- Die Vorderseite ist inzwischen zweigeteilt: `prompt` ist Prosa, `given` ist Code und wird in
  Monospace gesetzt (`ui/CodeParts.kt`, `GivenCode`). Das war der Layoutfehler aus den
  Screenshots.

## 2. Zeilen-Sortieren-Modus — **erledigt**

> Musterlösung in Zeilen zerlegt, gemischt, per Drag in Reihenfolge bringen. Die App soll eine
> Karte automatisch vom Sortier- in den Schreibmodus hochstufen, sobald sie zweimal fehlerfrei
> sortiert wurde.

`ui/SortRound.kt`, `Card.mode`, `Card.SORTS_TO_WRITE = 2`, `StudySession.sorted(clean)`.

- Greifen per Langdruck, Zeilen tauschen sobald der Finger eine Zeilenhöhe überschritten hat.
- Zeilen haben feste Höhe (`ROW_HEIGHT = 46.dp`) und scrollen seitwärts statt umzubrechen.
- Eine vermurkste Sortierung setzt den Zähler auf 0 zurück.
- **Ungeprüft:** die Drag-Geste selbst. Screenshots sind Standbilder.

## 3. Tipp-Antwort mit exaktem Vergleich — **erledigt**

> Normalisiert Whitespace, sonst exakt. Alternative Musterlösungen pro Karte erlaubt
> (`d=[3 6 2 5 9]'` und `d=[3;6;2;5;9]`).

`ui/TypeRound.kt`, `LineDiff.sameLine`, `CardMode.Type`, `CodeTask.isOneLiner`.

- Eine Karte, deren Musterlösung **eine Zeile** ist, landet im Einzeiler-Modus statt im Editor:
  ein Feld, kein Diff, keine Selbstbewertung — die App entscheidet.
- Normalisierung: Whitespace neben allem, was kein Buchstabe und keine Ziffer ist, fällt weg
  (`d=[3;6]` = `d = [3;6]`). Zwischen zwei Wortzeichen bleibt er stehen, weil er dort trennt
  (`int a` ≠ `inta`, `[3 6]` ≠ `[36]`). Groß- und Kleinschreibung zählt.
- Alternativen weiter über `alt:`.
- Screenshots: `11-type-line.png`, `12-type-wrong.png`.

## 4. Generator-Karten für Zahlensysteme — **erledigt**

> Kartentyp mit Parametern (Basis von, Basis nach, Bitbreite, Operation AND/OR/XOR/Shift), App
> würfelt Operanden bei jedem Aufruf neu und berechnet die Lösung selbst. Gleiches für
> `printf("%4x", a*b)`-Ausgaben.

`model/Generated.kt`, `domain/Generator.kt`, `ui/GeneratedRound.kt`, `CardMode.Generate`.

- Drei Sorten: `convert` (Basis → Basis), `bits` (zwei Zahlen, ein Operator), `printf`.
- Importformat: `type: gen` mit `kind:`, `op:`, `from:`, `to:`, `bits:`, `format:`. Steht im
  README, inklusive Beispielen.
- Die Zahlen kommen aus einem **Seed**, nicht aus der Uhr: der Seed ist die Rundennummer. Sonst
  würfelt jeder Frame neu und die Frage ändert sich unter dem Finger. Nebeneffekt: die
  Screenshots sind zwischen zwei Läufen vergleichbar.
- Antwortvergleich als **Zahl**, wenn sie als Zahl lesbar ist: `0f`, `f`, `0x0F`, `0000 1111`
  sind dieselbe Antwort. Nur bei `printf` zählt der Text, weil dort die Nullen die Aufgabe sind
  — führende und schließende Leerzeichen einer Breitenangabe (`%4x`) werden aber verziehen, die
  sind auf einer Telefontastatur nicht zumutbar.
- Fortschritt hängt an der Karte, nicht an der Instanz: die Box zählt, wie oft diese *Sorte*
  Aufgabe gerechnet wurde.
- Screenshot: `15-generated.png`.

## 5. Trace-Karten — **erledigt**

> Programm vorne, Ausgabe wird getippt. Zusätzlich ein Single-Choice-Modus mit genau drei
> Optionen, weil die Klausur so aussieht.

Fällt aus 1, 3 und der Vorderseitentrennung heraus:

- Drei-Optionen-SC: `type: choice` mit einem Codeblock unter `front:`. Der Block ist jetzt
  `given` und wird in Monospace gesetzt — vorher war ein Programm auf einer Multiple-Choice-Karte
  in der Fließtextschrift, also unlesbar. Screenshot: `13-trace.png`.
- Ausgabe tippen: `type: code` mit dem Programm unter `front:` und der Ausgabe als `back:`.
  Einzeilige Ausgabe landet automatisch im Einzeiler-Modus.

## 6. Bild auf Vorder- und Rückseite — **offen**

> Damit "Activity Chart → C" (Bild vorne, Editor hinten) und "C → Activity Chart" (Code vorne,
> Lösungsbild hinten, Zeichnen auf Papier, Selbstbewertung) funktionieren.

Braucht Bilder im Speicher. Vorschlag, der zum jetzigen Stand passt:

- `Task.image` für die Vorderseite — damit können Choice-, Code- und Generator-Karten alle ein
  Bild tragen, ohne einen neuen Typ.
- Ein neuer Typ für die andere Richtung: Vorderseite normal, Lösung ist ein Bild (oder Prosa),
  Selbstbewertung „konnte ich / konnte ich nicht". Das ist zugleich der fehlende schlichte
  Karteikartenmodus.
- Dateien nach `filesDir/images/`, in der Karte steht nur der Dateiname. Der relative Pfad aus
  der ursprünglichen Idee geht **nicht**: der Dateidialog gibt eine `content://`-URI zurück, aus
  der sich das Nachbarverzeichnis nicht ableiten lässt. Also entweder Bilder getrennt einlesen
  (Mehrfachauswahl im Import-Screen) oder mit `OpenDocumentTree` gleich einen Ordner wählen.
  Getrennt einlesen ist weniger Code und robuster.

## 7. Klausurmodus — **offen**

> Gewichtete Ziehung nach Subsection (z. B. 25 SC, 4 Theorie, 5 Programmieraufgaben, 20 MATLAB),
> 120-Minuten-Countdown, kein Umdrehen bis zur Abgabe, Auswertung mit Punkten pro Block.

Umgeht `StudySession` komplett: eigene Ziehung, kein Box-System, keine Runden. Sinnvoll als
zweite Domänenklasse neben `StudySession` statt als Flag darin. Die Punkte pro Block gibt es
schon in `Marking`; für Multiple Choice fehlt eine Punktzahl pro Frage.

## 8. Spaced Repetition — **offen**

> FSRS oder SM-2 mit Statistik pro Subsection, Leech-Erkennung (Karte 5× falsch → separate
> Liste), und Zeit pro Karte protokollieren.

Ersetzt `StudySession.GAPS` und die Runden-Logik durch echte Intervalle mit Datum. Das ist der
größte Eingriff der Liste: aktuell ist alles sitzungslokal und kennt keine Uhr. Braucht
`Card.due`, `Card.stability`, `Card.lapses` und eine Historie pro Antwort — und damit auch eine
Speicherversion 7 und eine Entscheidung, was mit den bestehenden Boxen passiert.

## 9. Tags als Filter — **erledigt**

> Klausurjahr und Aufgabentyp, damit "alle Node_Delete-Varianten" oder "alles aus WS24"
> filterbar ist.

`Deck.tags`, `Deck.cardsTagged`, `ui/SubtopicScreen.kt`, `Screen.Study.tags`.

- Die Tags stehen unter den Bereichen auf demselben Screen; Auswahl macht aus dem Knopf darunter
  eine Sitzung über genau diese Karten.
- **Zwei gewählte Tags heißen „beide", nicht „eins von beiden".** WS24 + Node_Delete ist die
  gemeinte Verengung; zwei Jahre gleichzeitig ergeben null Karten, und das sagt der Knopf vorher
  an statt hinterher. Falls dir „oder" lieber ist: `Deck.cardsTagged`, ein Wort.
- Ein Thema mit nur einem Bereich öffnet jetzt den Bereichs-Screen, **wenn** es Tags hat — sonst
  wäre der Filter für genau die Sets unerreichbar, die am ehesten welche tragen.
- Screenshot: `14-tags.png`.

## 10. Import aus Markdown/CSV — **erledigt** (CSV bewusst nicht)

> Ich schreibe die Karten am Desktop, nicht auf dem Handy. Eine Datei pro Subsection, Karten
> getrennt durch `---`, Felder `type:`, `front:`, `back:`, `alt:`, `tags:`.

`importer/CardFileParser.kt`, `importer/CardImport.kt`, `ui/ImportScreen.kt`, `MainActivity`.

- Einlesen jetzt auch **aus einer Datei** über denselben Dateidialog wie Sichern/Laden. Der
  Catch-all-MIME-Typ steht mit in der Liste, weil eine `.txt` vom Desktop oft als
  `application/octet-stream` ankommt und sonst ausgegraut wäre.
- Zwischenablage bleibt, beide Wege enden im selben Parser und in derselben Vorschau.
- CSV ist bewusst nicht gebaut: Code in CSV-Zellen ist dasselbe Escaping-Problem wie in JSON.

---

## Was noch ungeprüft ist

- **Drag-Geste** im Sortiermodus (`ui/SortRound.kt`, `ROW_HEIGHT`, `rowPx`).
- **Töne** (`audio/Feedback.kt`): synthetisierte Sinustöne, aufsteigende Quinte für richtig,
  fallende Terz für falsch. Frequenzen und Längen sind vier Zahlen in `RIGHT`/`WRONG`. Dass die
  Lautstärketasten sie erreichen, ist über `USAGE_MEDIA` plus `volumeControlStream` gelöst — die
  Ursache ist belegt, die Wirkung nicht.
- **Farbblitz** (`FLASH_PEAK = 0.16f`, `FLASH_IN = 110`, `FLASH_OUT = 420` in `StudyScreen.kt`).
  Roborazzi fotografiert erst nach Ende der Animation.
- **Tastatur-Insets** im Import-Screen und in den beiden Tippmodi.
- **Der Dateidialog** beim Import: dass die Datei ankommt, ist Code; dass dein Dateimanager sie
  anbietet, ist Gerätesache.

## Speicherversionen

`data/DeckStore.kt`, Konstante `VERSION`. 1 flache Kartenliste, 2 Bereiche, 3 Kartentypen,
4 Code auf der Vorderseite getrennt, 5 Generator-Karten. Alte Dateien werden weiter gelesen; eine
Karte aus Version ≤ 3 trägt ihre ganze Vorderseite im `prompt` und wird als Prosa gesetzt, bis
die Kartendatei neu importiert wird.

## Lokal weiterarbeiten

```sh
cd bueffel
./gradlew :app:assembleDebug          # APK
./gradlew :app:testDebugUnitTest      # Tests
./gradlew :app:recordRoborazziDebug   # Screens nach bueffel/screenshots/ rendern
```

Es gibt keinen Emulator in der CI, deshalb sind die Screenshots die einzige Sichtprüfung dort.
ktlint läuft nicht als Gradle-Task; Stil wurde von Hand nachgezogen. Wenn du es dauerhaft willst,
lohnt das ktlint-Gradle-Plugin.

## Entscheidungen, die auf Widerspruch warten

- Kein Abzug = richtig (Punkt 1). Eventuell zu streng.
- Zwei Tags heißen „beide" (Punkt 9).
- Einzeiler kommen nie in den Sortiermodus, auch nicht am Anfang (Punkt 3).
- `ROUNDS = listOf(4, 6, 8)` und `CURVE = 1.25` — die Kurve legt 4× richtig auf 60 %, 6× auf 82 %.
- `WORKING_SET = 12` Fragen in Rotation, `NEW_EVERY = 4` Antworten bis eine neue eingemischt wird.
- Sichern/Laden **merged** statt zu überschreiben, damit ein Fehlgriff die App nicht leerräumt.
