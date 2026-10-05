# Erste Strategieprüfstände — 05.10.2026

Der Prüfstand ist für SectorForces (`classic/1.0.0`) und den internen
Spaceward-Ausbau (`spaceward/1.0.0`) verfügbar. Er verwendet die echten
Rundenimplementierungen und prüft unterschiedliche Strategien, ohne die
reguläre KI oder bestehende Spielregeln zu verändern. Bedienung und Profile
stehen im [README](../README.md#strategy-experiments).

## Versuchsaufbau und Nachweise

Die erste Serie umfasst 765 ausgewertete Partien sowie 108 zusätzliche Partien
für Wiederholungen in getrennten JVMs. Java: 25.0.3. Quellfingerabdruck:
`c367442108167f83764078d0ef6e1cbdacf789a0615fd4cbbf14c9b577a60e7a`.

| Serie | Partien | Kartenstartwerte | Systeme | Rundenlimit | Kampfwiederholungen je Karte |
|---|---:|---|---|---:|---:|
| Zweikampfmatrix | 600 | 1–3 | 16, 32 | 120 | 1 |
| Mehrspieler | 44 | 1–2 | 16 | 80 | 1 |
| Bündnis | 16 | 1–2 | 16 | 80 | 1 |
| Einzelentscheidung | 105 | 1–3 | 16 | 80 | 3 |

Alle Serien verwenden RANDOM und EVEN sowie ±10 % Kampfvariation.
Der erste Kampfstartwert ist 314159; die Entscheidungsserie verwendet auch
314160 und 314161. Zweikampfpaare tauschen die Startplätze, Mehrspielerprofile
rotieren durch alle Sitze. Die Bündnisserie verwendet vier Reiche und einen
festen Vertrag zwischen Sitz 1 und 2. Sie enthält keine diplomatische KI.
Entscheidungsversuche verzweigen vor Runde 10; nur ein Befehl beziehungsweise
eine Ausbauzuweisung von Spieler 1 wird einmal ersetzt.

Die Versuchskarten verwenden die produktive Galaxieplatzierung und weit
auseinanderliegende Heimatwelten, aber eine kontrollierte Startwirtschaft:
Heimatkapazität 5, Startgarnison 20, neutrale Produktion 1–10. Die abschließende
Abstandsentspannung und Namensvergabe der Lobby werden nicht nachgebildet.
Diese Stichprobe ist daher keine repräsentative Stichprobe aller Lobby-Partien.

Rohdaten liegen lokal unter `build/strategy-harness/initial-matrix/`,
`initial-multiplayer/`, `initial-alliance/` und `initial-decisions/`.
Jede Serie enthält `invocation.txt`, `matches.csv`, `rounds.csv`, `orders.csv`,
`summary.md`, `decision-deltas.csv` und `skipped.txt`. Die erzeugten Dateien
werden nicht versioniert; dieser Bericht hält den geprüften Befund fest.

## Zweikämpfe

Diese Übersicht fasst beide Kartentypen und Größen zusammen. Jedes Profil
hat dieselben Gegner und dieselbe Zahl Startplatzwechsel innerhalb seiner
Variante. Die Detailauswertung trennt Kartentyp, Größe und Gegner.

| Profil | SectorForces: Siege / Partien | Spaceward: Siege / Partien |
|---|---:|---:|
| Bisherige KI | 34 / 96 | 31 / 120 |
| Aggressiv | 74 / 96 | 37 / 120 |
| Expansion | 64 / 96 | 69 / 120 |
| Flottenkonzentration | 41 / 96 | 70 / 120 |
| Verteidigung | 21 / 96 | 42 / 120 |
| Industrie | — | 17 / 120 |

Sechs von 240 SectorForces-Partien und 94 von 360 Spaceward-Partien erreichten
das Rundenlimit. Sie zählen weder als Sieg noch als Unentschieden. Die
Siegquoten beziehen sich einschließlich dieser offenen Ausgänge auf alle Läufe.

Aggressives Spiel ist in diesen SectorForces-Zweikämpfen stark. Bei Spaceward
liegen Expansion und Konzentration vorne. Die feste Industrieheuristik
investiert häufig zwei Drittel ihrer Kapazität und gewinnt selten. Das ist
ein Befund über dieses Profil; er beweist nicht, dass Industrieausbau nutzlos
oder seine Kosten falsch sind. Situationsabhängiges Investieren muss gezielt
verglichen werden.

## Mehrspieler und Bündnisse

In den 20 SectorForces-Partien mit fünf Reichen gewann das aggressive Profil
nur einmal, die bisherige KI dagegen neunmal. Der Erfolg im Zweikampf lässt
sich somit nicht auf Mehrspieler übertragen. Verteidigung, Expansion und
Konzentration gewannen ebenfalls einzelne Partien.

Von 24 Spaceward-Partien mit sechs Reichen endeten 16 am Rundenlimit.
Die begrenzte Laufzeit liefert hier vor allem Entwicklungsverläufe und keine
vollständige Rangfolge nach Siegen.

13 von 16 Bündnispartien endeten am Rundenlimit. Ein konkreter Grenzfall:
Partie 3 auf RANDOM verteilt alle 16 Systeme auf die Verbündeten als 11:5;
beide unabhängigen Gegner besitzen kein System mehr. Die individuelle
70-Prozent-Siegbedingung verlangt aber zwölf Systeme von einem einzelnen
Reich. Der feste Vertrag erlaubt keine Eroberung beim Partner und wird von
diesen Profilen nie gekündigt. Dieser Versuchsaufbau kann deshalb in einem
solchen Stand keinen Sieg mehr erzeugen. Das ist ein konkreter Prüfpunkt für
spätere Sieg- und Bündnisplanung; die Regeln wurden nicht verändert.

## Einzelentscheidungen

Verglichen werden derselbe Ausgangsstand und dieselben künftigen
Rundenstartwerte. Spätere Befehle dürfen auf den geänderten Verlauf reagieren.
Bei geänderten Kampfkonstellationen stimmen die einzelnen Würfe nicht
zwangsläufig überein; deshalb gibt es drei Kampfstartwerte je Karte.

| Eingriff in Runde 10 | Vergleichspaare | Veränderte Systeme, Schiffe oder Kapazität bei gleicher Runde | Anderer Sieger oder Ausgangstyp bis zum Limit |
|---|---:|---:|---:|
| SectorForces: ersten Versand auslassen | 18 | 6 | 0 |
| Spaceward: ersten Versand auslassen | 18 | 7 | 1 |
| Spaceward: volle Kapazität in Ausbau | 18 | 18 | 4 |
| Spaceward: volle Kapazität in Schiffbau | 15 | 15 | 1 |

Drei weitere Schiffbau-Eingriffe wurden ausgelassen, weil das betreffende
System ohnehin keine Kapazität in Ausbau investierte. Die 105 Partien
enthalten 36 unveränderte Referenzfortsetzungen und 69 geänderte Fortsetzungen.
Ein anderer Ausgangstyp kann auch Sieg statt Rundenlimit bedeuten; diese
Zahl ist keine reine Quote gewechselter Sieger. Die Eingriffe zeigen, dass
Entscheidungen messbar wirken. Sie belegen noch keine allgemein optimale
Ausbauquote oder situationsabhängige Balance.

## Verifikation und nächste Prüfung

Der vollständige Build ist erfolgreich: 640 Java-Tests, darunter 27 neue
Prüfungen, Coverage-Grenzen erfüllt und keine SpotBugs-Befunde. Die neuen
Prüfungen sichern Informationsgrenzen, unveränderte Ausgangsstände,
Fortsetzung aus Snapshots, gültige Befehle, Schiffsbilanzen und Reisedauern.

Je zwei getrennte JVM-Läufe der Zweikampfmatrix mit acht Systemen und eines
Bündnisversuchs erzeugen bytegleiche Ergebnis-, Runden-, Befehls- und
Zusammenfassungsdateien. `invocation.txt` unterscheidet sich erwartungsgemäß
im Ausgabepfad. Es wurde kein Browser gestartet und kein Ton abgespielt.

Weitere fachliche Versuche sollten getrennte Kontrollkarten verwenden
(`--first-seed=1001`), längere Spaceward-Partien untersuchen und mehrere
situationsabhängige Ausbauquoten vergleichen. Feste Bündnisse benötigen eine
gesonderte Betrachtung der individuellen Siegbedingung. Menschliche Partien
bleiben Teil der Bewertung. Metallvorkommen und Verschrottung kommen erst
nach ihrer Regelumsetzung als weitere Strategien und Bilanzgrößen hinzu.

Die Kartenanzahl ist klein und die Profile sind einfache Heuristiken. Die
Bootstrap-Intervalle im Detailbericht sind beschreibend; enge oder sogar
punktförmige Intervalle bei wenigen Karten sind kein Balancebeweis.
Die Versuche ersetzen weder M7 noch die Freigabe einer neuen Spielvariante.

## Nachprüfung: Bündnissieg in Spaceward 1.1.0

Der neue Regelstand erlaubt getrennte Partieoptionen für Bündnisse und
Bündnissiege sowie eigene Einzel- und Gruppenschwellen. Alte Regelversionen
bleiben unverändert. Der 11:5-Grenzfall ist als gezielte Prüfung enthalten:
mit Gruppensieg gewinnen beide Mitglieder, ohne Gruppensieg bleibt er offen.
Einzelsieg hat Vorrang; wirksame Mitglieder ohne Systeme gewinnen ebenfalls.
Kündigungen zählen erst ab ihrer Wirksamkeit.

Die neue Serie enthält 144 Partien, also 72 Vergleichspaare: RANDOM und EVEN,
je drei Kartenstartwerte (1–3), drei Kampfstartwerte (314159–314161), 16 Systeme,
120 Runden, ±10 Prozent Kampfvariation und vier rotierende Strategiebelegungen.
Sitz 1 und 2 sind fest verbündet; beide Schwellen betragen 70 Prozent.
Die Paare unterscheiden sich ausschließlich durch erlaubten Gruppensieg.
Die Profile sind RUSH, DEFENSE, CONCENTRATION und INDUSTRY. Java: 25.0.3;
Quellfingerabdruck:
`18126f789eb668ad1ff9086b96729c7e2a45314676dff3883030cd266237c556`.
Rohdaten: `build/strategy-harness/alliance-victory-paired/`.

| Bündnissieg | Einzelsiege | Gruppensiege | Rundenlimit | Partien |
|---|---:|---:|---:|---:|
| Ausgeschaltet | 22 | 0 | 50 | 72 |
| Eingeschaltet | 9 | 48 | 15 | 72 |

35 der zuvor offenen Partien enden mit erlaubtem Bündnissieg. In 13 weiteren
Paaren erfolgt ein Gruppensieg vor dem späteren Einzelsieg des Vergleichslaufs.
Die verbleibenden neun Einzelsiege behalten ihren Sieger. Die 15 offenen
Partien zeigen, dass Gruppensieg nicht jeden langen Verlauf löst. Diese kleine
Serie mit festem Vertrag bewertet weder Verhandlungsstrategien noch eine
allgemeine Spielbalance; ein Bündnissieg ist kein Beleg für gute Metall- oder
Industriekosten.

Bestandsvergleich: 50 Partien der früheren Wiederholungsmatrix erneut gespielt
(EVEN, acht Systeme, Kartenstartwert 1, Kampfstartwert 314159, 25 Runden).
Alle 20 SectorForces- und 30 Spaceward-1.0.0-Ausgänge, Sieger und Rundenzahlen
bleiben gleich. `rounds.csv`, `orders.csv`, `decision-deltas.csv` und `skipped.txt`
sind bytegleich mit der früheren Serie. Die erweiterten Ergebnisfelder und
Verlaufsfingerabdrücke haben ein neues Format und werden deshalb getrennt
vom fachlichen Bestandsvergleich betrachtet. Neue Dateien:
`build/strategy-harness/legacy-after-alliance/`.

Reproduzierbarkeit: zwei getrennte JVMs spielen je acht gepaarte Bündnispartien
(EVEN, acht Systeme, 25 Runden). Ergebnis-, Runden-, Befehls-, Zusammenfassungs-,
Entscheidungs- und Auslassungsdateien sind bytegleich. Dateien liegen unter
`build/strategy-harness/group-repeat-a/` und `group-repeat-b/`. Insgesamt sind
für diese Nachprüfung 210 Partien ausgewertet (144 + 50 + 16).

Verifikation: vollständiger Build mit 653 Java-Tests, erfüllten Coverage-Grenzen
und null SpotBugs-Befunden; 13 stumme Browserabläufe bestanden. Die gezielten
Prüfungen umfassen Schalterabhängigkeiten, getrennte Schwellen, neutrale Systeme,
Einzelpriorität, Gruppengleichstand, wirksame Kündigung, alte JSON-Daten,
gespeicherte Fortsetzung alter Versionen, mehrere Sieger, Vorlagen, Archiv und
Statistik. Ergebnisdialog und Wizard wurden anhand der Prüfbilder unter
`build/alliance-victory/` angesehen. Spaceward bleibt bis M7 gesperrt.
