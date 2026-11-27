# Strategieprüfstände

Der Prüfstand spielt ganze Partien mit den echten Rundenregeln von Klassisch (`classic/1.0.0`) und
Erweitert (technisch `spaceward/2.0.0`), ohne Spring, Datenbank oder Browser, und vergleicht
Strategieprofile und Computerstärken. Bedienung und Profile stehen im
[README](../README.md#strategy-experiments).

Die ersten Serien vom 05. und 06.10.2026 untersuchten Regelstände, die es nicht mehr gibt (die
früheren erweiterten Fassungen 1.x mit Wirtschaft und Forschung). Sie stehen in der Git-Historie
dieser Datei (bis Commit `7f6b803`). Geblieben sind daraus die Strategieprofile des Prüfstands und die
Basisstrategie BASELINE der Spiel-KI.

## Computerstärke und Zufallsstufen klassisch (K4) — 25.11.2026

Kalibriert werden die Stufen des Dialogs „Neue Partie“ für **Klassisch**. Erweitert behält
seine Vorschlagswerte bis Phase E.

### Aufbau

Neuer Modus `--mode=strength` (README, Strategy experiments). Er spielt Lobby-Partien statt
der synthetischen Prüfstandkarten: dieselbe Erzeugung wie eine neue Partie (`GalaxyGenesis`,
aus `DefaultGameRegistry` herausgelöst), Startwerte 4/8 mit Startanteil der Computer,
Glockenkurve bis 10, Startausgleich, Ereignisse und Angreifer-Reihenfolge der Zufallsstufe,
Sieg bei 70 %, Grenze 300 Runden, Kartenform Gleichmäßig. Sitz 1 ist der Mensch; ihn spielt
ein festes Referenzprofil, die Computer spielen die Profile, die ihre Stärke zieht.
Abgelehnte Sendungen (etwa an eine zerstörte Sonne) verfallen wie im Spiel.

**Referenz BASELINE.** Sie ist die Spiel-KI seit dem ersten Stand, spielt ohne
Sonderwissen und ohne Neigung zu einer Partieform (im Zweikampf mittelmäßig, mit mehreren
Reichen das stärkste Profil). Ein Mensch wird im Zweikampf eher besser, mit mehreren
Reichen eher anders spielen; die Quoten messen daher den Abstand der Stufen, sie sagen
nicht voraus, wie oft ein bestimmter Mensch gewinnt. Intervalle: Wilson 95 %, jede Partie
als unabhängig gezählt (jede Karte kommt je Zelle einmal vor).

Messfehler im Prüfstand behoben: `BaselinePolicy` rief die Spiel-KI auf, die für einen
Sitz mit gespeichertem anderem Profil dieses Profil spielte. Der Sitz plant jetzt immer mit
der Basisstrategie. Ältere Serien betrifft das nicht; ihre Karten speicherten kein Profil.

### Entwicklung (Karten 1–150, Mittel und Groß, Zufall Wenig)

Je Zelle 150 Partien, Computerprofile und Startanteil fest vorgegeben
(`--computer-profiles`, `--start-share`), noch ohne Auslosung der Heimat (unten).
Quote = Siege der Referenz.

| Spieler | Computer | Startanteil | Mittel 24 | Groß 40 |
|---:|---|---|---|---|
| 2 | BASELINE | 0,5 / 0,75 / 1,0 / 1,25 / 1,75 | 82 / 68 / 52 / 44 / 30 % | 69 / 55 / 49 / 45 / 37 % |
| 2 | RUSH oder EXPANSION | 0,75 / 1,0 / 1,1 | 33 / 24 / 22 % | 3 / 1 / 1 % |
| 2 | vier Profile (bisher Normal) | 0,75 / 1,0 | 41 / 29 % | 9 / 9 % |
| 2 | DEFENSE | 1,0 | 70 % | 35 % |
| 2 | BASELINE oder DEFENSE | 1,0 | 59 % | 43 % |
| 2 | BASELINE oder RUSH | 1,25 / 1,5 / 1,75 | 29 / 23 / 19 % | 23 / 19 / 19 % |
| 4 | BASELINE | 0,5 / 0,75 / 1,0 / 1,5 / 2,0 | 53 / 41 / 26 / 17 / 9 % | 58 / 43 / 35 / 29 / 27 % |
| 4 | vier Profile | 1,0 / 1,5 | 59 / 53 % | 76 / 69 % |
| 4 | RUSH oder EXPANSION | 1,25 / 1,5 | 49 / 47 % | 58 / 49 % |

Die Rangfolge kippt mit der Spielerzahl (wie schon in den ersten Serien im Oktober). Rush, Expansion und
Konzentration schlagen die Referenz im Zweikampf fast immer, auf Groß unabhängig vom
Startanteil; mit vier Reichen bekämpfen sie sich gegenseitig, und die Referenz gewinnt
mehr als den fairen Anteil. Die bisherige Normal-Mischung war deshalb im Zweikampf zu
schwer und mit mehreren Reichen zu leicht. Nur die Basisstrategie reagiert fein auf den
Startanteil.

**Heimat-Auslosung.** Mit lauter gleichen Computern (Basisstrategie, gleicher Start) gewann
bei drei Reichen Sitz 1 98 von 200 Partien, Sitz 3 nur 19 (Groß: 112 zu 13). Die gierige
Heimatwahl nimmt das erste System zufällig und jedes weitere möglichst weit weg von den
schon gewählten; das zuletzt gewählte liegt am engsten. Sitz 1 ist meist der Gastgeber.
Die Sitze losen ihre Heimat jetzt aus; danach verteilen sich die Siege bei drei gleichen
Reichen auf 58/59/82 (Mittel) bzw. 60/74/63 Partien (Groß).

### Endwerte und Kontrolle (Karten 1001–1200, Zufall Wenig)

| Stärke | Computer | Start (Produktion/Schiffe) |
|---|---|---|
| Einfach | Basisstrategie | ×0,5 (2/4) |
| Normal | Basisstrategie | ×1 (4/8) |
| Anspruchsvoll | Zweikampf: verdeckt Basisstrategie oder Rush; sonst Basisstrategie | ×1,5 (6/12) |

Siegquote der Referenz, je Zelle 200 Partien, in Klammern 95 % (fairer Anteil 50/33/25 %):

| Stärke | Größe | 2 Spieler | 3 Spieler | 4 Spieler |
|---|---|---:|---:|---:|
| Einfach | Mittel | 86 % (81–90) | 45 % (38–51) | 46 % (39–53) |
| Einfach | Groß | 66 % (59–72) | 40 % (33–46) | 43 % (36–49) |
| Normal | Mittel | 52 % (45–59) | 29 % (23–36) | 24 % (19–30) |
| Normal | Groß | 42 % (35–49) | 30 % (24–37) | 27 % (21–34) |
| Anspruchsvoll | Mittel | 23 % (18–29) | 26 % (20–33) | 10 % (7–15) |
| Anspruchsvoll | Groß | 16 % (12–22) | 28 % (22–34) | 16 % (11–21) |

Zusatz mit 80 Karten (3001–3080), Einfach/Normal/Anspruchsvoll: Klein 16 im Zweikampf
90/46/23 %, mit vier Reichen 51/24/13 %; Riesig 64 im Zweikampf 51/30/10 %, mit vier Reichen
33/19/16 %, dort bleiben aber 20–40 % der Partien nach 300 Runden offen.

Entscheidung:

- **Normal** spielt die Basisstrategie mit gleichem Start und liegt damit in jeder Größe
  und Spielerzahl im fairen Bereich. Keine Mischung der anderen Profile schaffte das: sie
  sind im Zweikampf zu stark und mit mehreren Reichen zu schwach. Die verdeckte Vielfalt
  entfällt deshalb in Klassisch. Basisstrategie oder Verteidigung (59/43 %) fiel nach der
  Auslosung auf Groß auf 35 % und wurde verworfen.
- **Einfach** halbiert den Start. Im Zweikampf gewinnt die Referenz 66–90 % (Ziel 70–85 %,
  Mittel knapp darüber, Groß knapp darunter), mit vier Reichen fast den doppelten fairen
  Anteil. Bei drei Reichen ist der Vorsprung kleiner (40–45 % statt 33 %); ein noch
  kleinerer Start (1/2) wäre kaum noch eine Partie.
- **Anspruchsvoll** startet anderthalbfach. Im Zweikampf spielt je zur Hälfte Rush oder die
  Basisstrategie: Rush allein ließ der Referenz auf Groß bei jedem Startanteil nur 0–3 %.
  Mit mehreren Reichen trägt der Start allein den Vorsprung.

Nicht erreicht: **Anspruchsvoll mit drei Reichen** liegt mit 26–28 % nur knapp unter dem
fairen Drittel, also kaum „klar im Vorteil“. Auf Groß mit vier Reichen kam die Referenz in der
Entwicklung auch gegen doppelten Start noch auf 27 %. Deutlich stärker werden die Computer
dort nur mit neuer Planung (etwa gemeinsam gegen den Führenden), die K4 nicht einführt.

### Partielänge und Nachbarsprung

Runden bis zur Entscheidung (Median, Einfach/Normal/Anspruchsvoll): Zweikampf Mittel
80/102/61, Groß 157/157/70; vier Reiche Mittel 88/86/92, Groß 137/147/141. Klein endet nach
50–75 Runden, Riesig erst nach 200–240. Nächster Nachbar je System: 3 Runden bei 68–78 %,
2 bei 10–22 %, 4 bei 9–13 %, Median 3 in jeder Größe. Nächste gegnerische Heimat im
Zweikampf (Median): 13 (Klein), 16 (Mittel), 21 (Groß), 26 Runden (Riesig).

Die Länge kommt vor allem aus der Planung der Basisstrategie (ein Ziel je Runde von der
stärksten Basis), nicht aus der Reisezeit: Auf denselben Karten entscheidet Rush den
Zweikampf auf Groß nach etwa 50 Runden. Die Geschwindigkeit (160 Bildpunkte je Runde,
gemeinsam mit Erweitert) bleibt deshalb. Riesig ist gegen Computer sehr lang; dort hilft
das Rundenlimit in den Details.

### Zufallsstufen

Normal, Mittel 24, 80 Karten (2001–2080), je drei Kampfstartwerte, jede Partie mit und ohne
Ereignisse (gleiche Karte, gleiche Würfel); 240 Partien je Zelle. Werte für zwei / vier Reiche.

| Stufe | Siegquote | Sieger wechselt mit dem Kampfstartwert, ohne Ereignisse | mit Ereignissen | Ereignisse drehen den Sieger | Runden Mittel (2 Reiche) |
|---|---:|---:|---:|---:|---:|
| Kein | 44 / 36 % | 0 / 0 % | 0 / 0 % | 0 / 0 % | 97 |
| Wenig | 48 / 31 % | 10 / 20 % | 23 / 38 % | 11 / 22 % | 101 |
| Mittel | 48 / 31 % | 16 / 19 % | 25 / 43 % | 17 / 21 % | 97 |
| Viel | 44 / 32 % | 16 / 33 % | 33 / 54 % | 19 / 31 % | 92 |

„Kein“ ist bei gleicher Karte und Strategiewahl vollständig bestimmt. Keine Stufe
verschiebt Siegquote oder Partielänge merklich. Mit jeder Stufe wechseln mehr Sieger,
bei „Viel“ deutlich: mit vier Reichen entscheidet dort in gut der Hälfte der Karten der
Würfel mit, wer gewinnt. Die Anteile sind Obergrenzen, denn Basisstrategie gegen
Basisstrategie mit gleichem Start ist der knappste Fall; eine ungleiche Partie kippt
seltener. Wenig und Mittel liegen nahe beieinander, ihr Unterschied steckt vor allem in
den Ereignissen. Kampfzufall 0/5/10/20 % und Ereignisse aus/×0,5/×1/×2 bleiben.

### Nachweise

Rohdaten lokal unter `build/strategy-harness/`: `k4-start` (Ausgangswerte, Karten 1–100),
`k4-sweep/*` (Entwicklung), `k4-control` (Kontrolle vor der Auslosung), `k4-final`,
`k4-sizes` und `k4-randomness` (Abschluss); je Ordner `invocation.txt`, `matches.csv` und
`strength.md`. Java 25.0.3, Quellfingerabdruck der Abschlussläufe
`fd19c1dd7c68e8d949918f15c23cdb15a5fd432e3e83ddd6b60e5d4a9b0c88c2`. Insgesamt rund
26.700 Partien.

## Normal weniger aggressiv (K7d) — 25.11.2026

Rückmeldung nach zwei Testpartien (drei Reiche, klassisch, zwei normale Computer): „Die ist auf
Normal etwas stark geworden. Greift unvorhersehbarer an.“ Entscheidung des Users: **Normal wird
bei gleichem Start weniger angriffslustig**, nicht über einen kleineren Start schwächer. Ziel war
eine Siegquote der Referenz am fairen Anteil oder etwas darüber (Zweikampf etwa 50–60 %, drei
Reiche 33–40 %, vier Reiche 25–32 %) bei sichtbar selteneren Angriffen. Einfach und
Anspruchsvoll bleiben unverändert und müssen ihre Rangfolge behalten.

„Groß“ meint in diesem Abschnitt noch 40 Systeme; seit `4ffd8ea` heißt diese Größe Riesig. Die
Nachmessung mit Groß 30 und Riesig 40 steht im Abschnitt zu E5.

### Aufbau

Prüfstand und Karten wie K4 (`--mode=strength`, Zufall Wenig, Ereignisse an, Startwerte 4/8,
Referenz BASELINE auf Sitz 1, Heimat ausgelost). Neu im Bericht `strength.md` ist der Abschnitt
„Angriffe der Computer“ (Spalten auch in `matches.csv`): Sendungen und Angriffe je Computer und
Runde, in der er ein System hält, Schiffe je Angriff und die Runde des ersten Angriffs auf die
Referenz. Ein Angriff ist eine Sendung an ein System, das beim Planen einem anderen Reich gehört;
Eroberungen neutraler Systeme zählen nur als Sendung.

Die Basisstrategie schickt in fast jeder Runde eine Flotte: von der stärksten Basis die Hälfte der
freien Schiffe an das nächste fremde System, ohne Blick auf dessen Verteidigung. Gegen Reiche sind
das 0,72–0,81 Angriffe je Runde. Weil im Kampf jedes Schiff ungefähr ein gegnerisches Schiff
mitnimmt, kostet dieses Nachschieben kaum Kraft, wirkt aber wie ein Dauerangriff von wechselnden
Basen.

Neues Profil `CAUTIOUS` (`ai.domain.CautiousPolicy`; Spiel und Prüfstand rufen dieselbe Planung
über `AiPolicy` auf und sehen wie die übrigen Profile nur, was ein Mensch sieht):

- Jedes System hält eine **Heimatwache** von drei Runden seiner Produktion gegen einen Rivalen;
  mit mehr Rivalen am Tisch schrumpft sie mit der Wurzel ihrer Zahl (drei Reiche gut zwei Runden,
  vier Reiche gut anderthalb). Verbündete zählen nicht.
- **Höchstens eine neue Flotte je Runde**, und nur, wenn die freien Schiffe eines Systems die
  geschätzte Verteidigung bei Ankunft um die Hälfte übertreffen (`(Garnison + Produktion bis zur
  Ankunft) × 1,5 + 1`; unbekannte Garnison 6, unbekannte Produktion 5). Dann gehen **alle** freien
  Schiffe dieses Systems.
- Ziele nach Reisezeit + Verteidigung/8, Systeme anderer Reiche zählen zwei Runden mehr: nahe,
  schwache und neutrale Ziele zuerst. Ein Ziel, zu dem schon eine eigene Flotte fliegt, bekommt
  keine zweite.

Die Stellgrößen wurden während der Entwicklung über eine Umgebungsvariable verändert, die der
Code nicht mehr enthält; je Vergleich änderte sich eine Größe gegenüber dem vorigen Stand.

### Entwicklung (Karten 1–150, Mittel und Groß, Zufall Wenig)

Je Zelle 150 Partien, Computer Normal (gleicher Start). Quote = Siege der Referenz; Angriffe je
Computer und Runde im Zweikampf auf Mittel.

| Variante | Mittel 2 / 3 / 4 | Groß 2 / 3 / 4 | Angriffe |
|---|---|---|---:|
| Basisstrategie (bisher Normal) | 47 / 27 / 17 % | 51 / 32 / 20 % | 0,72 |
| erster Entwurf: Wache 2 Runden, Überlegenheit 1,5, höchstens zwei Unternehmungen, nur benötigte Schiffe | 100 / 96 / 83 % | 100 / 100 / 100 % | 0,18 |
| ohne Obergrenze, Wache 1, Überlegenheit 1,2, nur benötigte Schiffe | 24 / 25 / 23 % | 84 / 59 / 55 % | 0,50 |
| dasselbe, Produktion des Ziels nur zur Hälfte geschätzt | 81 / 53 / 53 % | 97 / 97 / 91 % | 0,66 |
| alle freien Schiffe, Überlegenheit 1,5, Wache 1 | 15 / 21 / 23 % | 15 / 30 / 16 % | 0,38 |
| … Wache 2 | 32 / 29 / 34 % | 34 / 36 / 31 % | 0,36 |
| … Wache 2, höchstens ein Angriff auf Reiche zugleich | 97 / 79 / 76 % | 99 / 95 / 93 % | – |
| … Wache 2, Überlegenheit 1,2 bzw. 2,0 | 25 / 28 / 31 % bzw. 64 / 47 / 58 % | 33 / 36 / 31 % bzw. 54 / 47 / 49 % | – |
| … Wache 3 | 57 / 41 / 49 % | 52 / 45 / 45 % | 0,35 |
| … Wache 5 | 86 / 68 / 74 % | 78 / 69 / 67 % | 0,32 |
| … Wache 3, zwei neue Flotten je Runde | 35 / 36 / 43 % | 5 / 27 / 35 % | 0,37 |
| **Wache 3 / √Rivalen (gewählt)** | **57 / 31 / 33 %** | **52 / 34 / 31 %** | **0,35** |
| Wache 3 / Rivalen^0,7 | 57 / 29 / 27 % | 52 / 34 / 21 % | 0,35 |
| Wache 3,2 / √Rivalen | 60 / 31 / 33 % | 54 / 36 / 26 % | – |

Wer nur die nötigen Schiffe schickt, lässt die übrigen ungenutzt liegen und verliert gegen das
Dauernachschieben der Referenz deutlich. Wer alle freien Schiffe schickt, ist bei weniger
Angriffen stärker als die Basisstrategie; die Heimatwache ist dann der feine Regler. Eine feste
Wache passt aber nur zu einer Spielerzahl: drei Runden treffen den Zweikampf und machen die
Computer mit mehreren Reichen zu schwach, zwei Runden umgekehrt. Die Wache nach der Zahl der
Rivalen trifft alle drei Spielerzahlen. Obergrenzen für gleichzeitige Angriffe machten die
Computer sehr schwach, ebenso eine zu knappe Schätzung der Verteidigung (mehr Fehlschläge).

### Endwerte und Kontrolle (Karten 1001–1200, Zufall Wenig)

| Stärke | Computer | Start (Produktion/Schiffe) |
|---|---|---|
| Einfach | Basisstrategie (unverändert) | ×0,5 (2/4) |
| Normal | **ruhige Planung `CAUTIOUS`** | ×1 (4/8) |
| Anspruchsvoll | Zweikampf: verdeckt Basisstrategie oder Rush; sonst Basisstrategie (unverändert) | ×1,5 (6/12) |

Siegquote der Referenz, je Zelle 200 Partien, in Klammern 95 % (fairer Anteil 50/33/25 %); zum
Vergleich Normal mit der Basisstrategie auf denselben Karten (entspricht K4):

| Stärke | Größe | 2 Spieler | 3 Spieler | 4 Spieler |
|---|---|---:|---:|---:|
| Einfach | Mittel | 86 % (81–90) | 45 % (38–51) | 46 % (39–53) |
| Einfach | Groß | 66 % (59–72) | 40 % (33–46) | 43 % (36–49) |
| **Normal** | Mittel | **64 % (57–70)** | **32 % (26–39)** | **33 % (26–39)** |
| **Normal** | Groß | **50 % (43–56)** | **35 % (29–42)** | **29 % (23–36)** |
| Normal bisher (Basisstrategie) | Mittel | 52 % (45–59) | 29 % (23–36) | 24 % (19–30) |
| Normal bisher (Basisstrategie) | Groß | 42 % (35–49) | 30 % (24–37) | 27 % (21–34) |
| Anspruchsvoll | Mittel | 23 % (18–29) | 26 % (20–33) | 10 % (7–15) |
| Anspruchsvoll | Groß | 16 % (12–22) | 28 % (22–34) | 16 % (11–21) |

Angriffslust der Computer in der Kontrolle (je Computer und Runde; Normal neu / bisher):

| Größe | Spieler | Sendungen | Angriffe | Schiffe je Angriff | Angriffe auf die Referenz | erster Angriff auf die Referenz (Median) |
|---|---:|---:|---:|---:|---:|---:|
| Mittel | 2 | 0,64 / 1,00 | 0,33 / 0,73 | 85 / 55 | 0,33 / 0,73 | Runde 22 / 27 |
| Mittel | 3 | 0,65 / 1,00 | 0,37 / 0,73 | 54 / 37 | 0,20 / 0,36 | 17 / 20 |
| Mittel | 4 | 0,62 / 0,99 | 0,41 / 0,80 | 44 / 31 | 0,15 / 0,25 | 16 / 19 |
| Groß | 2 | 0,87 / 1,00 | 0,38 / 0,74 | 125 / 99 | 0,38 / 0,74 | 31 / 39 |
| Groß | 3 | 0,81 / 1,00 | 0,42 / 0,73 | 84 / 64 | 0,21 / 0,37 | 24 / 30 |
| Groß | 4 | 0,78 / 1,00 | 0,42 / 0,81 | 70 / 53 | 0,15 / 0,28 | 22 / 28 |

Runden bis zur Entscheidung (Median, Normal neu / bisher): Zweikampf Mittel 87/102, Groß 132/157;
drei Reiche 66/69 und 95/112; vier Reiche 78/86 und 123/147.

Entscheidung:

- **Normal** spielt in Klassisch die ruhige Planung `CAUTIOUS` mit gleichem Start. Die Computer
  greifen andere Reiche nur noch etwa halb so oft an (0,33–0,42 statt 0,73–0,81 je Runde), die
  Referenz ebenfalls etwa halb so oft, dafür mit größeren Flotten aus einem System, und es fliegt
  höchstens eine neue Flotte je Runde los. Die Referenz gewinnt in jeder Zelle etwas mehr als
  vorher und liegt am fairen Anteil oder darüber.
- **Einfach** und **Anspruchsvoll** bleiben unverändert; ihre Partien sind mit dem Stand vor der
  Änderung bitgenau gleich (gleiche Prüfsummen in `matches.csv`). Die Rangfolge der Siegquoten
  Einfach > Normal > Anspruchsvoll hält in jeder Zelle.
- Erweitert bleibt bei der Basisstrategie aus `DefaultSpacewardPlanning` (heute kalibriert).

Grenzen:

- **Zweikampf Mittel** liegt mit 64 % (57–70) etwas über dem Ziel von 50–60 %, Groß mit 50 %
  am unteren Rand; in der Entwicklung waren es 57 und 52 %. Eine kleinere Wache brächte Mittel in
  den Zielbereich, drückte Groß aber unter den fairen Anteil.
- **Offene Partien** nehmen zu: Nach 300 Runden ist auf Groß mit drei Reichen jede sechste
  (31 von 200), mit vier Reichen gut jede fünfte Partie (45 von 200) offen, bisher 3 bzw. 2. Die
  vorsichtigen Computer reiben sich gegenseitig kaum auf. Offene Partien zählen nicht als Sieg der
  Referenz; dort unterschätzen die Quoten eher, wie oft ein Mensch am Ende gewinnt, und die
  Partien werden länger.
- **Einfach mit drei Reichen auf Groß** liegt mit 40 % nur noch wenig über Normal (35 %); die
  Intervalle überlappen.
- Der **erste Angriff** auf die Referenz kommt früher (Mittel Runde 22 statt 27), weil die
  neutralen Nachbarn schneller fallen; danach folgen Angriffe deutlich seltener.
- Die Wache zählt die Rivalen am Tisch, auch ausgeschiedene; sie wächst nicht, wenn Reiche
  ausscheiden.
- Die Referenz ist die Basisstrategie, kein Mensch. Ob die Computer ruhiger und berechenbarer
  wirken, zeigt erst eine Testpartie.

### Nachweise

Rohdaten lokal unter `build/strategy-harness/`: `k7d-before` (alle Stärken vor der Änderung,
Karten 1–150), `k7d-sweep/*` (Entwicklung; Stellgrößen im Ordnernamen in der Reihenfolge
Überlegenheit, Wache, neue Flotten je Runde, Unternehmungen zugleich, Aufschlag Reich, unbekannte
Garnison, Anteil Produktion, Angriffe auf Reiche zugleich, Anteil freier Schiffe, Exponent der
Rivalen), `k7d-dev` und `k7d-dev-baseline` (Entwicklung, Endstand), `k7d-control` und
`k7d-control-baseline` (Kontrolle); je Ordner `invocation.txt`, `matches.csv` und `strength.md`.
Java 25.0.3, Quellfingerabdruck der Abschlussläufe
`85acb4aa82db794f894bc6908ef367911abe8909d22f89a8f612fca3cb51f16d`. Insgesamt rund 39.000
Partien.

## Computerstärke Erweitert (E5) — 25.11.2026

Kalibriert werden die Stufen des Dialogs „Neue Partie“ für **Erweitert 2.0.0** (Industrieausbau,
Reichweite, Bündnisse). Aufbau wie bei K4; `--mode=strength` kennt dafür `--rules=spaceward`.
Referenz auf Sitz 1 ist wieder die Basisstrategie. In Erweitert baut sie ein Drittel der Kapazität
aus, solange kein Gegner näher als zwei Runden steht, spielt also wie ein Mensch, der seine
Industrie mitnimmt. Die Computer schließen untereinander keine Bündnisse (die Spiel-KI beantwortet
nur Vorschläge), der Prüfstand spielt deshalb ohne Diplomatie; Sieg bei 70 % allein.

### Entwicklung (Karten 1–150, Mittel und Groß, Zufall Wenig)

Je Zelle 150 Partien, Computerprofile und Startanteil fest vorgegeben. Quote = Siege der
Referenz (offene Partien nach 300 Runden zählen nicht als Sieg).

| Computer | Startanteil | Mittel 2 / 4 Spieler | Groß 2 / 4 Spieler |
|---|---|---|---|
| BASELINE | 0,5 | 75 / 55 % | 54 / 37 % |
| BASELINE | 0,75 | 55 / 24 % | 41 / 14 % |
| BASELINE | 1,0 | 50 / 23 % | 40 / 15 % |
| BASELINE | 1,25 / 1,5 / 2,0 | 51 / 46 / 45 % · 22 / 18 / 17 % | 39 / 36 / 33 % · 14 / 11 / 9 % |
| RUSH | 1,0 | 87 / 89 % | 91 / 99 % |
| EXPANSION | 1,0 | 75 / 72 % | 76 / 79 % |
| CONCENTRATION | 1,0 | 63 / 59 % | 69 / 70 % |
| DEFENSE | 1,0 | 81 / 77 % | 89 / 81 % |
| INDUSTRY | 1,0 | 56 / 65 % | 24 / 42 % |
| INDUSTRY_LIGHT | 1,0 | 27 / 17 % | 21 / 23 % |
| INDUSTRY_ADAPTIVE | 1,0 | 16 / 17 % | 9 / 17 % |

Anders als in Klassisch verlieren Rush, Expansion, Konzentration und Verteidigung fast immer
gegen die ausbauende Referenz. Die bisherige Normal-Mischung aus genau diesen vier Profilen war
deshalb viel zu leicht. Der Startanteil wirkt ab ×0,75 kaum: Die günstigen ersten Ausbaustufen
(W7) lassen ein kleineres Reich aufholen, und ein größerer Start bringt wenig. Erst ×0,5 macht die
Computer spürbar schwächer; die bisherige Stufe Einfach (×0,75) war kaum leichter als Normal.

### Endwerte und Kontrolle (Karten 1001–1200, Zufall Wenig)

| Stärke | Computer | Start |
|---|---|---|
| Einfach | Basisstrategie | ×0,5 |
| Normal | Basisstrategie | ×1 |
| Anspruchsvoll | adaptive Industrie | ×1 |

Siegquote der Referenz, je Zelle 200 Partien, in Klammern 95 % (fairer Anteil 50/33/25 %):

| Stärke | Größe | 2 Spieler | 3 Spieler | 4 Spieler |
|---|---|---:|---:|---:|
| Einfach | Mittel | 72 % (65–78) | 48 % (41–55) | 51 % (44–58) |
| Einfach | Groß | 55 % (48–62) | 36 % (30–43) | 36 % (30–43) |
| Normal | Mittel | 50 % (43–56) | 29 % (23–36) | 26 % (20–32) |
| Normal | Groß | 37 % (30–43) | 26 % (20–33) | 16 % (11–21) |
| Anspruchsvoll | Mittel | 17 % (12–23) | 18 % (13–23) | 17 % (12–23) |
| Anspruchsvoll | Groß | 8 % (5–13) | 23 % (17–29) | 17 % (12–22) |

Offen nach 300 Runden blieben auf Mittel 4–21 %, auf Groß 12–32 % der Partien; auf Groß zählen
diese als Nicht-Sieg und drücken die Quoten. Unter den entschiedenen Partien liegt Normal auf Groß
im Zweikampf bei 53 %.

Zusatz mit 80 Karten (3001–3080), Einfach/Normal/Anspruchsvoll: Klein 16 im Zweikampf 80/55/23 %,
mit vier Reichen 59/19/11 %. **Riesig 64** blieb in Erweitert meist unentschieden: 60–78 % der
Partien waren nach 300 Runden offen (Klassisch: 20–40 %), Median der entschiedenen 140–260 Runden.

**Neue Größen Groß 30 und Riesig 40** (User 25.11.2026; Karten 3001–3080, je Zelle 80 Partien,
Siegquote der Referenz Einfach/Normal/Anspruchsvoll, in Klammern offen nach 300 Runden):

| Regeln | Größe | 2 Spieler | 4 Spieler | Median Runden |
|---|---|---|---|---|
| Klassisch | Groß 30 | 88 / 58 / 20 % (0–0 %) | 41 / 16 / 10 % (0–1 %) | 80–132 |
| Klassisch | Riesig 40 | 74 / 50 / 13 % (4–9 %) | 49 / 20 / 9 % (1–6 %) | 101–167 |
| Erweitert | Groß 30 | 68 / 48 / 14 % (9–18 %) | 45 / 21 / 14 % (6–29 %) | 117–174 |
| Erweitert | Riesig 40 | 61 / 44 / 16 % (14–25 %) | 28 / 14 / 19 % (24–31 %) | 141–201 |

Riesig ist damit in beiden Regeln meist zu Ende zu spielen; Normal liegt im Zweikampf fair.

Entscheidung:

- **Normal** spielt die Basisstrategie mit gleichem Start und liegt auf Mittel in jeder
  Spielerzahl im fairen Bereich.
- **Einfach** halbiert den Start; im Zweikampf gewinnt die Referenz 55–80 %, mit mehreren Reichen
  deutlich über dem fairen Anteil.
- **Anspruchsvoll** spielt die adaptive Industrie mit gleichem Start; die Referenz gewinnt nur noch
  8–23 %. Ein größerer Start wäre in Erweitert kaum wirksam.

Nicht erreicht: Normal mit vier Reichen liegt auf den großen Karten unter dem fairen Viertel
(14–21 %), auch wegen offener Partien.

### Nachweise

Rohdaten lokal unter `build/strategy-harness/`: `e5-sweep/*` (Entwicklung,
Quellfingerabdruck `a0bab3bfcd76f81240c618f0ac053a613cc0d79620c4846d617547b50bd51b9d`),
`e5-final` und `e5-sizes` (Abschluss, Quellfingerabdruck
`c58bc990959bafa899c91adba2b88cc4814dbf13cdfc0df30fd1e2c3501cdd05`); je Ordner `invocation.txt`,
`matches.csv` und `strength.md`; neue Größen unter `sizes-30-40-classic` und `sizes-30-40-spaceward`
(Quellfingerabdruck `f7b36deab78a5f3ffa5393f60116454b3771f2c54b9cdcbe15dd0108fabb68f0`). Java 25.0.3.
Insgesamt rund 14.300 Partien.
