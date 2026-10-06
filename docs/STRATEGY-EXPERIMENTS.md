# Erste Strategieprüfstände — 05.10.2026

Der Prüfstand ist für SectorForces (`classic/1.0.0`) und Spaceward
(`spaceward/1.0.0` und `spaceward/1.1.0`) verfügbar. Seit M7 ist Spaceward
1.1.0 auch in der Lobby freigegeben. Die erste Serie unten untersucht
ausdrücklich den damaligen Regelstand 1.0.0. Der Prüfstand verwendet die echten
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
`build/alliance-victory/` angesehen. Diese Nachprüfung erfolgte vor M7;
die spätere Freigabe ist seit dem Merge `af837ca` vom 05.10.2026 enthalten.


## Strategievertiefung — 06.10.2026

M7 ist erledigt. Diese Serie untersucht die freigegebene Spaceward-Regelversion
1.1.0 und SectorForces 1.0.0. Die produktive KI und Spielregeln bleiben erhalten.
Die neuen Industrieprofile wurden nach der Entwicklungsserie nicht nachjustiert;
alle folgenden Kontrollen verwenden denselben eingefrorenen Code. Java 25.0.3,
Quellfingerabdruck `463f1f005e05938f10190d1c683e6d454111d4949843a0518d8c2a2812e78432`.
Die Abhängigkeitsupdates, Build und Browserabnahme stehen im Changelog und Plan.

### Aufbau und Profile

INDUSTRY bleibt die bisherige Vergleichsheuristik (zwei Drittel Ausbau,
ein Fünftel bei bekanntem Feind innerhalb zweier Reiserunden).
INDUSTRY_LIGHT investiert ein Viertel außerhalb dieser Nahbedrohung und
sonst nichts. INDUSTRY_ADAPTIVE investiert nur mit mindestens zweimal der
Kapazität an versendbaren Schiffen und ohne Reservebedarf: ein Viertel bei
Feinden in drei bis vier Reiserunden, die Hälfte weiter hinten. Seine Zuweisung
übersteigt nicht die verbleibenden Ausbaukosten der nächsten Kapazitätssteigerung.
Beide lesen ausschließlich erlaubte Sichtdaten. Ein unbekannter Feind ist keine
bekannte Bedrohung; das Verfahren behauptet damit keine tatsächliche Sicherheit.
Integer-Rundung bedeutet auf sehr kleinen Kolonien gegebenenfalls keinen Ausbau.

Alle drei Industrieprofile haben dieselben militärischen Planungsregeln.
Unterschiedliche Ausbauzuweisungen verändern später verfügbare Schiffe und
Entscheidungen. Deshalb lässt sich die Wirkung dieser heuristischen Wahl prüfen,
aber keine allgemeine optimale Ausbauquote oder reine Kapitalrendite ableiten.
Expansion und Flottenkonzentration ergänzen die Zweikampfmatrix. Ohne --profiles
behält der Prüfstand seine bisherigen Versuchsdimensionen. Die Aufrufe verwenden
strategyBenchmark mit -x vaadinBuildFrontend; keine produktive Datenbank, Oberfläche
oder Audio. Alle Serien laufen mit RANDOM und EVEN sowie ±10 Prozent Kampfvariation.

Die Entwicklungsserie (Karten 101–103, 16/32 Systeme, 120 Runden) enthält 240
Partien. Pro Profil sind es 96 Einsätze: Expansion gewinnt 22, Konzentration 34,
Industrie 8, zurückhaltende Industrie 30 und adaptive Industrie 40. 106 Partien
enden am Rundenlimit. Die anschließenden Kontrollkarten 1001–1005 wurden für
keine Anpassung dieser Profile verwendet. Kontroll- und Wiederholungsläufe sind
voneinander getrennte Nachweise; vertauschte Sitze sind keine unabhängigen Karten.

Rohdaten unter build/strategy-harness/deepening-train, deepening-control-120,
deepening-control-300, deepening-classic, deepening-classic-multiplayer,
deepening-multiplayer, deepening-combat-control und deepening-repeat-a/-b.
Jeder Ordner enthält den genauen Aufruf in invocation.txt und CSV-Nachweise.

### Frühe Wirtschaftsentscheidung bei gleicher Runde

In allen 80 gemeinsamen Fällen der drei Industrieprofile gegen dieselben Gegner
Expansion und Konzentration sind die Parteien in Runde 20 noch auswertbar.
Die Mittelwerte beziehen sich auf dieselben Karten, Sitze und Kampfstartwerte;
sie sind nicht nur auf spätere Sieger eingeschränkt. Die reine Ausbauheuristik
investiert wesentlich mehr und hält dabei weniger Schiffe bereit.

| Profil | Systeme | Kapazität | Schiffe | Gebaut bis Runde 20 | Verloren | Investiert |
|---|---:|---:|---:|---:|---:|---:|
| Industrie | 7,0 | 54,1 | 172,3 | 188,6 | 36,3 | 291,1 |
| Zurückhaltend | 7,9 | 44,3 | 330,6 | 352,2 | 41,7 | 72,6 |
| Adaptiv | 8,0 | 49,6 | 302,5 | 324,8 | 42,4 | 134,0 |

Kapazität summiert eigene Systeme; Eroberungen und Verluste wirken mit.
Investitionen sind verwendete Produktionspunkte, keine Metallkosten.
Die Werte beschreiben einen frühen Unterschied zwischen den Profilen und
beweisen weder einen generellen Ausbaufehler noch falsche Spielkosten.

### Reproduzierbarkeit und technische Abnahme

Zwei getrennte JVMs mit EVEN, acht Systemen, Karten 2001–2002, 50 Runden
und allen fünf Spaceward-Matrixprofilen spielen je 40 Partien. matches.csv,
rounds.csv, orders.csv, summary.md, decision-deltas.csv und skipped.txt sind
bytegleich. invocation.txt unterscheidet sich absichtlich im Ausgabeordner.

Vollständiger Build, 664 Java-Tests, fünf Frontend-Tests und 13 stumme
Browserabläufe bestanden. Coverage erfüllt, keine SpotBugs-Befunde. Eine
saubere npm-ci-Installation funktioniert; npm audit meldet keine Befunde.
Die neuen Prüfungen sichern Ausbaugrenzen, Reservebedarf, Bedrohungsnähe,
Profilauswahl, legale Runden und dieselben Informationsgrenzen wie die übrigen
Profile. Die historischen Standardprofile werden nicht durch die neuen ersetzt.

### Kontrollserien und längere Verläufe

Insgesamt wurden in dieser Vertiefung 1.904 Partien ausgewertet:
240 Entwicklungspartien, zweimal 400 Spaceward-Kontrollpartien, 400
SectorForces-Zweikämpfe, 100 SectorForces- und 84 Spaceward-Mehrspielerpartien,
200 zusätzliche Kampfzufallskontrollen sowie 80 Wiederholungspartien.

| Serie | Kartenstartwerte | Systeme | Rundenlimit | Partien |
|---|---|---|---:|---:|
| Entwicklung | 101–103 | 16, 32 | 120 | 240 |
| Spaceward-Kontrolle | 1001–1005 | 16, 32 | 120 / 300 | 400 / 400 |
| SectorForces-Zweikämpfe | 1001–1005 | 16, 32 | 120 | 400 |
| SectorForces-Mehrspieler, fünf Reiche | 1001–1005 | 16, 32 | 120 | 100 |
| Spaceward-Mehrspieler, sieben Reiche | 1001–1003 | 16, 32 | 300 | 84 |
| Weiterer Kampfstartwert | 1001–1005 | 16 | 300 | 200 |
| Zwei JVM-Wiederholungen, nur EVEN | 2001–2002 | 8 | 50 | 80 |

Standard-Kampfstartwert ist 314159, die Zusatzserie verwendet 314160.
Mehrspieler rotieren die Sitze und schließen keine Bündnisse. Die Spaceward-
Mehrspielerserie umfasst BASELINE, RUSH, EXPANSION, CONCENTRATION sowie die drei
Industrieprofile; SectorForces verwendet seine fünf ursprünglichen Profile.
Keine Serie bewertet diplomatische Entscheidungen oder Bündnissieg erneut.

| Spaceward-Profil | Siege / 160 Einsätze, 120 Runden | Siege / 160 Einsätze, 300 Runden | Offene Einsätze bei 300 | Beschreibendes 95%-Intervall der Siegquote bei 300 |
|---|---:|---:|---:|---|
| Expansion | 63 | 64 | 3 | 33,1–49,4 % |
| Konzentration | 65 | 77 | 21 | 44,4–51,3 % |
| Industrie | 13 | 29 | 19 | 11,9–25,6 % |
| Zurückhaltend | 67 | 85 | 28 | 48,1–58,1 % |
| Adaptiv | 73 | 97 | 25 | 56,9–64,4 % |

Die Gesamtzahl offener Partien fällt von 119 auf 48. 71 zuvor offene Partien
enden später; alle 281 bereits entschiedenen Partien behalten Sieger, Runde
und Verlaufsfingerabdruck. Sämtliche 67.808 Rundenwerte und 1.151.161
Befehlszeilen bis Runde 120 stimmen zwischen beiden Serien überein.
Die höhere Grenze verändert damit ausschließlich die verfügbare Laufzeit.

Die Intervalle verwenden 2.000 deterministische Bootstrap-Ziehungen ganzer
Kartenstartwert-Blöcke (Startwert 20261006). Alle Sitze, Größen und Verteilungen
desselben Kartenstartwerts bleiben zusammen; es sind fünf Blöcke, nicht 160
unabhängige Karten. Sie beschreiben Unsicherheit innerhalb dieses kleinen
Versuchssatzes. Schmale Intervalle beweisen keine allgemeine Überlegenheit.
Die Detailberichte trennen Größe, Verteilung und Gegner nochmals auf.

Gegen dieselben beiden Gegner Expansion und Konzentration sind es je 80
Einsätze: Industrie gewinnt bei 300 Runden 19, Zurückhaltend 45 und Adaptiv 52.
Der Vorteil gegenüber der bisherigen Ausbauheuristik bleibt somit auch bei
identischer Gegnerauswahl sichtbar. Die Rangfolge ist dennoch ortsabhängig:
Bei RANDOM/16 gewinnt Adaptiv 22 von 40 Einsätzen, Zurückhaltend 21 und
Konzentration 20; bei EVEN/32 sind es 28, 20 und 15. Diese Unterschiede sind
keine Begründung, die Spielkosten automatisch zu ändern.

Alle 48 offenen Partien haben bei Runde 300 vollständig besetzte Systeme.
Sie verteilen sich auf RANDOM/16 (15), RANDOM/32 (19), EVEN/16 (3) und EVEN/32
(11). Fehlende neutrale Expansion erklärt diese Fälle somit nicht. Die
Rohdaten allein erklären aber nicht, welche militärische Entscheidung die
Front auflösen würde. Gezielt rekonstruierte Endzustände und Vergleichseingriffe
in Konzentration, Zielwahl und Versand sind der nächste Prüfpunkt.

### Zweikampf und Mehrspieler sind unterschiedliche Aufgaben

| SectorForces-Profil | Zweikampfsiege / 160 Einsätze | Mehrspielersiege / 100 Partien |
|---|---:|---:|
| Bisherige KI | 56 | 48 |
| Aggressiv | 113 | 6 |
| Expansion | 106 | 16 |
| Konzentration | 76 | 14 |
| Verteidigung | 40 | 11 |

Neun Zweikämpfe und fünf Mehrspielerpartien bleiben offen. Der frühere Befund
wiederholt sich auf den neuen Karten: Aggression ist im Zweikampf stark und
in diesem Mehrspielerfeld schwach. Die bisherige KI gewinnt dort deutlich
häufiger. Ein einziges globales Strategie-Ranking wäre irreführend.

Bei Spaceward mit sieben Reichen und 300 Runden gewinnen von 84 Partien:
bisherige KI 20, Aggressiv 6, Expansion 10, Konzentration 10, Industrie 3,
Zurückhaltend 12 und Adaptiv 18. Fünf Partien bleiben offen. Auch das adaptive
Profil verdrängt die Referenz damit nicht als allgemeiner Sieger.

### Empfindlichkeit gegenüber Kampfzufall

Die 200 Zweikämpfe mit 16 Systemen wurden mit identischen Karten und Sitzen,
aber Kampfstartwert 314160 erneut gespielt. Es ändern sich 19 Ausgangstypen
(Sieg statt Limit oder umgekehrt) und in weiteren 19 Paaren der Sieger bei
beiderseits entschiedenen Partien. Offene Partien: 18 bei 314159, 19 bei 314160.

| Profil | Siege / 80 Einsätze, 314159 | Siege / 80 Einsätze, 314160 |
|---|---:|---:|
| Expansion | 34 | 38 |
| Konzentration | 44 | 41 |
| Industrie | 11 | 14 |
| Zurückhaltend | 45 | 39 |
| Adaptiv | 48 | 49 |

Der Vorteil der beiden neuen Ausbauprofile gegenüber der alten Heuristik bleibt
in dieser Zusatzprüfung bestehen. Schon diese zwei Startwerte ändern einzelne
Ergebnisse deutlich; sie reichen nicht für eine allgemeine Aussage über alle
Kampfzufälle. Die 32-System-Kontrolle verwendet weiterhin nur einen Kampfstartwert.

### Ergebnis und Grenzen

Die Vertiefung ist technisch abgenommen und dokumentiert. Sie zeigt, dass die
bisherige feste Industrieheuristik früh zu viele Produktionspunkte bindet und
dass zurückhaltende beziehungsweise sichtabhängige Investition in diesem
Versuchsfeld bessere Ausgänge erzielt. Längere Laufzeit hilft, beseitigt die
militärischen Fronten aber nicht vollständig. SectorForces bestätigt besonders
deutlich den Unterschied zwischen Zweikampf und Mehrspieler.

Die Kontrolle umfasst nur fünf Kartenstartwerte und synthetische Startwirtschaft.
Untersucht werden begrenzte Heuristiken, keine optimalen Strategien. Weitere
Kampfstartwerte, 32-System-Endzustände, echte Lobby-Karten und menschliche
Partien bleiben sinnvoll. Rohstoffe, Recycling und Koloniewachstum wurden
nicht vorgezogen; erst ihre spätere Regelumsetzung macht entsprechende
Strategien und Bilanzgrößen prüfbar. Keine produktive KI oder gespeicherte
Partie wurde durch diese Befunde verändert.
## Übernahme in die Spiel-KI — 06.10.2026

Nach Abschluss der oben beschriebenen Versuchsserie wurden die acht Profile auf
ausdrücklichen Nutzerauftrag je KI-Sitz im Wizard auswählbar gemacht. Die
historischen Befunde und ihr Quellfingerabdruck bleiben unverändert. Die neuen
produktiven Alternativen und der Prüfstand verwenden dieselbe AiPolicy sowie
IndustryPolicy aus ai.domain; die militärischen und wirtschaftlichen Formeln
der untersuchten Profile wurden dabei nicht geändert.

BASELINE bleibt Vorgabe und Rückfall für alte JSON-Spielstände. Die Auswahl
ändert keine Regeln und keine bereits gespeicherte Partie. Sie wird je Spieler
sowie in den Ausgangseinstellungen gespeichert und von Vorlagen/Revanchen
übernommen. Vergleichstests prüfen die erzeugten Befehle aller Profile in
SectorForces und beiden Spaceward-Versionen sowie verborgene Garnisonen und
fremde Befehle. Die acht Profile sind keine garantierte Rangfolge und keine
festen Schwierigkeitsstufen. Die 48 anhaltenden Fronten bleiben ein eigenes
späteres Untersuchungsfeld.
