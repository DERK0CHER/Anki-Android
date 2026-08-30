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
