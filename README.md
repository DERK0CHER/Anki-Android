# Büffel

Ein Multiple-Choice-Lerntrainer für Android. Eine Frage, ein paar Antwort-Pills, ein Tipp —
mehr steht nie auf dem Schirm.

## Wie gelernt wird

Jede Frage sitzt in einer von acht Boxen. Eine richtige Antwort schiebt sie eine Box weiter,
eine falsche **halbiert** den Stand, statt ihn zu löschen — ein Patzer auf dem achten Durchgang
soll nicht sieben Durchgänge Arbeit wegwerfen. Nach acht richtigen Antworten in Folge fällt die
Frage aus der Rotation.

Wiederkehrende Fragen werden umso weiter nach hinten einsortiert, je weiter sie sind, damit
immer genug andere Fragen dazwischen kommen. Die Reihenfolge der Antwort-Pills wird bei jedem
Durchgang neu gewürfelt: bei fester Reihenfolge merkt man sich „die zweite von oben" statt der
Antwort.

Das **Lern-O-Meter** zeigt den Stand als eine Leiste, deren Verlauf von Rot über Amber nach
Hellgrün läuft. Die Farbe an der Spitze ist die Aussage.

## Kartentypen

**Multiple Choice** — Frage, zwei bis vier Antworten, eine richtig.

**Code schreiben** — vorne die Aufgabe, meist eine Signatur oder ein Rumpf mit `>>> Hier fehlt was`
an der Lücke; hinten die Musterlösung. Eine solche Karte wird in zwei Stufen gelernt:

1. **Sortieren.** Die Zeilen der Musterlösung kommen gemischt, du ziehst sie in die richtige
   Reihenfolge. Das ist die Vorstufe zum freien Schreiben — die Bausteine sind da, nur die
   Reihenfolge fehlt.
2. **Schreiben.** Nach zwei fehlerfreien Sortierungen stuft die App die Karte hoch: mehrzeiliges
   Monospace-Feld, Autokorrektur und Auto-Großschreibung aus, Sonderzeichenleiste
   (`⇥ { } ( ) [ ] ; * & -> == != < > = " %`), Return übernimmt die Einrückung der Vorzeile.
   Nach dem Abgeben wird zeilenweise gegen die Musterlösung verglichen — eine vergessene Zeile
   kostet **eine** Zeile im Diff, nicht alle danach.

Bewertet wird pro Zeile selbst, mit den Sätzen der Klausur: **richtig**, **Syntax −0,25**,
**Semantik −0,5**. Zeile antippen wechselt durch. Die App rechnet die Punkte, und nur ein
Durchgang **ohne jeden Abzug** zählt als richtige Antwort für die Box. Warum selbst bewerten: kein
Textvergleich kann eine umbenannte Variable von einer falschen unterscheiden — die App würde
entweder echte Fehler durchwinken oder korrekten Code an einem Leerzeichen scheitern lassen.

`alt:` erlaubt mehrere gültige Musterlösungen pro Karte; verglichen wird gegen die, die am besten
passt.

## Fragen hineinbekommen

Die Fragen schreibt eine KI. Der Import-Screen gibt einen fertigen Prompt zum Kopieren aus und
liest die Antwort aus der Zwischenablage wieder ein. Erwartet wird JSON:

```json
[
  {
    "question": "Wie verhältst du dich bei einer Panne auf der Autobahn?",
    "answers": ["Warnblinkanlage einschalten", "Auf der Fahrbahn winken"],
    "correct": 0
  }
]
```

`correct` darf der Index ab null oder der Text der richtigen Antwort sein. Prosa mit `A)`/`1.`
und einer `Lösung:`-Zeile wird ebenfalls gelesen, falls das Modell die Vorgabe ignoriert. Was
nicht lesbar ist, wird übersprungen und gezählt, nicht stillschweigend verschluckt.

### Kartendatei für Code

Code übersteht das Von-Hand-Schreiben in JSON nicht — jede Klammer müsste escaped und jeder
Zeilenumbruch zu `\n` werden. Dafür gibt es ein Textformat mit Codeblöcken, das du am Desktop
schreibst und über dieselbe Zwischenablage einliest:

    type: code
    topic: Verkettete Listen
    tags: WS24, Node_Delete
    front:
    ```c
    void node_delete(node_t *n) {
    >>> Hier fehlt was
    }
    ```
    back:
    ```c
        free(n->data);
        free(n);
    ```
    ---
    type: choice
    front: Was ergibt 1 << 3?
    - 4
    - *8
    - 16

Regeln:

- Karten trennt eine Zeile mit nur `---`. Innerhalb eines Codeblocks trennt `---` nicht.
- Ein Feldwert ist der Rest der Zeile — oder, wenn der leer ist, der Codeblock darunter.
  Einrückung im Block bleibt erhalten.
- Bei `choice` sind die Antworten Zeilen mit `- `, die richtige mit `*` markiert.
- `alt:` darf mehrfach vorkommen.
- `type:` darf fehlen: mit `back:` ist es Code, mit Antwortzeilen Multiple Choice.
- Die App erkennt selbst, ob in der Zwischenablage eine Kartendatei oder JSON liegt.

## Bauen

```sh
cd bueffel
./gradlew :app:assembleDebug
```

Es gibt keinen Emulator in dieser CI, also werden die Screens stattdessen mit Roborazzi in
einem JVM-Test gezeichnet und die Bilder nach `bueffel/screenshots/` eingecheckt:

```sh
cd bueffel
./gradlew :app:recordRoborazziDebug
```

Fertige APKs hängen an der rollenden Vorabversion `bueffel-latest`.
