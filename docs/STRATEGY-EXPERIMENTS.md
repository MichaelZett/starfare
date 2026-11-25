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

Die Rangfolge kippt mit der Spielerzahl (wie schon im Oktober). Rush, Expansion und
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
