# Wirtschaftsprüfung W4 — 07.10.2026

## Versuchsanordnung

Der stumme Prüfstand verwendet die vollständige W3-Wirtschaft, die unverändert
im Kandidaten 1.2.0-w4 steckt: 1 Metall je Schiff, 10 Startmetall, 200 Metall je
Reichtumsstufe, Förderung entsprechend dem Reichtum und 70 Prozent Recycling.
Jede Runde prüft die vollständige Metallbilanz einschließlich neutraler Schiffe,
Metallresten, Kampf- und Recyclingverlusten. Alle 25 Runden wird ein Snapshot neu
hergestellt. Ungültige Wirtschafts- und Flottenbefehle brechen den Versuch ab.

Abstimmungskarten und Kontrollkarten verwenden getrennte Kartenstartwerte.
Jede Kombination enthält zufällige und gleichmäßige Sternpositionen, 16 und 32
Systeme, beide Sitzfolgen im Zweikampf sowie rotierende Viererpartien.
Startgarnison: 20; Heimatindustrie: 5; neutrale Industrie: 1–10;
Siegschwelle: 70 Prozent; Kampfzufall: ±10 Prozent. Zwei Kampfstartwerte prüfen
die Empfindlichkeit gegenüber den Kämpfen. Ein Rundenlimit ist kein Remis.

Sechs Umgebungen: natürliche Geologie; fruchtbare Kolonien mit armen beziehungsweise
reichen Vorkommen; fruchtbare beziehungsweise raue Kolonien bei identischen
Vorkommen; vollständig erschöpfte Vorkommen ohne Startvorrat. Nur die jeweils
benannte Eigenschaft ändert sich, Positionen, Bevölkerung und Garnisonen bleiben
gepaart. Die erschöpfte Umgebung ist ein Belastungsfall, keine normale Neuanlage.

Alle acht auswählbaren KI-Profile werden separat mit der vorhandenen gemeinsamen
Förder- und Recyclingplanung geprüft. Für den Wirtschaftsvergleich bleibt die
militärische Planung CONCENTRATION. Nach deren normalen Befehlen passen drei
Versuchsprofile über reguläre Dienste die Wirtschaft an:

- MINING: die halbe verbleibende Kapazität fördern lassen, kein Recycling.
- RECYCLING: ein Viertel fördern lassen, bei Engpässen im Hinterland recyceln;
  vier ungebundene Schiffe erhalten.
- ADAPTIVE: Förderbedarf aus Vorrat, Kapazität und Ertrag bestimmen;
  bei Engpässen recyceln und acht ungebundene Schiffe erhalten.

STANDARD ist die unveränderte gemeinsame Planung. Die Wirtschaftsintervention
erfolgt nach der militärischen Planung; eine verbesserte Wirtschaft beeinflusst
spätere Flottenentscheidungen. Das isoliert Wirtschaftsänderungen mit derselben
militärischen Heuristik, untersucht aber keine optimal gemeinsam geplanten Züge.
Die Versuchsprofile sind nicht im Spiel auswählbar.

## Aufrufe und Rohdaten

```powershell
.\gradlew.bat strategyBenchmark "-PstrategyArgs=--mode=economy --suite=comparison --first-seed=1 --seeds=2 --combat-seeds=2 --rounds=150 --output=build/economy/tuning"
.\gradlew.bat strategyBenchmark "-PstrategyArgs=--mode=economy --suite=comparison --first-seed=1001 --seeds=2 --combat-seeds=2 --rounds=150 --output=build/economy/control"
```

`matches.csv` enthält Ausgänge, Karten- und Kampfstartwerte, Profile,
Verlustsummen und Verlaufshashes. `rounds.csv` enthält Reichsbestände und
Kapazitäten vor jeder Auflösung: `planned-built` ist ungenutzter Schiffbau wegen
Metallknappheit; `unused_labor_capacity` ist durch fehlende Arbeitskräfte gesperrte
Industrie. Verlustspalten sind globale kumulierte Bilanzwerte; sie dürfen nicht
über Spieler und Runden summiert werden. Die abschließenden Verluste stehen
je Partie einmal in `matches.csv`. `orders.csv` protokolliert Förderung, Ausbau,
Recycling und Versand. `invocation.txt` hält Aufruf, Java-Version und Quellhash fest.

## Ergebnisse und Entscheidung

Die Abstimmungsserie enthält 1.536 Partien: 1.152 Zweikämpfe und 384
Viererpartien, jeweils bis 150 Runden. Davon enden 1.045 mit Sieg; 491 erreichen
das Limit. Die Kontrolle umfasst weitere 1.536 Partien derselben Anordnung:
903 Siege und 633 offene Verläufe. Die getrennte Serie aller acht auswählbaren Profile umfasst weitere
768 Zweikämpfe mit bis zu 100 Runden: 391 Siege und 377 offene Verläufe.
Alle 62.482 dort aufgelösten Runden bestehen Befehls- und Bilanzprüfung.

STANDARD erscheint in den Rohdateien als CONCENTRATION. Siege je Profil und
Umgebung sind Einsätze in gemischten Zwei- und Viererpartien, keine gemeinsame
Siegwahrscheinlichkeit. Jedes Profil hat pro Umgebung und Serie 160 Einsätze;
Rundenlimit-Fälle zählen ausdrücklich nicht als Remis oder Niederlage.

Siege als Abstimmung / Kontrolle; jeweils 160 Einsätze:

| Umgebung | STANDARD | MINING | RECYCLING | ADAPTIVE |
|---|---:|---:|---:|---:|
| Natürlich | 84 / 73 | 53 / 39 | 34 / 29 | 25 / 27 |
| Metallarm | 36 / 34 | 135 / 123 | 0 / 0 | 79 / 77 |
| Metallreich | 53 / 49 | 16 / 21 | 100 / 67 | 32 / 34 |
| Rau | 93 / 82 | 49 / 46 | 28 / 14 | 29 / 23 |
| Fruchtbar | 93 / 82 | 49 / 46 | 28 / 14 | 29 / 23 |
| Erschöpft | 0 / 0 | 0 / 0 | 0 / 0 | 0 / 0 |

Die reine CONCENTRATION-Vergleichsserie baut keine Industrie aus. Rau und
fruchtbar liefern dort identische Ergebnisse: Der Wachstumsunterschied bindet
bei gleicher anfänglicher Arbeitsgrenze noch keine Kapazität. Die separate
Prüfung aller acht Profile weist den tatsächlichen Arbeitskräfteengpass nach.
BASELINE verliert auf rauen Karten 7.288 Kapazitätspunkt-Runden an fehlende
Arbeitskräfte, auf den gepaarten fruchtbaren Karten 109; INDUSTRY_LIGHT verliert
3.601 beziehungsweise 0. Das sind aufsummierte Größen entlang unterschiedlich
langer Verläufe, keine isolierten Wirkungsprozente. INDUSTRY_LIGHT baut dort
47.459 beziehungsweise 50.693 Schiffe. Wachstum wirkt also bei ausbauenden
Profilen, nicht automatisch bei jeder Spielweise.

Mehr Förderung ist auf metallarmen Karten hilfreich; auf reichen Karten ist
die kleinere Förderzuweisung mit Recycling stärker. Die adaptive Versuchsplanung
dominiert nicht über alle Umgebungen. Daher bleiben Baupreis, Startvorrat,
Vorkommen, Ertrag und Rückgewinnung unverändert. Keines der Versuchsprofile wird
in die öffentliche Auswahl übernommen. Die gemeinsamen legalen Förder- und
Recyclingentscheidungen bleiben für alle acht vorhandenen KI-Profile aktiv.
Industrieplanung berücksichtigt nun die bereits belegte Förderkapazität.
Die Kontrollkarten bestätigen die Richtung; insbesondere erreicht MINING auf
armen Karten 123/160 Siege, RECYCLING auf reichen Karten 67/160. Das ist eine
Heuristikbewertung, keine allgemeine Aussage über die Stärke des Recyclings.

Im künstlich vollständig erschöpften Fall erreicht jede Partie das Limit.
Recycling erzeugt wegen des 30-Prozent-Verlusts keinen unbegrenzten Rohstoff;
ohne neue Vorkommen kann es einen langfristigen Stillstand nicht verhindern.
Eine gewöhnliche neue Partie beginnt dagegen mit Vorkommen und Startmetall.

## Wiederholung, Bestandsschutz und Grenzen

Zwei getrennte JVMs wiederholen jeweils 384 Partien (Kartenstartwert 2001,
ein Kampfstartwert, 30 Runden). matches.csv, rounds.csv und orders.csv sind
jeweils bytegleich. SHA-256 der drei Dateien:

- matches: `96dafa7b87ccb1b471ebd5add0ef3f2fbfdc18e9e99f6734439b05e6b0f6d9de`
- rounds: `f9688216e2a93eb2272e616cd8044e0dbd63ff65d24523e56ca84dd09fec4985`
- orders: `0fa9b1cf149fb58b452328f27eba41d3e0501ce6f8252a193d018f87f1464952`

Zusätzlich stimmen die Ergebnis-, Runden- und Befehlsdateien von 50 alten
Referenzpartien unter build/economy/legacy mit dem gespeicherten Bestandslauf
build/strategy-harness/legacy-after-alliance exakt überein. Acht Spaceward-1.1-
Bündnisvergleiche unter legacy-1.1 stimmen mit group-repeat-a ebenfalls bytegleich
überein. Die neue Wirtschaft wird älteren Regelreferenzen nicht aufgezwungen.

Exakte zusätzliche Aufrufe:

```powershell
.\gradlew.bat strategyBenchmark "-PstrategyArgs=--mode=economy --suite=profiles --first-seed=1001 --seeds=1 --combat-seeds=2 --rounds=100 --output=build/economy/profiles"
.\gradlew.bat strategyBenchmark "-PstrategyArgs=--mode=economy --suite=comparison --first-seed=2001 --seeds=1 --combat-seeds=1 --rounds=30 --output=build/economy/repeat-a"
.\gradlew.bat strategyBenchmark "-PstrategyArgs=--mode=economy --suite=comparison --first-seed=2001 --seeds=1 --combat-seeds=1 --rounds=30 --output=build/economy/repeat-b"
.\gradlew.bat strategyBenchmark "-PstrategyArgs=--seeds=1 --systems=8 --layouts=EVEN --rounds=25 --output=build/economy/legacy"
.\gradlew.bat strategyBenchmark "-PstrategyArgs=--mode=alliance --rules=spaceward-1.1 --alliance-victory=both --seeds=1 --layouts=EVEN --systems=8 --rounds=25 --output=build/economy/legacy-1.1"
```

Die Abstimmung verwendet Kartenstartwerte 1 und 2, die Kontrolle 1001 und 1002.
Je Serie ergeben sich acht unabhängige Kartenkombinationen aus Startwert,
Positionsverteilung und Systemzahl; Umgebungen, Kampfstartwerte und Sitzwechsel
sind abhängige Varianten derselben Karten. Viele Partien ersetzen damit keine
breite Stichprobe. Ergebnisse beziehen sich auf diese Heuristiken, 16/32 Systeme
und die festen Rundenlimits; sie beweisen weder optimale Spielweise noch fertig
abgestimmte menschliche Partien. Offene Verläufe brauchen spätere gezielte
Frontenprüfung. Die persönliche Erprobung und öffentliche Freigabe stehen aus.

Quellhash der Abstimmung:
`e05cde709bd11ba903004ad30a8891284bf57da6397ee11d73cf0cf27f24acc4`.
Quellhash von Kontrolle, Profilserie und Wiederholung:
`660339559057efd8cd11dc34c4f0317ba459304cf6e8f7013dc48ea0b4abc99e`.
Zwischen diesen Läufen wurden Kandidatenkatalog und zugehörige Tests ergänzt;
Wirtschaftsformeln und Versuchsprofile blieben unverändert. Eine anschließende
Korrektur doppelter Ansichtsaktualisierung verändert die Simulation nicht.
Die Fingerprints umfassen alle Java-Quellen und build.gradle; aktuelle
Oberflächenkorrekturen können deshalb einen anderen Quellhash erzeugen.


## Technische Abnahme

Insgesamt 4.666 simulierte Partien: 3.072 Wirtschaftsvergleiche, 768 Partien
aller acht Profile, 768 Wiederholungspartien sowie 58 alte Referenzvergleiche.
Die ersten drei Wirtschaftsserien lösen zusammen 376.693 Runden auf; jede
besteht die exakte Bilanzprüfung. Es tritt kein abgelehnter Befehl, kein
überbuchter Kapazitätsplan und kein Fehler bei den gespeicherten Zwischenständen
auf. Wiederholungen und Bestandsvergleiche bestehen zusätzlich.

Vollständiger Build: 779 Java-Tests, Coverage-Prüfung bestanden, keine
SpotBugs-Befunde. Abschließender stummer Browserlauf: alle 18 Abläufe bestanden,
keine Überspringung. Die zusätzliche gezielte Neuanlage mit aktiviertem W4-Profil
prüft tatsächliche Förderung, Recycling, einmalige Gutschrift, Rundenwechsel
und Neuladen. Fünf Frontend-Prüfungen der Schlachtwiedergabe sind grün.

Der lokale Erprobungsstand läuft im Produktionsmodus mit local,economy-preview
auf Port 8110. Normale Spaceward-Neuanlage ohne diesen Schalter bleibt 1.1.0.
Die technische W4-Abnahme ist abgeschlossen; die persönliche Erprobung des
Spielgefühls und die gemeinsame öffentliche Freigabe stehen noch aus.


### Ergänzende Sonar-Prüfung am 07.10.2026

Die lokale Analyse des gesamten Projekts meldete zunächst 123 offene Befunde.
Assertions, Importe, Methodenreferenzen und Enum-Maps wurden bereinigt;
Wiederherstellung, Wirtschaftsplanung und Wizard-Bestätigung in kleinere
Methoden aufgeteilt. Die Verlegungsvorschau rechnet vor Math.clamp mit long,
damit große Garnisonen nicht vor dem Begrenzen überlaufen. Eine Regression
prüft Integer.MAX_VALUE ausdrücklich. Wirtschaftswerte und Heuristiken bleiben
unverändert; die 50 alten Referenz- und acht Spaceward-1.1-Partien liefern erneut
bytegleiche Ergebnis-, Runden- und Befehlsdateien (build/economy/sonar-legacy*).

Abschlussanalyse 07.10.2026, 12:05 Uhr: 0 offene Befunde, Qualitätsprofil OK,
88,9 Prozent Coverage im neuen Code, 0,11 Prozent neue Duplikation. 780 Java-Tests,
Coverage-Grenzen, SpotBugs mit null Befunden und alle 18 stummen Browserabläufe
grün. Fünf Frontend-Prüfungen sind ebenfalls grün. Die Analyse enthält weiterhin
die erwartete Git-Zuordnungswarnung für nicht committete Änderungen.

Die abschließende gezielte W4-Neuanlage mit economyPreview=true ist ebenfalls
grün. Der lokale Testserver wurde anschließend mit dem bereinigten Stand
neu gestartet; gespeicherte Partien bleiben erhalten.
