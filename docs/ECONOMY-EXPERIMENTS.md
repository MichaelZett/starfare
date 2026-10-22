# Wirtschaftsprüfung

Der erste Teil dokumentiert die W4-Prüfung vom 07.10.2026; der Abschnitt W6 ergänzt
den Vergleich der neueren Wirtschaft und die gleichmäßige Metallverteilung.

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


## W6: Metallverteilung und Industrie gegen frühe Flotten

### Regeländerung und Fragestellung

W6 verteilt knappes Metall gleichmäßig auf aktive Werften, begrenzt durch deren
Schiffbauplan. Nicht benötigte Anteile gehen an die übrigen Werften. Einzelne
Reststücke gehen nach Systemkennung; die Listenreihenfolge hat keinen Einfluss.
Bei Bedarf 10 und 4 sowie 8 Metall baut W6 jeweils 4 Schiffe; W5 bleibt bei 6 und 2.
Metall ist weiter reichsweit verfügbar. W6 übernimmt W5 ohne Arbeitskräftebegrenzung
und mit Verlegungskürzung. Ausbaukosten, Förderwerte und KI-Profile bleiben unverändert.
Das lokale Erprobungsprofil legt W6 an; gespeicherte W4/W5 behalten ihre Regeln.
Terraforming, aktive Bevölkerung und Besiedlung sind zurückgestellt.

Geprüft wird die Beobachtung, dass Industrieausbau gegenüber frühen Flotten und
Eroberungen verliert. Die Studie bewertet konkrete vorhandene KI-Profile und
beweist keine optimale menschliche Spielweise.

### Versuchsanordnung

Die breite Serie verwendet alle acht auswählbaren produktiven KI-Profile in jeder
Zweierpaarung mit beiden Sitzfolgen (56 Paarungen), dazu acht Viererpartien mit
rotierenden Sitzen. Je Regelversion sind es 512 Partien: vier Kartenkombinationen
(16/32 Systeme, zufällige/gleichmäßige Positionen), zwei Kampfstartwerte und 64
Aufstellungen. W4, W5 und W6 verwenden dieselben Karten und Sitzfolgen (Startwert
4101); die unabhängige W6-Kontrolle verwendet 6101. Rundenlimit: 150.

Die Vierergruppen sind Rush/Konzentration/starker/adaptiver Ausbau und
Basisstrategie/Expansion/Verteidigung/zurückhaltender Ausbau. Alle rotieren durch
alle vier Startplätze; ihre unterschiedlichen Gegner erlauben keine gemeinsame
Rangliste allein aus den Vierersiegen. Jede Strategie hat 112 Zweikampf- und 32
Vierereinsätze je breiter Serie.

Zusatzserien auf Startwert 5101 prüfen alle Profile gegen Konzentration in beiden
Sitzfolgen: metallarme und metallreiche Karten bis 150 Runden (128 Partien) sowie
natürliche Karten bis 300 Runden (64 Partien). Die Konzentration-Selbstvergleiche
gehören zum bestehenden Profilprüfstand. Die natürliche Geologie ist in der
breiten Serie unverändert; arme/reiche Umgebungen sind kontrollierte Geologievarianten.

Heimatindustrie 5, Startgarnison 20, neutrale Industrie 1–10, Siegschwelle 70 Prozent,
Kampfzufall ±10 Prozent. Die Positionen kommen aus dem produktiven Kartengenerator;
Startwelten und neutrale Garnisonen werden durch den vorhandenen Prüfstand gesetzt.
Alle Befehle gehen durch die normalen Dienste und die zur Regelversion gehörende
Rundenauflösung. Jede Runde prüft Kapazitätspläne, Reservierungen und Metallbilanz;
alle 25 Runden wird der Spielstand aus seinem Snapshot wiederhergestellt.

Abgeschlossen: **2.240 Studienpartien und 205.980 aufgelöste Runden**.

| Serie | Partien | Siege | Rundenlimit | Remis |
|---|---:|---:|---:|---:|
| w4-natural-150 | 512 | 375 | 137 | 0 |
| w5-natural-150 | 512 | 371 | 141 | 0 |
| w6-natural-150 | 512 | 370 | 142 | 0 |
| w6-control-150 | 512 | 464 | 48 | 0 |
| w6-resources | 128 | 116 | 12 | 0 |
| w6-long | 64 | 64 | 0 | 0 |

### Zweikämpfe: W4, W5 und W6 getrennt

Angaben jeweils **Siege/Niederlagen/offen/Remis**, 112 Einsätze je Feld.
Offen bedeutet Rundenlimit und zählt nicht als Niederlage oder Remis.

| Profil | W4 | W5 | W6 | W6 Kontrolle |
|---|---:|---:|---:|---:|
| Basisstrategie | 25/67/20/0 | 31/59/22/0 | 31/59/22/0 | 35/65/12/0 |
| Rush | 25/62/25/0 | 21/68/23/0 | 22/70/20/0 | 53/40/19/0 |
| Expansion | 53/37/22/0 | 47/42/23/0 | 48/42/22/0 | 76/33/3/0 |
| Konzentration | 65/7/40/0 | 67/7/38/0 | 67/6/39/0 | 69/34/9/0 |
| Verteidigung | 50/16/46/0 | 46/17/49/0 | 46/17/49/0 | 52/46/14/0 |
| Starker Ausbau | 0/111/1/0 | 0/104/8/0 | 0/104/8/0 | 0/112/0/0 |
| Zurückhaltender Ausbau | 56/16/40/0 | 61/19/32/0 | 60/18/34/0 | 65/39/8/0 |
| Adaptiver Ausbau | 55/13/44/0 | 55/12/45/0 | 54/12/46/0 | 60/41/11/0 |

In beiden breiten W6-Serien zusammen gewinnt starker Ausbau keinen seiner
224 Zweikämpfe (216 Niederlagen, 8 offene Partien). Zurückhaltender Ausbau gewinnt
125, verliert 57 und bleibt 42-mal offen; adaptiver Ausbau gewinnt 114, verliert
53 und bleibt 57-mal offen. Bereits W4 und W5 liefern dem starken Profil in der
gepaarten Serie keinen Sieg. Die aufgehobene Arbeitsgrenze behebt diese konkrete
Schwäche somit nicht. Bei moderatem Ausbau gibt es dagegen viele Siege; eine
allgemeine Aussage „Industrieausbau verliert immer“ wäre zu weitgehend.

Der gepaarte Wechsel W4 → W5 verändert Ausgang oder Sieger in 83 von 512 Partien;
W5 → W6 in 11 von 512. Die Verlaufshashes ändern sich in 416 beziehungsweise 220
Fällen. Verglichen werden ausschließlich identische Karten, Kampfstartwerte,
Sitzfolgen und Profile. Die geringe Änderung der Ausgänge durch W6 ist kein
Nachweis fehlender Wirkung der Metallverteilung und keine allgemeine Balancegarantie.

### W6: direkter Vergleich der Industrieprofile

Breite Serie und unabhängige Kontrolle zusammen; je Gegner 32 Partien.
Angaben wiederum Siege/Niederlagen/offen/Remis.

| Profil | Gegen Rush | Gegen Konzentration |
|---|---:|---:|
| Starker Ausbau | 0/31/1/0 | 0/32/0/0 |
| Zurückhaltender Ausbau | 15/10/7/0 | 12/12/8/0 |
| Adaptiver Ausbau | 16/6/10/0 | 8/10/14/0 |

### Viererpartien

Breite W6-Serie und Kontrolle zusammen, 64 Einsätze je Profil. Die oben genannten
unterschiedlichen Vierergruppen bleiben getrennte Gegnerfelder.

| Profil | Siege/Niederlagen/offen/Remis |
|---|---:|
| Basisstrategie | 14/34/16/0 |
| Rush | 10/38/16/0 |
| Expansion | 12/36/16/0 |
| Konzentration | 19/29/16/0 |
| Verteidigung | 13/35/16/0 |
| Starker Ausbau | 1/47/16/0 |
| Zurückhaltender Ausbau | 9/39/16/0 |
| Adaptiver Ausbau | 18/30/16/0 |

### Rohstoffkontrolle und längerer Horizont

Je Feld acht Partien gegen Konzentration. Die Langserie endete vollständig mit
Siegern vor dem 300-Runden-Limit.

| Profil | Metallarm (150) | Metallreich (150) | Natürlich (300) |
|---|---:|---:|---:|
| Starker Ausbau | 4/4/0/0 | 0/8/0/0 | 0/8/0/0 |
| Zurückhaltender Ausbau | 6/2/0/0 | 4/4/0/0 | 4/4/0/0 |
| Adaptiver Ausbau | 5/3/0/0 | 2/5/1/0 | 4/4/0/0 |

### Warum starker Dauerausbau zurückfällt

In der natürlichen Langserie erreichen alle acht Partien je Industrieprofil
Runde 25. Der starke Ausbauer besitzt dann im Mittel 51,0 Kapazität und 45,4
Schiffe; seine Konzentrationsgegner 49,4 Kapazität und 391,6 Schiffe. Beim
zurückhaltenden Profil sind es 54,5 Kapazität und 286,9 Schiffe, beim adaptiven
52,2 und 298,5. Diese Mittelwerte kommen aus denselben acht Karten-/Sitzfällen
je Profil, ohne Auslese bereits beendeter Partien in Runde 25.

Industrie aus Eroberungen ist nicht selbst gebaut. Die Analyse rekonstruiert
Kapazität und Ausbaufortschritt aus den ursprünglichen Systemwerten und den
tatsächlich protokollierten Ausbauaufträgen. Bei Besitzerwechseln zählt die
übernommene Kapazität getrennt; vor jeder Produktion wird die rekonstruierte
Kapazität mit den aufgezeichneten Reichssummen abgeglichen.

| Profil, Langserie | Investierte Kapazitätspunkte | Selbst gebaute Kapazität | Aus neutralen Systemen übernommen | Von Gegnern übernommen |
|---|---:|---:|---:|---:|
| Starker Ausbau | 9741 | 443 | 318 | 29 |
| Zurückhaltender Ausbau | 8283 | 373 | 447 | 317 |
| Adaptiver Ausbau | 3760 | 199 | 422 | 335 |

Diese Gesamtsummen umfassen unterschiedlich lange Partien; sie sind keine
isolierten Wirkungsprozente. Ein investierter Punkt ist gebundene Kapazität,
nicht automatisch ein verlorenes Schiff: Bei Metallmangel hätte dieser Punkt
auch ohne Ausbau nicht zwingend ein Schiff produziert.

Die Kostenformel bleibt 2 × aktuelle Kapazität. Bei Kapazität 10 kostet die nächste
Stufe 20 Punkte. Wenn jeder Punkt alternativ ein Schiff erzeugen könnte und
die neue Stufe danach vollständig in zusätzlichen Schiffbau geht, benötigt sie
nach Fertigstellung weitere 20 Produktionsrunden bis zum Ausgleich. Metallmangel,
veränderte Zuweisungen, Eroberung oder weiterer Ausbau verändern diese Rechnung.

Der starke Ausbau belegt ohne nahe Bedrohung bis zu zwei Drittel der Kapazität;
die gemeinsame Förderplanung beansprucht zusätzlich etwa ein Drittel. Dadurch
bleibt im Hinterland häufig wenig unmittelbarer Schiffbau. Zurückhaltender und
adaptiver Ausbau lassen früher mehr Schiffe zu. Das erklärt eine Schwäche des
konkreten starken KI-Profils; es beweist nicht, dass jeder Industrieausbau falsch ist.
Der geringe Kapazitätsvorsprung in Runde 25 bei großem Flottenrückstand zeigt
zugleich, warum die Rendite eigenen Ausbaus gegenüber Eroberungsgewinnen geprüft
werden sollte.

**Entscheidung:** Gleichmäßige Metallverteilung ist umgesetzt. Ausbaupreise und
KI-Profile werden durch diese Studie nicht geändert. Vor einer Balanceänderung
sollen Kosten/Dauer und Eroberungsgewinne isoliert auf denselben militärischen
Heuristiken verglichen werden. Persönliche Abnahme und Freigabe 1.2.0 bleiben offen.

### Nachvollziehbarkeit und Grenzen

Rohdaten, Aufrufe und Analyse liegen unter `build/economy/w6-study/`; nur die
vollständigen, oben aufgeführten Läufe gehen in die Studie ein. Jedes Verzeichnis
enthält `invocation.txt`, `matches.csv`, `rounds.csv`, `orders.csv` und die lokale
Zusammenfassung `analysis.json`. Kartenstartwerte und Sitzwechsel bilden
abhängige Fälle derselben Karten. Die Paarserie umfasst je vier räumliche
Kartenkombinationen pro Startwert, nicht Hunderte unabhängiger Karten. Auch
die unabhängige W6-Kontrolle und die Geologievarianten ersetzen keine breiten
menschlichen Partien. Rundenlimits sind keine Gleichstände und keine Niederlagen.

Zwei getrennte JVMs wiederholen zusätzlich je 64 Profilpartien bis Runde 30.
Alle drei Ergebnisdateien sind bytegleich. SHA-256:

- matches: `83fbbc45204d80f0b48a0e41bd1a15b283b2f18ea36710945447d32f244c15a3`
- rounds: `07df49660c40bfbe5a3bc08e79f8b46f3eb315650d151af2b9f35416e98ba119`
- orders: `10832d78c5955bb039a489d7b59fde77a62cddcf07d2a76893c7bed7d24bef9b`

Quellhash aller Serien:
`4167b68f17ca4524b1a50cb9c79b6cd8735b70253a447ca0d8c6e03e50287d26`.
Die 128 Wiederholungspartien kommen zu den Studienpartien hinzu. Vollständige
Java-Testsuite: 866 Tests, keine Fehler oder Überspringungen. Vollständiger Build,
SpotBugs, Coverage-Grenzen und beide Frontend-Prüfdateien bestehen.

Reproduzierbare Aufrufe (jeweils im Projektverzeichnis; unter Windows
`./gradlew` durch `.\gradlew.bat` ersetzen):

```sh
./gradlew strategyBenchmark '-PstrategyArgs=--mode=economy --suite=strategies --rules=w4 --environments=NATURAL --first-seed=4101 --seeds=1 --combat-seeds=2 --rounds=150 --output=build/economy/w6-study/w4-natural-150'
./gradlew strategyBenchmark '-PstrategyArgs=--mode=economy --suite=strategies --rules=w5 --environments=NATURAL --first-seed=4101 --seeds=1 --combat-seeds=2 --rounds=150 --output=build/economy/w6-study/w5-natural-150'
./gradlew strategyBenchmark '-PstrategyArgs=--mode=economy --suite=strategies --rules=w6 --environments=NATURAL --first-seed=4101 --seeds=1 --combat-seeds=2 --rounds=150 --output=build/economy/w6-study/w6-natural-150'
./gradlew strategyBenchmark '-PstrategyArgs=--mode=economy --suite=strategies --rules=w6 --environments=NATURAL --first-seed=6101 --seeds=1 --combat-seeds=2 --rounds=150 --output=build/economy/w6-study/w6-control-150'
./gradlew strategyBenchmark '-PstrategyArgs=--mode=economy --suite=profiles --rules=w6 --environments=POOR,RICH --first-seed=5101 --seeds=1 --combat-seeds=1 --rounds=150 --output=build/economy/w6-study/w6-resources'
./gradlew strategyBenchmark '-PstrategyArgs=--mode=economy --suite=profiles --rules=w6 --environments=NATURAL --first-seed=5101 --seeds=1 --combat-seeds=1 --rounds=300 --output=build/economy/w6-study/w6-long'
```

Für die Byte-Wiederholung den letzten Aufruf mit `--rounds=30` in zwei verschiedene
Ausgabeverzeichnisse ausführen. Der historische Standard des Prüfstands bleibt
W3; jede neue Studie muss ihre Regelreferenz ausdrücklich wählen.


## Gezielter Vergleich: früher Ausbau, Systemeignung und Eroberungsgewinne

### Ziel und isolierte Eingriffe

Ausbau soll zu Beginn eine sinnvolle Option sein, besonders auf günstig
erschließbaren Systemen. Geprüft werden Preise und eroberte Industrie getrennt
von einer aggressiven Dauerinvestition. Die investierende Seite verwendet immer
die produktive militärische CONCENTRATION-Heuristik; der Gegner CONCENTRATION
oder RUSH. Der gemeinsame W6-Förder- und Recyclingplan bleibt aktiv. Gleiche
Heuristik bedeutet dieselbe Entscheidungsregel, nicht identische Befehle nach
auseinanderlaufenden Spielständen.

Zum Zeitpunkt der Studie lagen alle Varianten ausschließlich im Testquellbaum
(Übernahme der EARLY_COST-Kurve als W7 siehe unten). W6, gespeicherte Partien,
öffentlicher Katalog, Kosten und auswählbare KI-Profile blieben in der Studie unverändert.

| Modell | Geänderter Faktor |
|---|---|
| BASELINE | Aktueller Preis 2C je zusätzlicher Kapazität |
| HALF_COST | Preis C bei jeder Ausbaustufe |
| EARLY_COST | Preis min(2C, max(2, ceil(C²/10))); günstig bei kleinen Industrien, ab C=20 wieder 2C |
| QUALITY_COST | EARLY_COST × 0,75/1/1,25 für fruchtbar/bewohnbar/rau, aufgerundet und höchstens 2C |
| HALF_CAPTURE | Preise 2C; bei jeder Eroberung bleibt ceil(C/2), mindestens 1, Ausbaufortschritt entfällt |

C bezeichnet die bestehende Kapazität. QUALITY_COST nutzt die vorhandene
Umwelteignung als Versuchsstellvertreter; damit wird weder aktive Bevölkerung
noch Besiedlung eingeführt. Die Eignung ist keine abschließend beschlossene
physische Begründung für Fabrikkosten. Der Vergleich EARLY_COST → QUALITY_COST
isoliert diesen Eignungsfaktor. HALF_CAPTURE ändert weder Schiffe noch Metall
oder Bevölkerung und ist keine beauftragte Zerstörungsregel.

| Kapazität | Heute | Halbiert | Günstiger Anfang | Fruchtbar | Rau |
|---|---:|---:|---:|---:|---:|
| 2 | 4 | 2 | 2 | 2 | 3 |
| 5 | 10 | 5 | 3 | 3 | 4 |
| 10 | 20 | 10 | 10 | 8 | 13 |
| 25 | 50 | 25 | 50 | 38 | 50 |
| 45 | 90 | 45 | 90 | 68 | 90 |

Wirtschaftsentscheidungen bei sonst gleicher militärischer Heuristik:

- SHIPS: kein Ausbau; die gemeinsame Förderung bleibt aktiv, also nicht
  buchstäblich jeder Kapazitätspunkt im Schiffbau.
- EARLY: höchstens eine zusätzliche Stufe je Systembesitz, Ausbaupunkte nur
  bis einschließlich Runde 20 und nur bei einem Stufenpreis bis 10.
- LIGHT: moderater Ausbau über den gesamten Verlauf.
- LATE: höchstens eine zusätzliche Stufe je Systembesitz, ab Runde 40.

Investiert wird nur ohne sichtbaren feindlichen Besitz innerhalb zweier direkter
Reiserunden und unterhalb Kapazität 50. Der Anteil beträgt ein Viertel der
Kapazität (mindestens 1), begrenzt durch freie Kapazität nach Förderung. Bei
EARLY/LATE endet er nach einer zusätzlichen Stufe; ein etwaiger Überschuss bleibt
als Ausbaufortschritt gespeichert. Bei Besitzverlust wird das lokale Ziel
entfernt. Diese festen Heuristiken sind keine optimalen Wirtschaftspläne.

### Karten, Auflösung und Integritätsprüfung

Abstimmung: Kartenstartwert 7101. Unabhängige Kontrolle: 8101. Je Familie vier
räumliche Kartenkombinationen aus 16/32 Systemen und zufälligen/gleichmäßigen
Positionen, zwei Kampfstartwerte, beide Sitzfolgen. Jede Kombination aus Modell,
Politik und Gegner hat je Familie 16 Einsätze, zusammen 32. Rundenlimit 150;
Heimatkapazität 5, Startgarnison 20, neutrale Kapazität 1–10, Siegschwelle 70 Prozent,
Kampfzufall ±10 Prozent. Alle fünf Modelle werden unverändert kontrolliert.

Die normale W6-Auflösung erledigt Versand, Recycling, Förderung, Schiffbau und
Kämpfe. Experimentelles Wachstum wird aus denselben zugewiesenen Punkten am
Produktionsschritt berechnet; die tatsächliche Schiffproduktion dieser Runde
bleibt unverändert. Die alternative Industrie wird am Rundenende über einen
vollständig validierten Snapshot eingesetzt, da sie erst nächste Runde produziert.
Eroberungen erhalten den richtigen neuen Fortschritt und setzen die Zuweisung
zurück; HALF_CAPTURE ersetzt zusätzlich die übernommene Industrie. In diesen
Versuchen gibt es keine Produktionsverlegungen. Die Industriegewinne im
Produktionsbericht berücksichtigen die experimentelle Berechnung.

Referenzgleichheit ist gegen den vorhandenen Wirtschaftsprüfstand getestet.
Ohne Ausbau sind die gesamten Verlaufshashes aller drei Preisvarianten in
beiden Familien identisch zu BASELINE. Eine inaktive Intervention darf auch
innerhalb eines Modells den Verlauf nicht verändern; die Analyse prüft dies
für jeden Einzelfall. Jede Runde prüft Befehle und die exakte Metallbilanz,
alle 25 Runden erfolgt die gewöhnliche Snapshot-Wiederherstellung. Die Testfälle
prüfen zusätzlich Fortschritt, Kapazitätsgrenze, reale Eroberung, geänderte Preise,
Wiederholbarkeit und den unveränderten Verlauf vor dem späten Einstieg.

Abgeschlossen: **1280 Partien und 117018 aufgelöste Runden**.

### Ergebnisse: gemeinsame Tabelle mit getrennter Gegnerart

Siege/Niederlagen/offen, je Feld 32 Partien aus Abstimmung und Kontrolle.
Die SHIPS-Spalte ist die passende Referenz desselben Modells. Offene Partien
werden weder als Niederlagen noch als Remis gezählt.

| Modell | Gegner | Kein Ausbau | Früher Einzelausbau | Moderater Ausbau | Einstieg ab 40 |
|---|---|---:|---:|---:|---:|
| Heutige Kosten | CONCENTRATION | 12/12/8 | 12/13/7 | 10/18/4 | 11/14/7 |
| Heutige Kosten | RUSH | 18/10/4 | 21/5/6 | 22/8/2 | 18/9/5 |
| Halbe Kosten | CONCENTRATION | 12/12/8 | 13/12/7 | 11/16/5 | 15/10/7 |
| Halbe Kosten | RUSH | 18/10/4 | 22/7/3 | 21/7/4 | 17/10/5 |
| Günstiger Anfang | CONCENTRATION | 12/12/8 | 14/9/9 | 11/17/4 | 15/10/7 |
| Günstiger Anfang | RUSH | 18/10/4 | 23/5/4 | 24/5/3 | 17/9/6 |
| Systemeignung | CONCENTRATION | 12/12/8 | 11/12/9 | 12/16/4 | 16/10/6 |
| Systemeignung | RUSH | 18/10/4 | 21/6/5 | 24/6/2 | 17/10/5 |
| Halbe eroberte Industrie | CONCENTRATION | 11/11/10 | 8/9/15 | 10/11/11 | 8/9/15 |
| Halbe eroberte Industrie | RUSH | 18/8/6 | 21/8/3 | 20/5/7 | 19/5/8 |

### Unabhängige Kontrolle des frühen Einzelausbaus

Je Feld 16 Fälle. Gleiche Geometrien mit Sitzwechsel und Kampfstartwerten bleiben
abhängige Beobachtungen; die kleinen Tabellen begründen keine allgemeine Rangfolge.

| Modell | Gegner | Abstimmung EARLY | Kontrolle EARLY | Kontrolle ohne Ausbau |
|---|---|---:|---:|---:|
| Heutige Kosten | CONCENTRATION | 7/8/1 | 5/5/6 | 6/6/4 |
| Heutige Kosten | RUSH | 10/2/4 | 11/3/2 | 10/3/3 |
| Halbe Kosten | CONCENTRATION | 9/6/1 | 4/6/6 | 6/6/4 |
| Halbe Kosten | RUSH | 9/4/3 | 13/3/0 | 10/3/3 |
| Günstiger Anfang | CONCENTRATION | 7/6/3 | 7/3/6 | 6/6/4 |
| Günstiger Anfang | RUSH | 10/3/3 | 13/2/1 | 10/3/3 |
| Systemeignung | CONCENTRATION | 6/8/2 | 5/4/7 | 6/6/4 |
| Systemeignung | RUSH | 10/4/2 | 11/2/3 | 10/3/3 |
| Halbe eroberte Industrie | CONCENTRATION | 6/4/6 | 2/5/9 | 4/4/8 |
| Halbe eroberte Industrie | RUSH | 10/4/2 | 11/4/1 | 12/2/2 |

### Gepaarte Änderungen statt bloßer Siegquoten

Einige Karten werden schon ohne Ausbau gewonnen. Die folgende Tabelle zählt
nur tatsächliche Wechsel zwischen Sieg und Niederlage derselben Ausgangslage.
Übergänge von oder zu offenen Partien sind davon getrennt und bleiben in
`analysis.json` vollständig enthalten. Je Feld 64 Fälle über beide Gegnerarten.

| Modell | Früher Ausbau: Niederlage→Sieg / Sieg→Niederlage | Moderat | Spät |
|---|---:|---:|---:|
| Heutige Kosten | 2 / 0 | 2 / 1 | 1 / 1 |
| Halbe Kosten | 3 / 0 | 3 / 1 | 1 / 1 |
| Günstiger Anfang | 5 / 0 | 4 / 2 | 2 / 0 |
| Systemeignung | 3 / 0 | 4 / 0 | 1 / 0 |
| Halbe eroberte Industrie | 2 / 1 | 4 / 2 | 1 / 0 |

### Einordnung und konkreter Vorschlag

Gezielter günstiger Anfangsausbau ist bereits bei den heutigen Preisen eine
Option: Gegen Rush erreicht EARLY 21/5/6 gegenüber 18/10/4 ohne Ausbau.
Der frühere Befund zum starken Dauerausbau lässt sich darauf nicht übertragen.

Die günstige Anfangskurve ist unter den getesteten Kandidaten der passendste
Vorschlag: EARLY erreicht gegen Rush 23/5/4 und gegen Konzentration 14/9/9;
die jeweiligen Referenzen ohne Ausbau 18/10/4 und 12/12/8. In der unabhängigen
Kontrolle sind es 13/2/1 gegen Rush (Referenz 10/3/3) und 7/3/6 gegen Konzentration
(Referenz 6/6/4). Die Richtung bestätigt sich dort, die Stichprobe aus insgesamt
acht räumlichen Kartenkombinationen bleibt klein und ist kein allgemeiner
Optimalitätsnachweis. Der erste fertige frühe Ausbau liegt im Median bei Runde 3,
bei heutigen Preisen Runde 10, bei pauschal halbierten Preisen Runde 5.

Als Regelentwurf bietet sich deshalb an: niedrige Kapazitäten günstig erweitern,
mit steigender Kapazität deutlich teurer werden und ab C=20 wieder den bisherigen
Preis erreichen. Die konkret geprüfte Kurve kostet C=2 → 2, C=5 → 3, C=10 → 10,
C=20 → 40. Sie fördert kurze frühe Projekte, ohne auch den gesamten späten Ausbau
pauschal zu halbieren. Eine einzelne Stufe soll gegen einen Flottenauftrag
abwägbar sein; dauerhafter Pflichtausbau ist kein Ziel.

Der zusätzliche Eignungsfaktor verbessert diese EARLY-Resultate nicht:
QUALITY_COST ergibt 11/12/9 gegen Konzentration und 21/6/5 gegen Rush.
Das belegt keinen generellen Nachteil von Systemeigenschaften, trägt aber keinen
sofortigen zusätzlichen Preisfaktor. Falls später gewünscht, sollte die fachliche
Industrieeignung bewusst von Bevölkerungszahl und bloßer Bewohnbarkeit abgegrenzt
werden. Es gibt mit dieser Studie keinen neuen Bevölkerungseffekt.

Halbe übernommene Industrie erzeugt 75 offene Partien von 256 statt 43 beim
Bestandsmodell. Ein klarer Vorteil für frühen Einzelausbau gegenüber seiner
passenden Referenz bestätigt sich auf den Kontrollkarten nicht. Daher ist eine
pauschale Beschädigung eroberter Industrie kein bevorzugter Balancehebel.

**Stand beim Studienabschluss:** Die getesteten Kosten waren ausschließlich
Prüfvarianten.
Zum Studienabschluss war keine neue spielbare Regelversion freigegeben oder
in W6 eingebaut; die spätere Übernahme als W7 ist unten dokumentiert.
Der konkrete Vorschlag ist die günstige Anfangskurve ohne zusätzlichen
Eignungsfaktor und ohne Änderungen an Eroberungen. Späterer Ausbau bleibt eine
Entscheidung über das einzelne Projekt und seinen nutzbaren Resthorizont.

### Was die späten Projekte aussagen

Diese LATE-Politik baut vor Runde 40 nicht aus; ihr Gegner baut ebenfalls nicht
industriell aus. Die spät betrachteten Systeme haben deshalb überwiegend noch
kleine Kapazitäten, nicht bereits C=25–50. Das ist ein Test spät erschlossener
oder bisher nicht ausgebauter Welten. Er beweist keine Rentabilität weiteren
Ausbaus auf voll entwickelten Industriewelten.

| Modell | Späte Fälle gesamt | Tatsächlich investiert | Mindestens eine Stufe fertig |
|---|---:|---:|---:|
| Heutige Kosten | 64 | 59 | 55 |
| Halbe Kosten | 64 | 59 | 58 |
| Günstiger Anfang | 64 | 59 | 58 |
| Systemeignung | 64 | 59 | 58 |
| Halbe eroberte Industrie | 64 | 62 | 60 |

Bei C=25 kostet die nächste Stufe heute 50 Punkte, bei C=45 bereits 90.
Unter der günstigen Annahme, dass ein investierter Punkt sonst ein Schiff
gebaut hätte und die neue Stufe anschließend jede Runde ein zusätzliches
Schiff liefert, dauert allein der Ausgleich nach Fertigstellung weitere
50 beziehungsweise 90 Runden. Bauzeit, Weg zur Front und ein kurzer Resthorizont
kommen hinzu. Metallknappheit verändert diesen Vergleich; zusätzliche Kapazität
kann auch Förderung ermöglichen, aber ohne verbleibende Vorkommen erzeugt sie
kein neues Metall. Bei C=50 ist kein weiterer Ausbau zulässig.

Es gibt deshalb keine allgemeine Regel „ab Runde X nur noch Flotte“.
Entscheidend sind Größe und Preis des konkreten Projekts, verfügbare Rohstoffe,
Bedrohung, Weg zur Front und nutzbarer Resthorizont. Eine noch kleine, sichere
Welt kann spät ein günstiges Projekt bieten, während auf einer großen Frontwelt
zusätzlicher Ausbau häufig zu spät wirkt. Der Vergleich hält die gemeinsame
Förderplanung fest; er optimiert nicht die gesamte späte Ressourcenwirtschaft.

### Nachvollziehbarkeit

Rohdaten unter `build/investment/`: Je Modell und Kartenfamilie liegen dort
`invocation.txt`, `matches.csv`, `rounds.csv`, `complete.txt` und `analysis.json`.
Abgeschlossene Verzeichnisse enthalten jeweils 128 Partien. `rounds.csv` hält
vor jeder Produktion eigene Systeme, Kapazität, Schiffe, Ausbau, tatsächlichen
metallgedeckten Schiffbau und Vorrat fest. `matches.csv` ergänzt investierte
Punkte, gebaute Kapazität, übernommene Kapazität und erste Investitions-/Abschlussrunde.
Übernommene Kapazität zählt Eroberungsereignisse, also Rückeroberungen erneut;
sie ist kein Nettogewinn und kein Preis pro siegreichem System. Mittelwerte
zu festen Runden verwenden nur Paare, in denen beide Verläufe diese Runde erreichen.

Quellhash: `cc5b37117d53d376c71afcb944db7c45912ba1c30ddf4ee8de0813e62df311c9`.

Reproduzieren: Für jedes Modell `BASELINE`, `HALF_COST`, `EARLY_COST`,
`QUALITY_COST`, `HALF_CAPTURE` die folgenden Aufrufe ausführen; unter Windows
`.\gradlew.bat` verwenden.

```sh
./gradlew strategyBenchmark '-PstrategyArgs=--mode=investment --model=EARLY_COST --first-seed=7101 --seeds=1 --combat-seeds=2 --rounds=150 --output=build/investment/calibration-EARLY_COST'
./gradlew strategyBenchmark '-PstrategyArgs=--mode=investment --model=EARLY_COST --first-seed=8101 --seeds=1 --combat-seeds=2 --rounds=150 --output=build/investment/control-EARLY_COST'
```

Technische Prüfung: 874 Java-Tests ohne Fehler oder Überspringungen, vollständiger
Build einschließlich SpotBugs und Coverage-Grenzen sowie beide Frontend-Prüfdateien
bestanden. Das war der Prüfstand vor W7; persönliche Abnahme und öffentliche
Freigabe bleiben offen.


### Übernahme des Kostenmodells als W7

Nach Zustimmung ist die untersuchte EARLY_COST-Kurve als interne Regelversion
`spaceward/1.2.0-w7` umgesetzt. Im normalen Assistenten sind W7 (Vorgabe)
und W6 wählbar; das frühere Profil `economy-preview` ist nicht mehr erforderlich.
Die oben dokumentierten Studien bleiben historische W6-Gegenversuche mit ihrem
angegebenen Quellhash; sie wurden nicht nachträglich als W7-Partien umetikettiert.
W6 behält 2C, W7 verwendet min(2C, max(2, ceil(C²/10))). Umweltfaktor und
Eroberungseingriff bleiben ausschließlich Testvarianten. Die produktive
W7-Ausbaurechnung wird für alle gültigen Kapazitäten, Zuteilungen und
Fortschrittsstände gegen das untersuchte Kostenmodell geprüft.
Persönliche Abnahme und öffentliche Freigabe bleiben offen.

W7-Prüfung: 895 Java-Tests ohne Fehler oder Überspringungen, vollständiger
Build einschließlich SpotBugs und Abdeckungsgrenzen sowie beide Frontend-
Prüfdateien bestanden. Kein erneuter großer Strategievergleich: Die oben
angegebenen Ergebnisse stammen weiterhin aus der dokumentierten Studie.

Auswahlbereinigung vor der PR-Vorbereitung: 899 Java-Tests, vollständiger Build
mit SpotBugs und Abdeckungsgrenzen sowie beide Frontend-Prüfdateien bestanden.
Zusätzliche Regressionen prüfen W6/W7 im Assistenten, Versionsschutz bei
Neuanlagen/Vorlagen und die Fortsetzung historischer Bündnispartien.
Eine vollständige Browser-E2E-Prüfung wurde dabei nicht erneut ausgeführt.
