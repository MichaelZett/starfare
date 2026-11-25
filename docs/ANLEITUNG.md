# Starfare – Spielanleitung

Stand: 1.0.0-SNAPSHOT, 6. Oktober 2026. Die Anleitung beschreibt die
Desktop-Oberfläche. Technische Einrichtung und Start der Anwendung stehen in
der [README](../README.md).

Gespielt wird unter **[starfare.zettsystems.de](https://starfare.zettsystems.de)**.
Fehler, Ideen und Fragen nimmt der Bereich
[Issues](https://github.com/MichaelZett/starfare/issues/new/choose) auf GitHub
entgegen. Dafür brauchst du ein kostenloses GitHub-Konto. Wähle **Fehlerbericht**
oder **Idee** und fülle das Formular aus, auf Deutsch oder Englisch. Gib dabei
Spielvariante, Regelversion und Runde an, bei Fehlern außerdem die Schritte und
möglichst einen Screenshot. Issues sind öffentlich, schreibe deshalb keine
persönlichen Daten hinein. Ohne GitHub-Konto erreichst du den Betreiber über die
Kontaktadresse im Impressum.

## Schnellstart

1. Melde dich auf der Spieleplattform an und öffne Starfare. Im eigenen Start
   (lokal) spielst du ohne Anmeldung als Standardkonto gegen den Computer.
2. Erstelle in der Lobby ein **Neues Spiel** oder tritt einer verfügbaren Partie bei.
3. Der Gastgeber startet die Partie, sobald alle menschlichen Plätze besetzt sind.
4. Wähle auf der Karte ein eigenes System und danach ein anderes System als Ziel.
   Lege die Schiffsanzahl fest und merke den Sendebefehl vor.
5. Prüfe den Reiter **Flotten** und klicke auf **Runde abgeben**.
6. Sobald alle Menschen abgegeben haben oder die Frist abläuft, wird die Runde
   ausgewertet. Sieh dir anschließend den **Bericht** an und bestätige offene Schlachten.

## Partie erstellen und beitreten

Starfare setzt Sector Forces um, ein C64-Spiel der 80er-Jahre: Jedes System baut
Schiffe, Flotten erobern fremde Systeme, und wer genug der Galaxie beherrscht,
gewinnt. **Neues Spiel** in der Lobby öffnet den Dialog **Neue Partie**. Er passt auf
eine Seite; die Auswahl triffst du über Bildkarten und Klapplisten. Ein Klick auf
**Spiel anlegen** legt die Partie an.

**Partie**

- **Name**: Der Dialog schlägt einen zufälligen Namen vor; der Würfel daneben würfelt
  einen neuen.
- **Regeln**: **Klassisch** ist vorausgewählt; die folgenden Spielregeln beschreiben
  sie. **Erweitert** ergänzt Industrieausbau, Metall und Schiffsforschung, begrenzte
  Reichweite, Bündnisse und gemeinsame Schlachten. Die Abschnitte „Erweiterte Regeln“
  weiter unten beschreiben, worin sie von den klassischen Regeln abweichen.
- **Name des Imperiums**: So erscheint dein Reich auf der Karte und im Bericht.

**Mitspieler**

- **Spieler** 2–10 (Vorgabe 3) und **Davon Computer** 0 bis Spieler minus 1 (Vorgabe:
  alle anderen). Änderst du die Spielerzahl, bleibt „alle anderen sind Computer“
  erhalten; eine gemischte Runde behält ihre Computerzahl.
- **Computerstärke** **Einfach**, **Normal** (Vorgabe) oder **Anspruchsvoll**, siehe unten.
- **Spieltempo** nur bei mehr als einem Menschen: die Rundenfristen (Live, Zügig,
  Täglich, Gemütlich).
- **Nur auf Einladung**: Sonst ist die Partie öffentlich und für andere angemeldete
  Spieler auffindbar. Einladen kannst du im Warteraum.
- **Tutorial**: Die Partie zeigt Hilfen zum Lernen, die es im normalen Spiel nicht
  gibt. Zurzeit ist das eine gestrichelte Raute am System mit der Zahl freier Schiffe,
  wenn dort mehr Schiffe stehen, als das System in einer Runde baut. Alle Teilnehmer
  sehen in Warteraum und Kopfzeile, dass es ein Tutorial ist; es zählt nicht in der
  Statistik der Plattform.

Sind alle anderen Sitze Computer, startet die Partie sofort und die Karte öffnet sich.
Sonst wartest du im Warteraum, bis alle menschlichen Plätze besetzt sind, und startest
dann selbst.

**Karte**

- **Kartenform**: **Gleichmäßig** verteilt die Systeme ohne abgelegene Startplätze,
  **Zufällig** erzeugt Haufen und Lücken.
- **Galaxiegröße**: **Klein** 16, **Mittel** 24 (Vorgabe), **Groß** 40 oder **Riesig**
  64 Systeme. Die Systemdichte ist auf jeder Karte gleich, größere Karten sind also
  weiter. Größen mit weniger als zwei Systemen je Spieler sind gesperrt.
- **Höchste Produktion** 6, 10 (Vorgabe), 15 oder 20: Neutrale Systeme produzieren
  1 bis zu diesem Wert, meist um die Mitte.

Flotten fliegen auf jeder Karte gleich schnell: 160 Bildpunkte der Karte je Runde,
mindestens 2 und höchstens 40 Runden. Benachbarte Systeme liegen meist etwa drei
Runden auseinander; quer über die ganze Karte sind es etwa 19 (Klein), 23 (Mittel),
29 (Groß) oder 37 Runden (Riesig).

Jedes Reich startet mit einem Heimatsystem mit Produktion 4 und 8 Schiffen; die
Farben vergibt Starfare der Reihe nach. Der **Startausgleich nach Lage** ist immer
aktiv: Wer nah an Gegnern startet, bekommt mehr Schiffe; wer viele neutrale Systeme in
der Nähe hat, weniger. Jede Startgarnison bleibt zwischen der Hälfte und dem
Doppelten des normalen Werts.

**Zufall**

| Stufe | Kampfstärke | Ereignisse (klassisch) | Mehrere Angreifer |
| --- | --- | --- | --- |
| Kein | genau nach Stärke | keine | stärkster zuerst |
| Wenig (Vorgabe) | ±5 % je Seite | selten | zufällige Reihenfolge |
| Mittel | ±10 % je Seite | normal | zufällige Reihenfolge |
| Viel | ±20 % je Seite | häufig | zufällige Reihenfolge |

In den erweiterten Regeln kämpfen alle feindlichen Seiten gleichzeitig; dort wirkt
die Stufe nur auf die Kampfstärke.

**Details** (aufklappbar, nur bei klassischen Regeln)

- Die **Galaxie-Ereignisse** einzeln, solange der Zufall nicht auf **Kein** steht.
- **Spielende nach Runden**: aus oder 10–500 Runden.

Fest eingestellt sind: Sieg mit 70 % aller Systeme, Zuschauer und Wiedereinstieg
erlaubt, Schlachten zunächst mit Wiedergabe (eine gespeicherte persönliche
Geschwindigkeitswahl hat Vorrang). In den erweiterten Regeln sind Bündnisse und
Bündnissiege (gemeinsam 70 %) erlaubt.

Auf der gemeinsamen Spieleplattform hat jede Variante eine Stufe (Alpha, Beta,
Produktion); die erweiterten Regeln sind derzeit in Alpha. Wer eine Variante nutzen
darf, entscheidet die Plattform; die Regelkarte kennzeichnet Alpha und Beta. Wer eine
Variante nicht mehr nutzen darf, erreicht auch ihre Partien nicht mehr, auch laufende
und die Nachbetrachtung. Im eigenen Start von Starfare stehen beide Varianten allen
offen. Die Variante bleibt für die Partie fest; Vorlagen und Revanchen übernehmen sie.

Die Lobby zeigt offene und laufende Partien. Suche nach Partie- oder Spielernamen
und nutze den Filter für eigene, offene oder laufende Partien. Beendete Partien
findest du im **Archiv** über **Mein Bereich**.

### Computerstärke

| Stärke | Spielweise (klassisch) | Start der Computer (klassisch) |
| --- | --- | --- |
| Einfach | Basisstrategie: stärkste Basis, nahes Ziel, begrenzter Versand | halbe Produktion und Schiffe (2 und 4) |
| Normal | Basisstrategie | wie Menschen (4 und 8) |
| Anspruchsvoll | im Zweikampf verdeckt Basisstrategie oder früher Angriff, mit mehreren Reichen die Basisstrategie | anderthalbfache Produktion und Schiffe (6 und 12) |

Die Werte sind mit dem Strategieprüfstand abgestimmt: Wer wie die Basisstrategie spielt,
gewinnt im Zweikampf gegen **Einfach** etwa drei bis vier von fünf Partien, gegen
**Normal** etwa jede zweite und gegen **Anspruchsvoll** etwa jede fünfte; mit mehreren
Reichen liegt **Normal** beim fairen Anteil, **Einfach** darüber und **Anspruchsvoll**
darunter. Welche Heimat welcher Sitz bekommt, wird
ausgelost. In den erweiterten Regeln spielen normale Computer verdeckt früher Angriff,
Expansion, Kräftekonzentration oder Verteidigung, anspruchsvolle den adaptiven
Industrieausbau; sie starten mit ×0,75, ×1 oder ×1,25.

Die Computer nutzen nur sichtbare Informationen; verborgene Garnisonen und
gegnerische Befehle erhalten sie nicht. Die Stärke wird mit der Partie gespeichert
und von Vorlagen und Revanchen übernommen.

## Karte lesen und bedienen

Eigene Systeme tragen deine Farbe, gegnerische Systeme die Farbe des Besitzers,
neutrale Systeme sind grau. Die sichtbaren Informationen hängen vom Kriegsnebel ab:

| Sicht | Angezeigte Informationen |
| --- | --- |
| Eigenes System | Garnison und Produktion genau |
| Sensorreichweite | Aktueller Besitzer, geschätzte Garnison und Produktion (`~G:`, `~P:`) |
| Außerhalb der Reichweite | Letzte Aufklärung aus einem Kampf oder unbekannte Werte; das Alter steht im Hinweistext |

Sensorreichweite reicht zwei Reiserunden um eigene Systeme und Flottenziele.
Die Größe eines Systemzeichens richtet sich nach der sichtbaren Produktion;
unbekannte Systeme haben eine einheitliche Größe.

- Ziehe den Kartenhintergrund mit gedrückter linker Maustaste zum Verschieben.
- Nutze die sichtbaren Kartenknöpfe für Zoom, die ganze Galaxie oder das eigene
  Reich. **Kartenhilfe** enthält Zurücksetzen und die Erklärung der Verbindungslinien.
- Klicke auf ein System oder eine Flotte, um **Details** zu öffnen.
- Fokussierbare Kartenpunkte und Aktionskacheln lassen sich mit **Enter** oder
  **Leertaste** auslösen. Mit **Tab** wechselst du den Fokus.
- Verschiebe die Trennlinie zwischen Karte und Seitenleiste, um die Breite anzupassen.
- Ein Berichtsmarker führt zur zugehörigen Meldung. Eine Meldung zentriert die Karte
  auf das betroffene System.

### Auf dem Handy

Handys bekommen eine eigene Oberfläche; Tablets, Laptops und Desktop-Rechner
behalten die gewohnte. Die Wahl fällt einmal je Sitzung anhand des Browsers.
Mit `?layout=mobile` oder `?layout=desktop` an einer beliebigen Adresse legst du
sie für die restliche Sitzung selbst fest.

Auf der Karte zeigt das Handy oben einen kompakten Kopf mit Partie, Runde,
Reichsübersicht, **Runde abgeben** und dem Menü (Rundenregeln, Verlassen, Aufgeben,
Lobby). Darunter füllt die Karte den Bildschirm; unten wechselt eine
Leiste zwischen **Karte** und **Übersicht**. Die Übersicht ist die Seitenleiste
als Vollbild, dazu Bündnisse und Sammelbefehle.

- Wische zum Verschieben; zoome mit zwei Fingern oder den runden Knöpfen am
  rechten Rand. Diese passen die Ansicht auch an das eigene Reich oder die ganze
  Galaxie an; das Fragezeichen öffnet die **Kartenhilfe** mit der Legende.
- Ein Tipp auf ein System öffnet seine Details in der Übersicht.
- **Flotte senden** oder **Verlegung anlegen** führen zurück zur Karte. Ein Hinweis
  bleibt sichtbar, bis du das Ziel antippst oder **Zielwahl abbrechen** wählst.
- Dialoge füllen den Bildschirm; ihre Knöpfe bleiben unten erreichbar.

Im Spiel und im Kartenkopf erscheinen Imperiumsnamen. Spielernamen in Klammern
stehen nur in Übersichten und in der Diplomatie.

Die Seitenleiste enthält **Details, Flotten, Verlegungen und Bericht**. Bei wenig
Platz ersetzt das Auswahlfeld **Ansicht** die
Reiter; es bietet dieselben Bereiche. Sammelversand und **Rückgängig** stehen direkt am Kopf der Seitenleiste. Die kurzen Knopfbeschriftungen
zeigen den vollständigen Aktionsnamen als Hilfetext. Einzelversand und Verlegungen behalten den aktuellen
Reiter; Sammelbefehle öffnen den Reiter **Flotten**, eine ausgewertete Runde den Bericht. Aktualisierungen unveränderter Inhalte erhalten Eingaben und
Fokus.

In den Systemdetails stehen versendbare Schiffe und Gesamtbestand oben;
außerdem die erwartete Schiffproduktion pro Runde. **Flotte senden** und
**Verlegung anlegen** stehen direkt darunter, danach die ein- und ausgehenden
Verlegungen des Systems. **Logistik** öffnet die Verlegungsbilanz (eingehend,
Produktion, ausgehend) und die Ankünfte; bei Kolonien in erweiterten Partien
heißt dieser Bereich **Kolonie & Logistik** und enthält auch die Kolonieangaben.
Für fremde Systeme außerhalb der eigenen Sicht zeigen die Details die **Letzte
Aufklärung**: Besitzer, gezählte oder geschätzte Schiffe, Produktion und die Runde
des letzten Kontakts.
**Hintergrund** enthält Beschreibungen, Umweltwerte und Besitzhistorie. Die geöffneten
Bereiche bleiben bei Aktualisierungen und Systemwechseln geöffnet; die Angaben folgen der
Auswahl. Hintergrundwerte folgen dem Kriegsnebel.

## Flotten und Befehle

### Einzelne Flotte senden

Rechtsklick auf ein eigenes System beginnt direkt die Zielwahl. Alternativ
wähle das System mit Linksklick und nutze **Flotte senden** in den Details.
Klicke anschließend auf das Ziel. Stelle die Menge per Regler,
Eingabefeld oder Schnellknopf ein. **Alle** nutzt die verfügbaren Schiffe nach
Abzug bereits vorgemerkter Sendungen. **Alle außer Produktion**
behält den Nettozufluss einer Runde zurück: Produktion + eingehende − ausgehende Verlegungen, mindestens null. Eingehende Schiffe bleiben bis zu ihrer tatsächlichen Ankunft unterwegs.

**Abbrechen**, Escape und ein Klick außerhalb des Dialogs erhalten die bisherige
Auswahl und Zielwahl. Nach dem Bestätigen erscheint eine kurze Meldung; die
Quellsicht bleibt geöffnet, sodass du gleich einen weiteren Auftrag beginnen kannst.

Der Auftrag erscheint unter **Flotten**. Solange du die Runde noch nicht abgegeben
hast, kannst du ihn **Stornieren** oder mit **Befehl bearbeiten** Menge und Ziel
ändern. Ein Sendebefehl darf nicht mehr Schiffe vormerken, als am Ausgangssystem
verfügbar sind.

**Letzten Befehlschritt zurücknehmen** stellt die eigene Befehlsliste vor der
letzten Änderung wieder her. Das gilt auch für einen Sammelversand. Es gibt einen
Rücknahmeschritt, keine mehrstufige Historie. Er bleibt nach Neuladen und Neustart
erhalten, endet aber mit dem Rundenwechsel. Verlegungen gehören nicht zu dieser
Befehlsrücknahme.

### Gleichzeitig ankommen und gemeinsam senden

Klickst du auf der Karte ein fremdes System oder eine eigene fliegende Flotte an,
zeigt jedes eigene System mit freien Schiffen links ein Abzeichen mit der Runde,
in der eine jetzt gesendete Flotte dort ankäme (`R14`). Gerechnet wird über
dieselben Routen wie beim Senden, in erweiterten Partien also mit Reichweite und
Zwischenhalten; Systeme ohne Weg dorthin bekommen kein Abzeichen.

Gibt es schon eine Ankunft, nach der du dich richten kannst, vergleichen die
Abzeichen damit: bei einer gewählten Flotte ihre Ankunft, bei einem gewählten
System die früheste eigene Flotte oder vorgemerkte Sendung dorthin.

- **Grün mit ✓:** kommt in derselben Runde an.
- **Gelb mit `+1 ⏸`:** käme so viele Runden zu früh; eine Runde später senden
  oder die Flotte unterwegs warten lassen.
- **Grau:** käme später an.

Über dem Ziel steht **Gemeinsam senden**, in Klammern die Systeme, die
gleichzeitig ankommen. Der Knopf öffnet das gemeinsame Senden mit Ziel und
diesen Systemen vorausgewählt:

1. Passe die Auswahl der eigenen Systeme bei Bedarf an.
2. Nutze **Alle verfügbaren Schiffe** oder gib eine feste Menge **je System** an.
3. Prüfe die Vorschau mit den Summen je Ankunftsrunde und bestätige mit
   **Flotten gesammelt senden**.

Die Aktion wird als Ganzes geprüft. Ist eine Menge ungültig oder inzwischen die
Runde gewechselt, wird keine Teilmenge der Befehle übernommen.

### Unterwegs und Ankunftsvorschau

Der Reiter **Flotten** zeigt reisende Flotten und die in dieser Runde vorgemerkten
Befehle in einer Liste, gruppiert nach Ankunftsrunde; innerhalb einer Runde stehen
die reisenden Flotten vor den Befehlen. Der aufklappbare Bereich
**Gemeinsame Ankünfte** summiert reisende Flotten und vorgemerkte Sendungen nach
Ziel und Ankunftsrunde. Die Regeln für Aufenthalte an Zwischenstationen stehen
in der Kartenhilfe.
Die Routensuche schränkt die angezeigten Karten ein.

**Warten** steht direkt an der Flottenkarte und in ihren Details und verschiebt
eine Flotte um eine Runde; die Vorschau berücksichtigt das ohne Doppelzählung.
**Auflösen** erscheint nur bei einer gerade gestarteten oder an einem eigenen
System stationierten Flotte und führt ihre Schiffe in die Garnison zurück.
Rückzugsflotten bieten diese Aktionen nicht an. Beide Aktionen werden als Befehle
für die Rundenabgabe vorgemerkt. Ein Aktionsklick behält den aktuellen Reiter.

## Produktionsverlegungen

Eine **Produktionsverlegung** sendet jede Runde eine feste Anzahl Schiffe.
Ziehe ein eigenes System auf das Ziel oder wähle die Verlegung im Sendedialog.
Unter **Verlegungen** und direkt an Quelle oder Ziel stehen **Bearbeiten** und **Aufheben** nebeneinander. Bearbeiten öffnet die vorhandene Route; beim Speichern wird sie ersetzt.
Beim Aufheben der Route bleiben bereits
reisende Flotten erhalten. Hinter der Produktion steht der eingehende
Zufluss, zum Beispiel `4 (+6/Runde)`. Das gemeinsame Verlegungslimit beträgt
dann 10, abzüglich anderer ausgehender Aufträge.

Das gemeinsame Limit aller ausgehenden Verlegungen ist die eigene Produktion plus
die eingehenden Verlegungsmengen. Eingehende Schiffe brauchen trotzdem ihre normale
Reisezeit. Wenn nötig, nutzt die Verlegung vorhandene Garnison. Nicht versandte
Produktion bleibt am System. Die Karte zeigt die Routen
violett gestrichelt mit ihrer Menge; der Reiter **Verlegungen** listet alle Routen.

## Runden, Fristen und Kommunikation

Nach **Runde abgeben** ist der Knopf gesperrt. Die Rundenleiste zeigt abgegebene
Spieler und nennt diejenigen, auf die noch gewartet wird. Sobald alle Menschen
abgegeben haben, plant die KI auf demselben Ausgangsstand. Dann werden Befehle,
Produktion, Ankünfte und Kämpfe ausgewertet.

Bei mindestens zwei Menschen gelten zwei Uhren: das Rundenlimit ab Rundenbeginn
(Vorgabe fünf Minuten) und das Nachzüglerlimit, sobald nur noch eine Abgabe fehlt
(Vorgabe eine Minute). Die frühere Frist beendet die Runde. Säumige spielen mit
ihren vorhandenen Befehlen. Versäumte Abgaben zählen über die ganze Partie: Beim zweiten Mal
fliegst du hinaus, die KI übernimmt deinen Sitz endgültig, und ein Wiedereinstieg ist auch dann
nicht möglich, wenn die Partie ihn sonst erlaubt. Den Chat kannst du weiter lesen.
Der Gastgeber kann über das Kartenmenü die **Rundenfristen** anpassen.
Eine Partie mit nur einem Menschen hat keine automatische Rundenfrist.

Den **Partie-Chat** stellt die Plattform: Sein Knopf sitzt in ihrer schmalen Leiste über der Karte
und im Warteraum, auch auf dem Handy. Den Chat lesen alle, die einen Sitz in der
Partie haben oder hatten. Schreiben kann nur, wer noch mitspielt: Wer verlassen oder aufgegeben
hat oder seinen Sitz durch verpasste Runden an die KI verloren hat, liest nur noch mit, und nach
dem Ende der Partie ist der Chat für alle nur noch lesbar. Zuschauer haben keinen Chat. **Zur Lobby** verlässt nur die
Kartenansicht. **Partie verlassen** übergibt deinen Sitz an die KI. Erlaubt die Partie
den Wiedereinstieg, kannst du später zurückkehren.

## Schlachten ansehen oder sofort auswerten

Eine offene Kampfmeldung zeigt zunächst den Ort. Besitzer und Ergebnis am
betroffenen System bleiben verdeckt, bis du die Schlacht bestätigt hast.
Klicke die Meldung an, um die Wiedergabe zu öffnen, und bestätige mit
**Ergebnis übernehmen**. Die Wiedergabe nennt über Schiffen und Stärken auch die Produktion
des Systems, soweit du sie kennst; einen eigenen Eroberungsdialog gibt es nicht. Die
Auswahl verändert ausschließlich die Darstellung; die Runde ist zu diesem Zeitpunkt
bereits berechnet.

Im Bericht wählst du die **Geschwindigkeit**:

| Auswahl | Wirkung |
| --- | --- |
| 0,5× | Langsame Wiedergabe |
| 1× | Normale Wiedergabe |
| 1,5× / 2× | Beschleunigte Wiedergabe |
| Sofort | Ergebnisse ohne Animation auswerten |

Bei **Sofort** erscheint **Alle offenen Schlachten auswerten**. Damit bestätigst du
alle noch offenen Kämpfe der Runde, auch solche, die ein Berichtsfilter ausblendet.
Während der Wiedergabe kannst du die Geschwindigkeit ändern und **Kampfgeräusche**
ein- oder ausschalten; beim Einschalten erklingt ein kurzer Treffer. Der Browser
benötigt zuvor eine Nutzeraktion, um Ton freizugeben.

Bestätigungen bleiben je Konto, Partie, Runde und Ereignis gespeichert. Ein
Neuladen oder anderes Gerät setzt sie nicht zurück. Geschwindigkeit und Ton ändern
weder die Kampfstärke noch Rundenfristen oder Ergebnisse.

## Sieg, Fortsetzung und Archiv

Gewonnen hat, wer 70 % aller Systeme kontrolliert; neutrale Systeme zählen zur
Gesamtzahl. Wenn die letzte Runde noch
offene Schlachten enthält, erscheint zunächst der Bericht. Erst nach deren
Bestätigung öffnet sich das Ergebnis mit persönlichen Schiffssummen. Erweitert
zeigt zusätzlich die Wirtschaft der Partie: gebaute Industrie in Kapazitätspunkten
und, mit Metall, gefördertes und verbrauchtes Metall. Recycling zählt nicht als
Förderung. Fehlt die Erfassung für ältere Partien, steht dort „nicht erfasst“;
beginnt sie später oder hat Lücken, nennt der Dialog „seit Runde N“ bzw.
„unvollständig erfasst“.

Der Sieger darf **Bis zur vollständigen Eroberung weiterspielen** wählen. Das Ziel
steigt dann auf 100 %, die Abgaben werden zurückgesetzt und die Fristen starten neu.
Die anderen Teilnehmer werden automatisch informiert. Das zuerst erreichte
Ergebnis bleibt in der Statistik der Plattform bestehen. Endet eine Partie nach klassischen Regeln
durch ihr Rundenlimit, gibt es keine Fortsetzung.

**Aufgeben** steht in der Kopfleiste neben **Verlassen**. Nach einer Rückfrage übernimmt
der Computer dein Reich bis zum Ende der Partie. Anders als beim Verlassen kannst du
nicht zurückkehren, auch wenn die Partie Wiedereinstieg erlaubt.

## Galaxie-Ereignisse und Wertung (Klassisch)

Nach dem C64-Klassiker Galactic Empire (1983). Wie oft Ereignisse eintreten, folgt der
**Zufallsstufe** der Partie (Kein: nie, Wenig: selten, Mittel: normal, Viel: häufig). Unter
**Details** lässt sich jedes Ereignis einzeln abwählen. Meldungen erscheinen im Bericht als
Ticker-Karten:

- **Kosmische Stürme:** Alle Flotten im Hyperraum, auch Piraten, kommen eine Runde später an.
- **Raum-Zeit-Erschütterung:** Alle Flotten im Hyperraum brauchen nur noch die halbe Restzeit.
- **Mysteriöses Verschwinden:** Eine zufällige Flotte verliert bis zur Hälfte ihrer Schiffe.
  Nur ihr Besitzer erfährt davon.
- **Raumhafenbeben:** Ein zufälliges System verliert einen Teil seiner Garnison (leichtes oder
  schweres Beben). Nur der Besitzer erfährt davon.
- **Supernova:** Der Bericht meldet allen „Sonne instabil“, die Karte zeigt das System mit
  pulsierendem Rand. Am Ende dieser Runde explodiert die Sonne; bis dahin kannst du stationierte
  Schiffe abziehen. Danach ist das System für immer zerstört (dunkel auf der Karte), zählt nicht mehr zur Siegschwelle und kann nicht
  mehr angeflogen werden. Seine Garnison flieht als Flotte zu einem zufälligen System;
  Flotten auf dem Weg dorthin springen per Panikschaltung zu einem anderen System weiter.
  Heimatwelten explodieren nie.
- **Piraten** (nur zusammen mit der Supernova): Die Garnison einer neutralen Sonne flieht
  als Piratenflotte ohne Sitz. Gewinnt sie den Kampf am Ziel, wird das System neutral.

**Spielende nach Runden** (10 bis 500): Erreicht bis dahin niemand die Siegschwelle, endet
die Partie nach der letzten Runde. Es gewinnt die höchste Wertung: 400 Punkte je System,
25 je stationiertes Schiff, 20 je Schiff unterwegs; bei Gleichstand mehr Systeme, dann der
niedrigere Sitz. In den letzten fünf Runden kündigt der Bericht das Ende an; die Wertung
aller Reiche steht im letzten Bericht.

Im **Archiv** öffnet **Ansehen** die Nachbetrachtung. Such- und Ergebnisfilter bleiben bei Rückkehr innerhalb
derselben Sitzung erhalten; die Liste führt zur zuletzt angesehenen Partie.

Im **Archiv** kannst du die beendete Karte lesend öffnen, eine Teilnehmerperspektive
wählen und den Kriegsnebel umschalten. Die Zeitleiste zeigt gespeicherte Runden ohne
Kriegsnebel; bei **Endstand** gelten wieder Perspektive und Nebelschalter. Ältere
Partien ohne gespeicherte Runden bieten nur den Endstand. Private Archive bleiben
auf Berechtigte beschränkt; öffentliche erlauben fremde Zuschauer nur bei aktivierter
Zuschaueroption. Deine Ergebnisse zeigt die **Statistik** der Plattform über alle
Spiele; Starfare selbst führt keine eigene Statistik.

## Erweiterte Regeln: Wirtschaft

Erweitert verteilt die Kapazität eines Systems zwischen Schiffbau und Industrieausbau.
Der Ausbau sammelt Punkte; eine zusätzliche Kapazitätseinheit kostet das Doppelte
der aktuellen Kapazität. Das Maximum ist 50. Neue Kapazität wirkt ab der folgenden
Runde. Fortschritt bleibt bei Umschaltung und Eroberung erhalten; ein Eroberer
beginnt mit vollem Schiffbau.

Die Systemdetails zeigen Industriegebäude, Verteilung, Fortschritt und die
voraussichtliche nächste Lieferung. Bestehende Verlegungen bleiben erhalten;
reicht der Schiffbau nicht, können sie auf die Garnison zugreifen.
Engpässe werden angezeigt. Rohstoffe, Forschung und Schiffstypen folgen später.
Klassisch verwendet weiterhin seine bisherigen Regeln.

Die nächste Wirtschaftsfassung wird zunächst intern geprüft. Der Zwischenstand
**1.2.0-w1** ergänzt eine gespeicherte Hauptkolonie je System. Bevölkerung und
Industrie bestimmen gemeinsam die nutzbare Kapazität; zusätzliche Arbeitskräfte
und fertiger Ausbau wirken erst in der nächsten Runde. Die Systemdetails zeigen
Bevölkerung, Wachstum, Eignung und den Engpass durch fehlende Arbeitskräfte.
Eroberung erhält Bevölkerung und Systemeigenschaften. Metallreichtum ist hier
eine gespeicherte Eigenschaft; Förderung und Recycling folgen später.
Dieser Abschnitt beschreibt den historischen W1-Stand. Neue Partien mit erweiterten Regeln
verwenden die aktuelle Regelfassung 1.3.0-r1; ältere Versionen bleiben fortsetzbar.

### Interne Metallwirtschaft 1.2.0-w2

Diese gesonderte interne Regelfassung ergänzt endliche Vorkommen je System und
Metallvorräte je Spieler. Ein Schiff kostet **1 Metall**. Der Vorrat steht im
gesamten eigenen Reich zur Verfügung; Bündnispartner teilen ihn nicht automatisch.
Die Spielleiste zeigt den Reichsvorrat; die Systemdetails zeigen Restvorkommen, Förderergiebigkeit und geplante Förderung
und den metallgedeckten Schiffbau. Förderung und Ausbau werden gemeinsam zugewiesen
und dürfen zusammen die nutzbare Industrie nicht überschreiten. Die übrige
Kapazität dient dem Schiffbau. Der Planungsdialog zeigt ihn als eigenen gekoppelten
Regler; die Vorschau reagiert vor dem Speichern auf jede Änderung. Nach Übernahme wird die reichsweite
Vorschau aktualisiert; sie verwendet den gespeicherten Plan.

Zuerst werden reguläre Flotten aus vorhandenen Schiffen verschickt. Danach erfolgt
reichsweit die Förderung und der Schiffbau. Bei Metallknappheit erhält jedes System
einen proportionalen Anteil seiner geplanten Schiffe; ganzzahlige Reste gehen nach
größtem Bruchrest, bei Gleichstand nach Systemkennung. Verlegungen verwenden danach
nur tatsächlich vorhandene Schiffe.
Industrieausbau kostet kein Metall; Wachstum und Ausbau wirken erst nächste Runde.
Unverbrauchtes Metall bleibt erhalten, unerfüllter Schiffbau erzeugt keine Schuld.

Erschöpfte Vorkommen bleiben erschöpft, auch bei Eroberung. Örtliche Restvorkommen
gehen an den neuen Besitzer, der Reichsvorrat bleibt beim bisherigen Spieler.
Neuladen und Nachbetrachtung verändern keine Rohstoffmengen. Interne Prüfwerte sind
10 Startmetall je Spieler, 200 Metallvorkommen je Reichtumsstufe und eine Fördermenge
je Reichtumsstufe und Kapazitätspunkt. W1 und ältere Partien behalten ihre Regeln.
Dieser Abschnitt beschreibt den historischen W2-Stand; neue Partien mit erweiterten Regeln
verwenden die aktuelle Regelfassung 1.3.0-r1.

## Erweiterte Regeln: Navigation

Jede Partie mit erweiterten Regeln erhält eine feste Reichweite je Reiseabschnitt.
Sie wird aus der Galaxie berechnet, damit alle Systeme schrittweise erreichbar
bleiben. Reisezeit und Sensorreichweite sind davon unabhängig.

Während der Zielwahl zeigt ein gestrichelter Kreis um die Quelle die direkte
Reichweite. Ziele tragen einen Zustand: durchgezogener Rahmen für direkt erreichbar,
gestrichelter Rahmen für „nur über Stationen“, abgeblendet für „kein Weg“. Der
Hinweistext und der Versanddialog nennen den Zustand ohne Prozentangabe:
„Direkt erreichbar“, „Über 2 Stationen“ oder „Kein Weg“. Klassisch hat keine
Reichweitengrenze und zeigt keinen Kreis.

Für ein entferntes Ziel sucht der Flottenversand automatisch den schnellsten
Weg über eigene oder verbündete Systeme. Der Dialog zeigt einen grafischen
Routenstreifen und einmal die Ankunftsrunde einschließlich der Aufenthalte.
Lange Routen lassen sich mit **+… Stationen** vollständig aufklappen.
Ein Klick auf einen bekannten Systempunkt markiert ihn und zentriert die Karte,
ohne Menge oder Versandmodus zu ändern. Die Reisedetails nennen Teilstrecken
und Aufenthalte. Ohne erreichbaren Weg ist der
Versand gesperrt. Unverbündete Systeme können angegriffen werden, dienen aber erst
nach ihrer Eroberung als Zwischenstationen.

An jeder Zwischenstation bleibt die Flotte eine volle Runde zum Auftanken.
Sie bleibt ein eigener Verband und wird nicht zur Garnison. Die Flottendetails
zeigen den Reiseplan und den frühesten Abflug. Mit **Warten** lässt sich die
Weiterreise verschieben; **Auflösen** fügt den Verband der aktuellen eigenen
Station hinzu. Die Aktionen stehen direkt an Flottenkarten und in den Details
bereit; am Flottenmarker gibt es weiterhin das Kontextmenü. Der Routenstreifen
kennzeichnet erreichte Abschnitte, aktuellen Aufenthalt und nächste Station.
Eine blockierte Reise verspricht keine Ankunft; **System ansehen** öffnet den
Standort, bei bekanntem fremdem Besitzer auch **Bündnisse ansehen**, sofern erlaubt.

Geht eine Station während des Fluges verloren, kämpft die Flotte dort nach den
normalen Regeln; ihre Weiterreise endet. Geht sie während des Aufenthalts
verloren, bleibt der Verband mit unterbrochener Weiterreise stehen. Seine
Ankunftsrunde ist dann offen. Nach Rückeroberung wird eine neue volle
Auftankrunde abgewartet. Produktionsverlegungen verwenden dieselben Wege und
Reisezeiten; ohne nutzbaren Weg pausieren neue Lieferungen, der Auftrag bleibt
erhalten. Verbündete Stationen verwenden dieselbe Reichweitenprüfung.

Ist bereits vor dem Abflug die nächste Zwischenstation verloren, bleibt die
Flotte an ihrer aktuellen Station, bis dieser Reiseabschnitt wieder zulässig ist.

Klassisch verwendet weiterhin direkte Flüge ohne Reichweitengrenze.

## Vorlagen und Revanche

Öffne bei einer eigenen Partie in der Lobby oder in der Nachbetrachtung einer beendeten Partie
(Leiste über der Karte) **Als Vorlage / Revanche**.
Vergib einen Namen. Mit **Bisherigen Teilnehmern ihre Plätze reservieren** werden
die früheren menschlichen Plätze reserviert; ohne Haken entsteht nur eine Vorlage
mit denselben Ausgangseinstellungen.

Die neue Partie erhält eine frische Karte, ist privat und startet nicht automatisch.
Die Teilnehmer müssen selbst beitreten, der Gastgeber startet anschließend.
Alte Partien ohne gespeicherte Ausgangseinstellungen zeigen einen Hinweis und
können nicht als Vorlage dienen.

## Persönliche Einstellungen und Konto

Geschwindigkeit, Ton, Berichtsfilter, Routensuche und Seitenleistenbreite
werden je Konto gespeichert und bei einer späteren Anmeldung wieder geladen.
Sprache bleibt sitzungsbezogen; Kartenposition und Zoom werden je Partie in der
Browsersitzung behalten.

**Mein Bereich** bündelt Lobby und Archiv. Konto, Kontolöschung, Impressum und
Datenschutz gehören der Spieleplattform und stehen in ihrem Rahmen.

## Erweiterte Regeln: Siegoptionen

Die Regelfassung 1.1.0 ergänzt zwei Partieeigenschaften: **Bündnisse erlaubt** und
**Bündnissiege erlaubt**. Neue Partien mit erweiterten Regeln haben beide eingeschaltet,
dazu Bündnisse der Computer; Einzel- und Bündnisschwelle liegen fest bei 70 %. Ältere
Partien behalten ihre gespeicherten Einstellungen. Klassisch bietet keine
Bündnisse.

Einzelsieg wird zuerst geprüft. Danach kann eine wirksame Gruppe mit mindestens
zwei Mitgliedern gemeinsam gewinnen. Gezählt werden ihre Systeme zusammen;
neutrale Systeme zählen weiterhin zur Gesamtzahl. Erfüllen mehrere Gruppen
die Bedingung, gewinnt die mit den meisten Systemen, bei Gleichstand die mit
der kleineren Gruppenkennung. Alle wirksamen Mitglieder gewinnen, auch solche
ohne Systeme. Kündigungsfristen gelten bis zum tatsächlichen Austritt.

Bei 16 Systemen und 70 % reichen zwölf Systeme gemeinsam: Partner mit 11 und
5 Systemen gewinnen als Gruppe, sofern Bündnissiege erlaubt sind. Ohne diese
Option braucht weiterhin ein einzelnes Imperium zwölf Systeme.

Dialog, Bericht, Archiv und die Statistik der Plattform berücksichtigen jeden Sieger.
Jeder Sieger darf einmal bis zur vollständigen Eroberung fortsetzen; dann
steigen beide Schwellen auf 100 %. Das erste Ergebnis bleibt
erhalten. Einstellungen bleiben in Vorlagen und Revanchen erhalten. Bestehende
Partien bleiben bei ihrer Regelversion; Fassung 1.0.0 behält ausschließlich
den Einzelsieg und erlaubte Bündnisse.

## Erweiterte Regeln: Bündnisse

**Bündnisse** zeigt zuerst offene Entscheidungen, danach dein Bündnis und
mögliche Partner. Zustimmen und Ablehnen stehen direkt am Vorschlag.
**Bündnis vorschlagen** am Partner sendet den Gründungsvorschlag sofort.
**Bündnis kündigen** verlangt eine Bestätigung mit der genauen Austrittsrunde
und den Folgen für stationierte Flotten. Abbrechen kündigt nichts.
Allgemeine Regeln stehen im aufklappbaren Hintergrundbereich.

Ein Imperium gehört höchstens einer Gruppe an.
Alle Mitglieder sind miteinander verbündet, neutrale Systeme gehören keiner
Gruppe an. Eine Gründung braucht beide Partner; eine Aufnahme braucht die
Zustimmung aller bestehenden Mitglieder. Ein Imperium beantragt seinen eigenen
Beitritt. Fremde Mitgliedschaft kann niemand erzwingen. Ablehnen oder
Zurückziehen verwirft den offenen Vorschlag.

Die Kündigungsfrist beträgt fest 3 Runden. Eine Kündigung zeigt sofort die
Wirksamkeitsrunde: aktuelle Runde plus 3. Bis zu deren Beginn gilt das Bündnis
vollständig. Gespeicherte Bündnisse aus älteren Fassungen behalten ihre
vereinbarte Frist; offene Friständerungen daraus verfallen zur nächsten Runde.
Bleibt nur ein Mitglied, endet die Gruppe. Die Bedienung ist nach Rundenabgabe
gesperrt, Verträge bleiben lesbar.

Verbündete Systeme können als Zwischenstationen dienen. Ankommende Unterstützung
bleibt eine eigene Flotte und übernimmt weder Garnison noch System des Partners.
Verbündete kämpfen gemeinsam (siehe „Erweiterte Regeln: Kampf“). Ein Vertrag verrät keine zusätzlichen militärischen
Werte und verändert die individuelle Siegschwelle nicht.

Nach wirksamem Austritt treten fremd stationierte Schiffe automatisch die
kürzeste erlaubte Heimreise zu einem eigenen System an. Das gilt auch für
bereits anfliegende Unterstützung: die Kündigung macht daraus keinen Angriff.
Ohne erreichbares eigenes Ziel bleibt die Heimreise gesperrt. Diese Flotten
greifen nicht an, unterstützen keine Verteidigung und können nicht durch
Wartebefehle geparkt oder in einer fremden Garnison aufgelöst werden.
Die KI stimmt Anfragen zu, sofern sie nicht bereits selbst einen Austritt
angekündigt hat; sie gründet keine Bündnisse von sich aus.

Beim Anlegen einer Partie mit erweiterten Regeln legt **KI darf Bündnisse eingehen** fest, ob
KI-Imperien neuen Bündnissen beitreten oder sie gründen dürfen (Vorgabe: ja). Ist
die Option aus, erscheinen KI-Imperien in der Bündnisauswahl mit dem Hinweis, dass
sie in dieser Partie keine Bündnisse eingehen; Bündnisse zwischen Menschen bleiben
möglich. Übernimmt die KI einen menschlichen Sitz, bleiben bestehende Verträge
samt Fristen und Kündigungen gültig; nur neue Beitritte sind gesperrt. Kehrt der
Mensch zurück, gelten wieder seine Rechte. Ohne erlaubte Bündnisse ist die Option
wirkungslos; ältere Partien behalten die bisherige KI-Teilnahme.

## Erweiterte Regeln: Kampf

Erweitert wertet Kämpfe für ganze Bündnisgruppen gemeinsam aus; Klassisch
verwendet weiter seinen bisherigen Kampf.

An einem System kämpfen Besitzer, eintreffende Verbündete und dort stationierte
Unterstützung als gemeinsame Seite. Andere Bündnisgruppen sowie vertragslose
Imperien bilden jeweils eigene Seiten. Eine neutrale Garnison bildet eine
weitere Seite. Alle Seiten teilen ihre Feuerkraft anteilig auf die feindlichen
Stärken auf. Verluste eines Schlagabtauschs werden gleichzeitig berechnet.
Falls mehrere feindliche Seiten überleben, kämpfen sie gleichzeitig weiter,
bis höchstens eine Seite übrig bleibt. Die Zufallsvorgabe der Partie gilt einmal
je Seite und Schlacht. Verluste werden auf ganze Schiffe aufgerundet; innerhalb
einer Seite verteilen sich die Überlebenden anteilig auf die eingesetzten
Imperien, dann auf Garnison und Flotten. Größte Reste erhalten die übrigen
ganzen Schiffe; Gleichstände folgen festen Kennungen.

Ohne Zufall behalten 100 Schiffe gegen 60 Gegner 40 Schiffe. Zwei verbündete
Flotten mit jeweils 50 Schiffen behalten gegen 60 Gegner je 20. Gleich starke
Seiten können sich vollständig vernichten. Dann bleibt der bisherige Besitzer,
auch wenn seine Garnison leer ist. Erfolgreiche gemeinsame Verteidigung erhält
den bisherigen Besitzer.

Vor dem Flottenversand lässt sich als Eroberungsziel das eigene oder ein
verbündetes Imperium wählen. Der Befehl zeigt die Wahl, auch nach Neuladen.
Bearbeiten und Rückgängig erhalten sie. Bei unterschiedlichen Zielen derselben
siegreichen Seite entscheidet das Imperium mit den meisten überlebenden
Schiffen; Gleichstand entscheidet die kleinere Spielerkennung. Innerhalb
jenes Imperiums gilt das mit den meisten Überlebenden vertretene Ziel, danach
die kleinere Zielkennung. Besteht das Bündnis beim Kampf nicht mehr, fällt
eine Benennung auf das eigene Imperium zurück. Sammelversand und regelmäßige
Verlegungen verwenden das eigene Imperium als Ziel.

Schiffe werden durch Eroberung weder verschenkt noch einer fremden Garnison
zugeschlagen: verbündete Überlebende bleiben eigenständige Kontingente.
Heimkehrende Schiffe und gesperrte Aufenthalte aus dem Navigationsablauf
beginnen keine zusätzlichen Kämpfe. Ein Kampf an einer verlorenen
Zwischenstation beendet die ursprüngliche Route dort.

Die Wiedergabe zeigt alle Seiten, die einmal ausgewürfelten Stärken und
anschließend die gespeicherten Verluste jedes Imperiums. Bis zur persönlichen
Bestätigung bleiben Besitzer, Endwerte und überlebende Unterstützung in Karte,
Flottenliste und Systemdetails verborgen. Auch Neuladen umgeht das nicht. Erst nach
der letzten offenen Schlacht erscheint ein ausstehendes Spielergebnis.
Der Ergebnisdialog zählt verlorene Schiffe exakt und teilen Abschüsse
anhand des verursachten Feuers auf, sodass jedes zerstörte Schiff insgesamt
nur einmal als Abschuss gezählt wird. Kampfentscheidungen und Flucht folgen
in einem gesonderten Ausbau.

### Internes Schiffsrecycling 1.2.0-w3

In dieser gesonderten internen Fassung können eigene stationierte Schiffe an
Systemen mit Industrie verschrottet werden. In den Systemdetails eine Menge
vormerken; **0 hebt den Befehl auf**. Die Schiffe bleiben bis zur Rundenauflösung
stehen, sind aber gemeinsam mit dem Versand reserviert.
Die Verschrottung erfolgt nach dem regulären Versand und vor Förderung und
Schiffbau. Sie benötigt keine zusätzlichen Kapazitätspunkte. Ankommende Schiffe
stehen erst in der folgenden Runde zum Recycling bereit.

Ein Schiff liefert **0,7 Metall** zurück. Zehntelreste werden reichsweit gespeichert:
zehn einzeln verschrottete Schiffe ergeben ebenso 7 Metall wie ein gemeinsamer
Befehl. Neue Schiffe benötigen weiterhin je 1 ganzes Metall. Die Vorschau
berücksichtigt die vorgemerkte Rückgewinnung, die tatsächliche Gutschrift erfolgt
nur einmal bei der Rundenauflösung und erscheint im Bericht. Kampfverluste liefern
kein Metall zurück. Startschiffe und neutrale Garnisonen zählen zur Metallbilanz.

Alle acht KI-Profile nutzen Recycling bei Metallknappheit für überschüssige Schiffe
im Hinterland; sie behalten eine Garnison und schonen Systeme mit bekannten nahen
Gegnern. Der Wirtschaftsvergleich für W4 steht in `docs/ECONOMY-EXPERIMENTS.md`. W2-Partien erhalten
keine Recyclingbefehle. W3 bleibt nur fortsetzbar; neue Partien mit erweiterten Regeln verwenden die aktuelle Regelfassung 1.3.0-r1.

### Erweiterte Regeln: immer die aktuelle Regelfassung

Im Dialog **Neue Partie** die Regelkarte **Erweitert** auswählen. Neue Partien verwenden
stets die aktuelle Regelfassung **1.3.0-r1**, einschließlich Metallförderung,
Recycling, Industrieausbau und Schiffsforschung. Eine Versionsauswahl oder ein
besonderes Startprofil ist nicht erforderlich. „Klassisch“ bleibt eine eigene
Spielvariante und verwendet ebenfalls ihre aktuelle Fassung.

Seit 1.2.0-w5 ist die Bevölkerung vorerst Atmosphäre: Sie wächst und beschreibt die
Kolonie, begrenzt aber nicht mehr die nutzbare Industrie. Die gesamte installierte
Kapazität steht für Schiffbau, Förderung und Ausbau bereit. Gespeicherte
W4-Partien behalten die Begrenzung durch Arbeitskräfte.

Ein Schiff benötigt 1 Metall. Förderung, Ausbau und Schiffbau teilen sich die
nutzbare Kapazität; Recycling gewinnt 70 Prozent zurück und speichert Zehntelreste.
Die Metallwerte bleiben für diese Erprobung unverändert. Alle acht KI-Profile
berücksichtigen die Förderung bei ihrer Industrie- und Flottenplanung.

Auch Vorlagen und Revanchen verwenden immer die aktuelle Fassung ihrer Variante.
Die ursprüngliche Partie wird dabei nicht verändert. Gespeicherte Partien mit
älteren Regeln lassen sich weiterhin laden und mit ihren bisherigen Regeln fortsetzen.

Bei Metallknappheit erhalten die Werften seit W6 gleich große Anteile, höchstens
bis zum geplanten Schiffbau. Nicht benötigte Anteile gehen an die übrigen Werften;
Systeme ohne Schiffbau bekommen keinen Anteil. Einzelne Reststücke gehen nach
Systemkennung. Beispiel: Bei Bedarf 10 und 4 sowie Vorrat 8 entstehen jeweils
4 Schiffe. Gespeicherte W5-Partien bleiben bei der proportionalen Verteilung
(6 und 2). Vorschau und Rundenauflösung verwenden dieselbe Berechnung.
Metall bleibt reichsweit verfügbar; unverbrauchter Vorrat bleibt erhalten.
In W7 sind die ersten Industriestufen günstiger: Die nächste Stufe kostet bei
Kapazität 2 nur 2 Punkte, bei Kapazität 5 sind es 3 und bei Kapazität 10 sind es
10 Punkte. Ab Kapazität 20 gilt wieder der bisherige Preis von zweimal der
Kapazität. Formel je Stufe: `min(2C, max(2, aufrunden(C²/10)))`.
Mehrere Stufen können in derselben Runde fertig werden; Restfortschritt bleibt
erhalten. Zusätzliche Kapazität steht ab der folgenden Runde bereit, höchstens 50.
Die Ausbauanzeige nennt den Preis und Fortschritt der jeweils nächsten Stufe.
Gespeicherte ältere Partien behalten ihre bisherigen Kosten. Systemeigenschaften
und Eroberung ändern den Preis nicht.
Terraforming, aktive Bevölkerungssteuerung und Besiedlung kommen später.

### Entscheidungen in der Oberfläche

Der Dialog **Neue Partie** zeigt nur die Einstellungen der gewählten Variante; die
Details mit Ereignissen und Rundenlimit gibt es nur bei klassischen Regeln.

In den Systemdetails steht unter der Kapazität ein einziger Regler **Ausbau**:
Zahlenfeld mit Plus/Minus und Schieberegler von 0 bis zur Kapazität. Der Rest der
Kapazität baut Schiffe. Darunter steht eine Ergebniszeile, etwa
„6 Schiffe/Runde · Ausbau 4/10 · fertig in 3 Runden“: Schiffe je Runde, der
Ausbaufortschritt nach der nächsten Runde und die Runden bis zur nächsten Stufe.
Ohne Ausbau fehlt die Rundenangabe; bei Kapazität 50 ist der Regler gesperrt.
Nach der Rundenabgabe bleibt nur die Ergebniszeile.

Es gibt keinen Übernehmen-Knopf: Zahlenfeld, Plus/Minus und Vorgaben speichern
sofort, der Schieberegler beim Loslassen. Während des Ziehens zeigt die Ansicht nur
eine Vorschau. „Gespeichert“ bestätigt die Änderung; wird sie abgelehnt, etwa nach
Rundenabgabe, erscheint „Änderung nicht gespeichert“ mit dem gespeicherten Plan.
Die Bedien-Fixierungen gelten zunächst in der geöffneten Karte. Bei eingeschalteter
Förderautomatik schützt eine Fixierung für Schiffbau oder Förderung das ganze
System zusätzlich dauerhaft vor automatischem Umverteilen. Diesen gespeicherten
Schutz siehst du im Automatikdialog; dort kann er auch nach einem Neuladen
aufgehoben werden. Eine noch sichtbare lokale Fixierung löst du am Produktionsfeld.
Die Vorschau zeigt Verlegung und die Metallbilanz für das gesamte Imperium.
Oben in der Spielleiste stehen Metallbarren, Vorrat und Änderung zur nächsten Runde,
etwa „24 (−3)“: Förderung plus Recycling minus Verbrauch. Die Aufschlüsselung
öffnet sich per Klick oder Tastatur.
Planeteneigenschaften liegen unter **Planet und Wirtschaft — Hintergrund**.

Ab 1.2.0-w5 kürzt weniger Schiffbau die ausgehenden Verlegungen des bearbeiteten
Systems, soweit sie Schiffbau plus Zulieferung übersteigen. Mehrere Routen werden
anteilig gekürzt, etwa 6/4 bei 6 verfügbaren Schiffen auf 4/2; eine Route bei 0
wird aufgehoben. Die Rückmeldung nennt jede Änderung, z. B. „Verlegung nach Vega:
6 → 4/Runde“. Späterer Mehrbau erhöht nichts automatisch, und reiner Metallmangel
ohne eigene Eingabe ändert keine Verlegung. Ältere Fassungen behalten ihre Routen.

In den Details eines eigenen Fördersystems listet **Eigene Systeme nach
Fördereignung** alle eigenen Fördersysteme mit Ertrag je Punkt und Restvorkommen,
die ergiebigsten zuerst; ein Klick wählt das System. Dort steht auch der Knopf
**Förderautomatik**.

Die Reichshinweise neben den Kennzahlen führen zu gefilterten Listen im Reiter
**Details** (**Filter aufheben** zeigt wieder das gewählte System):
Metallmangel, begrenzte Förderung, fehlende Arbeitskräfte und stockende Verlegungen.
Die Gruppen zählen jeweils Systeme; sie dürfen nicht zu einer Gesamtsumme addiert
werden. Hinweise verwenden gespeicherte Pläne und
bleiben während offener Schlachten verborgen.

**Weitere Aktionen → Schiffe verschrotten** öffnet die selten benötigte
Recyclingplanung. Ein vorgemerkter Befehl bleibt sichtbar und lässt sich aufheben.
Eine Rückgewinnung wird erst bei der Rundenauflösung gutgeschrieben.

Der Flottenversand nennt direkte Erreichbarkeit oder konkrete Zwischenstationen,
Reisedauer und Ankunftsrunde. Die Gesamtzeit enthält die Aufenthalte. Ohne verfügbaren
Weg ist Senden gesperrt. **Für Bündnispartner erobern** erscheint nur, wenn eine
Eroberungsentscheidung mit anderen zulässigen Partnern möglich ist; eigene oder
verbündete Verstärkung benötigt diese Wahl nicht. Die eigene Partei bleibt Vorgabe.
Klassisch nutzt denselben Versandaufbau ohne erweiterte Wirtschaft und Bündniswahl.

Impressum und Datenschutz stehen im Seitenfuß und öffnen in einem neuen Tab,
damit laufende Eingaben erhalten bleiben.

Flotten lassen sich auf freien Strecken auswählen. Systeme und ihre unmittelbare Umgebung bleiben auch unter kreuzenden Linien erreichbar; fehlt Platz für eine Flottenmarke, nutze die Flottenliste. Schlacht- und Engpasszeichen öffnen ihre eigene Information. In Schlachten stehen Angreifer links und Verteidiger rechts. Die Darstellung bleibt bis zum Ergebnis neutral; kurze Mannschaftstexte beschreiben die momentane Gefechtsverfassung. Exakte Stärken stehen unter **Kampfdetails**.


### Gemeinsame Schiffsforschung erproben

Schiffsforschung gehört zur aktuellen Regelfassung der erweiterten Regeln **1.3.0-r1**
und steht bei jeder neuen Partie mit erweiterten Regeln ohne besonderes Startprofil bereit.
Gespeicherte ältere Partien behalten ihre bisherigen Regeln.

Im Kartenmenü **Schiffsforschung** öffnen. Der Forschungsanteil gilt reichsweit
und startet bei 0 Prozent. Er wird zuerst aus der gesamten Industriekapazität
reserviert. Die Rundung erfolgt einmal für das Reich; die Verteilung auf Systeme
richtet sich nach deren Kapazität. Reicht die verbleibende Kapazität nicht,
werden Ausbau und danach Förderung gekürzt. Verlegungen werden an den Schiffbau
angepasst. Die Produktionsansicht nennt die reservierten Forschungspunkte.

Es gibt zunächst nur eine gemeinsame Schiffstechnologiestufe, ohne Gebiete oder
eigene Entwürfe. Die erste Stufe kostet 100 Punkte, danach folgen 400, 900 usw.
(Formel: 100 × Quadrat der nächsten Stufe). Restpunkte bleiben erhalten.
Jede Stufe erhöht die Kampfstärke von Neubauten um 10 Prozent des Grundwerts.
Ein Abschluss erscheint im Rundenbericht; die neue Technik gilt ab der folgenden
Runde für neu gebaute Schiffe. Bestehende Schiffe behalten ihre Baustufe.
Bei Stufe 20 ist vorerst Schluss; dann wird keine Industrie mehr für Forschung
reserviert.

Gemischte Garnisonen und Flotten behalten ihre Zusammensetzung. Versand nimmt
zuerst neue Schiffe, Recycling zuerst alte. Kampfverluste werden proportional
auf die vorhandenen Baustufen verteilt, ganzzahlige Reste deterministisch nach
größtem Rest und anschließend niedrigerer Stufe. Flottenansichten zeigen die
Baustufen und ihre gemeinsame Kampfstärke. Neue Technik ändert weder Tempo noch
Reichweite oder Metallkosten: Ein Schiff kostet weiterhin ein Metall, Recycling
bringt unverändert 70 Prozent zurück.

KI-Imperien verwenden 10 Prozent Forschung, Rush 5 Prozent. Der erste kontrollierte
Vergleich stützt kleine Budgets; hohe Dauerbudgets schwächen den frühen Flottenaufbau.
Die Ergebnisse und Grenzen stehen in `RESEARCH-EXPERIMENTS.md`. Die persönliche Spielabnahme ist erledigt (18.11.2026).
Bevölkerung, Besiedlung und getrennte Forschungsgebiete bleiben für später.


## Dauerhafte Förderautomatik

Mit **Förderautomatik** in den Details eines eigenen Fördersystems kannst du eine freiwillige, reichsweite
Planung einschalten. Voreingestellt ist **aus**. Wähle die teilnehmenden Systeme,
geschützte Systeme und den Zielvorrat in Verbrauchsrunden (0 bis 10, Vorgabe 1).
**Globale Vorschau** zeigt den geplanten Metallverbrauch, Zielvorrat, prognostizierten
Restvorrat, Unterdeckung und konkrete Änderungen. Erst **Einstellungen übernehmen**
aktiviert oder ändert die Automatik. Bei einer veralteten Runde oder inzwischen
geänderten Produktion musst du den Dialog neu öffnen.

Eine Verbrauchsrunde bezieht sich auf den vollen geplanten Schiffbau **nach** der
Umverteilung, auch wenn Metallmangel den tatsächlichen Bau begrenzen würde.
Ziel 0 deckt den laufenden Bedarf ohne zusätzlichen Reservenvorrat. Die Automatik
bevorzugt ergiebige freie Fördersysteme; sie ist keine Zusage einer optimalen
Strategie. Sie nutzt die normale Prognose einschließlich Forschung, Recycling,
Restvorkommen und gemeinsamer Metallverteilung.

Ausbau und Forschungsbudget bleiben stehen. Verlegungsquellen und geschützte
Systeme werden nicht umgestellt; bestehende Routen werden nicht gestrichen.
Ist das Ziel deshalb oder wegen fehlender Kapazität/Vorkommen nicht erreichbar,
zeigt die Vorschau die Unterdeckung. Die Runde bleibt abgebbar.

Die Einstellungen überleben Neuladen und Serverneustart. Nach jeder Runde wird
der Plan für die nächste Runde neu berechnet, ebenso nach eigenen Änderungen
an Forschung, Recycling oder Schutzvorgaben. In den Systemdetails steht
**automatisch**, **geschützt** oder **manuell**. Die zuletzt berechneten
Förderänderungen stehen mit Rundennummer im Dialog. Eine manuelle Änderung der
Produktion nimmt das betreffende System aus der Automatik; aktiviere es bei Bedarf
erneut. Neue Eroberungen werden nicht ungefragt aufgenommen. Bei Verlust oder
Sitzwechsel wird die Zuordnung entfernt. Abschalten beendet weitere Eingriffe
und lässt den zuletzt gültigen Produktionsplan stehen.

**Autarker Schiffbau** bleibt eine einmalige 50/50-Vorgabe und wird dadurch
nicht selbst zur dauerhaften Automatik. Bestehende Wirtschaftsregeln und
Regelversionen ändern sich nicht.
