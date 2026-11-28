# Starfare – Spielanleitung

Stand: Release 0.3.0, November 2026. Die Anleitung beschreibt die
Desktop-Oberfläche. Technische Einrichtung und Start der Anwendung stehen in
der [README](../README.md).

Gespielt wird unter **[games.zettsystems.de/starfare](https://games.zettsystems.de/starfare)**.
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
   ausgewertet. Der Rundenbericht legt sich danach vor das ganze Spielfeld und führt dich
   durch die Ereignisse der Runde; erst dann sind neue Befehle möglich (Abschnitt „Rundenbeginn: Ereignisse durchgehen“).

## Partie erstellen und beitreten

Starfare setzt Sector Forces um, ein C64-Spiel der 80er-Jahre und vermutlich ein deutscher
Klon von Galactic Empire (1983): Jedes System baut
Schiffe, Flotten erobern fremde Systeme, und wer genug der Galaxie beherrscht,
gewinnt. **Neues Spiel** in der Lobby öffnet den Dialog **Neue Partie**. Er passt auf
eine Seite; die Auswahl triffst du über Bildkarten und Klapplisten. Ein Klick auf
**Spiel anlegen** legt die Partie an. Der Dialog merkt sich deine Wahl je Konto:
Die nächste neue Partie beginnt mit Regeln, Mitspielern, Tempo, Karte, Zufall, Ereignissen
und Rundenlimit der letzten. Was nicht mehr passt, etwa eine Kartengröße, die es nicht mehr
gibt, fällt auf die Vorgabe zurück. Der Name wird jedes Mal neu gewürfelt.

**Partie**

- **Name**: Der Dialog schlägt einen zufälligen Namen vor; der Würfel daneben würfelt
  einen neuen.
- **Regeln**: **Klassisch** ist vorausgewählt; die folgenden Spielregeln beschreiben
  sie. **Erweitert** ergänzt Industrieausbau, begrenzte Reichweite mit
  Zwischenstationen, Bündnisse und gemeinsame Schlachten. Der Abschnitt „Erweiterte
  Regeln“ am Ende beschreibt, worin sie von den klassischen Regeln abweichen.
- **Name des Imperiums**: So erscheint dein Reich auf der Karte und im Bericht.

**Mitspieler**

- **Spieler** 2–10 (Vorgabe 3) und **Davon Computer** 0 bis Spieler minus 1 (Vorgabe:
  alle anderen). Änderst du die Spielerzahl, bleibt „alle anderen sind Computer“
  erhalten; eine gemischte Runde behält ihre Computerzahl.
- **Computerstärke** **Einfach**, **Normal** (Vorgabe) oder **Anspruchsvoll**, siehe unten.
- **Spieltempo** nur bei mehr als einem Menschen: die Rundenfristen (Live, Zügig,
  Täglich, Gemütlich).
- **Nur auf Einladung** nur bei mehr als einem Menschen: Sonst ist die Partie öffentlich
  und für andere angemeldete Spieler auffindbar. Einladen kannst du im Warteraum. Eine
  Partie allein gegen den Computer ist immer privat.
- **Tutorial**: Die Partie zeigt Hilfen zum Lernen, die es im normalen Spiel nicht
  gibt: Die Startkarte erklärt, woraus eine Runde besteht. Neben **Runde abgeben** steht
  der nächste Schritt (System wählen und **Flotte senden**, Ziel anklicken, weitere Flotten
  oder **Runde abgeben**); das Kreuz daran blendet die Hinweise für dein Konto aus. Eine
  gestrichelte Raute am System zeigt die Zahl freier Schiffe, wenn dort mehr Schiffe stehen,
  als das System in einer Runde baut. Alle Teilnehmer
  sehen in Warteraum und Kopfzeile, dass es ein Tutorial ist; es zählt nicht in der
  Statistik der Plattform.

Sind alle anderen Sitze Computer, startet die Partie sofort und die Karte öffnet sich.
Sonst wartest du im Warteraum, bis alle menschlichen Plätze besetzt sind, und startest
dann selbst.

Beim ersten Öffnen liegt die Startkarte wie der Rundenvorhang vor dem Spielfeld: Sie nennt
dein Reich, das System, in dem es beginnt (es bleibt auf der Karte hell), und wie viele
Gegner mitspielen, davon wie viele vom Computer. **Los geht’s** gibt die erste Runde frei;
die Karte erscheint je Konto und Partie nur einmal. Eine Zugreihenfolge gibt es nicht, alle
planen gleichzeitig.

**Karte**

- **Kartenform**: **Gleichmäßig** verteilt die Systeme ohne abgelegene Startplätze,
  **Zufällig** erzeugt Haufen und Lücken.
- **Galaxiegröße**: **Klein** 16, **Mittel** 24 (Vorgabe), **Groß** 30 oder **Riesig**
  40 Systeme. Die Systemdichte ist auf jeder Karte gleich, größere Karten sind also
  weiter. Größen mit weniger als zwei Systemen je Spieler sind gesperrt.
- **Höchste Produktion** 6, 10 (Vorgabe), 15 oder 20: Neutrale Systeme produzieren
  1 bis zu diesem Wert, meist um die Mitte.

Flotten fliegen auf jeder Karte gleich schnell: 160 Bildpunkte der Karte je Runde,
mindestens 2 und höchstens 40 Runden. Benachbarte Systeme liegen meist etwa drei
Runden auseinander; quer über die ganze Karte sind es etwa 19 (Klein), 23 (Mittel),
25 (Groß) oder 29 Runden (Riesig).

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
Geschwindigkeitswahl hat Vorrang). In den erweiterten Regeln sind Bündnisse, auch
mit Computern, und Bündnissiege (gemeinsam 70 %) immer erlaubt.

Auf der gemeinsamen Spieleplattform hat jede Variante eine Stufe (Alpha, Beta,
Produktion); die erweiterten Regeln sind derzeit in Alpha. Wer eine Variante nutzen
darf, entscheidet die Plattform; die Regelkarte kennzeichnet Alpha und Beta. Wer eine
Variante nicht mehr nutzen darf, erreicht auch ihre Partien nicht mehr, auch laufende
und die Nachbetrachtung. Im eigenen Start von Starfare stehen beide Varianten allen
offen. Die Variante bleibt für die Partie fest; Vorlagen und Revanchen übernehmen sie.

Die Lobby zeigt offene und laufende Partien. Suche nach Partie- oder Spielernamen
und nutze den Filter für eigene, offene oder laufende Partien. Beendete Partien
listet die Plattform; im eigenen Start findest du sie im **Archiv** über **Mein Bereich**.

### Computerstärke

| Stärke | Spielweise (klassisch) | Start der Computer (klassisch) |
| --- | --- | --- |
| Einfach | Basisstrategie: stärkste Basis, nahes Ziel, begrenzter Versand | halbe Produktion und Schiffe (2 und 4) |
| Normal | Ruhige Planung: Heimatwache, höchstens eine Flotte je Runde, nur mit klarer Überlegenheit, dann mit allen freien Schiffen | wie Menschen (4 und 8) |
| Anspruchsvoll | im Zweikampf verdeckt Basisstrategie oder früher Angriff, mit mehreren Reichen die Basisstrategie | anderthalbfache Produktion und Schiffe (6 und 12) |

Die Werte sind mit dem Strategieprüfstand abgestimmt: Wer wie die Basisstrategie spielt,
gewinnt im Zweikampf gegen **Einfach** etwa drei bis vier von fünf Partien, gegen
**Normal** jede zweite oder etwas mehr und gegen **Anspruchsvoll** etwa jede fünfte; mit
mehreren Reichen liegt **Normal** beim fairen Anteil oder knapp darüber, **Einfach** darüber und
**Anspruchsvoll** darunter. Normale Computer greifen etwa halb so oft an wie die
Basisstrategie, dafür mit größeren Flotten. Welche Heimat welcher Sitz bekommt, wird
ausgelost. In den erweiterten Regeln spielen einfache und normale Computer die
Basisstrategie, anspruchsvolle den adaptiven Industrieausbau; einfache starten mit halber
Produktion und halben Schiffen, normale und anspruchsvolle wie Menschen.

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
als Vollbild mit denselben vier Reitern, dazu Bündnisse und die Befehlsrücknahme.

- Wische zum Verschieben; zoome mit zwei Fingern oder den runden Knöpfen am
  rechten Rand. Diese passen die Ansicht auch an das eigene Reich oder die ganze
  Galaxie an; das Fragezeichen öffnet die **Kartenhilfe** mit der Legende.
- Ein Tipp auf ein System öffnet seine Details in der Übersicht.
- **Flotte senden** oder **Verlegung anlegen** führen zurück zur Karte. Ein Hinweis
  bleibt sichtbar, bis du das Ziel antippst oder **Zielwahl abbrechen** wählst.
- Dialoge füllen den Bildschirm; ihre Knöpfe bleiben unten erreichbar.

Im Spiel und im Kartenkopf erscheinen Imperiumsnamen. Spielernamen in Klammern
stehen nur in Übersichten und in der Diplomatie.

Die Seitenleiste hat vier Reiter: **Details, Flotten, Verlegungen und Bericht**, auch
bei schmaler Seitenleiste und auf dem Handy. Darüber stehen **Bündnisse** (nur
erweiterte Regeln) und **Letzten Befehlschritt zurücknehmen** als Symbolknöpfe; der
Hilfetext nennt den vollständigen Namen. Einzelversand und Verlegungen behalten den
aktuellen Reiter; ein gemeinsamer Angriff und das Bearbeiten eines Befehls öffnen den
Reiter **Flotten**, eine ausgewertete Runde den Bericht. Aktualisierungen
unveränderter Inhalte erhalten Eingaben und Fokus.

In den Systemdetails stehen versendbare Schiffe, Garnison und die erwartete
Schiffproduktion pro Runde oben. **Flotte senden** und **Verlegung anlegen** stehen
direkt darunter, danach die ein- und ausgehenden Verlegungen des Systems; in
erweiterten Partien folgt die Industrie (Abschnitt „Industrie“). **Logistik** öffnet
die Verlegungsbilanz (eingehend, Produktion, ausgehend) und die Ankünfte. Für fremde
Systeme außerhalb der eigenen Sicht zeigen die Details die **Letzte Aufklärung**:
Besitzer, gezählte oder geschätzte Schiffe, Produktion und die Runde des letzten
Kontakts. **Hintergrund** enthält die Besitzhistorie, soweit du das System siehst.
Die geöffneten Bereiche bleiben bei Aktualisierungen und Systemwechseln geöffnet; die
Angaben folgen der Auswahl.

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
letzten Änderung wieder her. Das gilt auch für einen ganzen gemeinsamen Angriff. Es gibt einen
Rücknahmeschritt, keine mehrstufige Historie. Er bleibt nach Neuladen und Neustart
erhalten, endet aber mit dem Rundenwechsel. Verlegungen gehören nicht zu dieser
Befehlsrücknahme.

### Gemeinsam angreifen

Klickst du auf der Karte ein fremdes System oder eine eigene fliegende Flotte an,
zeigt jedes eigene System mit freien Schiffen links ein Abzeichen mit der Runde,
in der eine jetzt gesendete Flotte dort ankäme, und seinen freien Schiffen (`R14 · 8`). Gerechnet wird über
dieselben Routen wie beim Senden, in erweiterten Partien also mit Reichweite und
Zwischenhalten; Systeme ohne Weg dorthin bekommen kein Abzeichen.

Gibt es schon eine Ankunft, nach der du dich richten kannst, vergleichen die
Abzeichen zunächst damit: bei einer gewählten Flotte ihre Ankunft, bei einem
gewählten System die früheste eigene Flotte oder geplante Sendung dorthin.

- **Grün mit ✓:** kommt in derselben Runde an.
- **Gelb mit `+1 ⏸`:** käme so viele Runden zu früh.
- **Grau:** käme später an.

So planst du einen gemeinsamen Angriff auf ein angeklicktes System: Über seinen Details
steht der **Angriffsplan** (auf dem Handy als Blatt über der unteren Kartenhälfte, die
Abzeichen bleiben darüber sichtbar). Er zeigt die bekannte Verteidigung des Ziels als
Zahl („Vega: 20 Schiffe · Produktion 4“), die gewählten Quellen und darunter die
möglichen Quellen, sortiert nach Ankunft („Altair · R40 · 8 Schiffe“).

1. Wähle die Systeme, die mitfliegen sollen: Abzeichen auf der Karte anklicken oder im
   Plan abhaken, beides wählt dieselbe Quelle. Gewählte Abzeichen tragen ein ✔ und
   einen hellen Rand, das System einen grünen Ring; ein zweiter Klick oder der Haken
   wählt ab. Die Abzeichen vergleichen jetzt mit der gemeinsamen Ankunft, der
   spätesten Ankunft der gewählten Systeme.
2. Jede gewählte Quelle hat ein Zahlenfeld mit −/+ und **alle** (vorgegeben: alle
   freien Schiffe), darunter „Start sofort“ oder „Start in Runde 37 (+2)“ und die Ankunft.
   Darunter steht die Summe: „Gemeinsame Ankunft R39 · 45 Schiffe“.
3. **Angriff planen – 45 Schiffe, Ankunft R39** (bei einem eigenen Ziel **Senden
   planen**) übernimmt alles oder nichts; ohne Quelle oder mit ungültiger Menge ist der
   Knopf gesperrt. **Abbrechen** oder ✕ schließt den Plan ohne Befehle.

Bei einer gewählten eigenen Flotte gibt es keinen Plan in der Seitenleiste: Über ihrem
Ziel steht dann **Gemeinsam angreifen (Ankunft R39)** und öffnet eine kurze
Bestätigung mit denselben Angaben.

Das langsamste System startet sofort. Schnellere Systeme starten später, genau so,
dass alle in derselben Runde ankommen: Kommt ein System in Runde 37 an, ein anderes
in 39, startet das erste zwei Runden später. Bis zum Start bleiben diese Schiffe in
der Garnison und verteidigen das System; für andere Sendungen sind sie aber
vorgemerkt und nicht mehr verfügbar. Gestartet wird automatisch in der richtigen
Runde. Geplante Starts stehen im Reiter **Flotten** mit Start- und Ankunftsrunde und
lassen sich stornieren, solange die Runde offen ist; **Letzten Befehlschritt
zurücknehmen** nimmt auch einen ganzen gemeinsamen Angriff zurück.

Ändert sich bis zum Start etwas, steht es im Rundenbericht:

- Geht das Startsystem verloren, entfällt der Start.
- Sind weniger Schiffe da als geplant, startet der Rest; ist keines mehr da,
  entfällt der Start.
- Ist das Ziel nicht mehr erreichbar (erweiterte Regeln: Route unterbrochen,
  oder das Ziel ist zerstört), entfällt der Start.
- Gehört das Ziel inzwischen dir oder einem Verbündeten, fliegen die Schiffe
  trotzdem los und verstärken es, wie jede Flotte, die schon unterwegs ist.

### Unterwegs und Ankunftsvorschau

Der Reiter **Flotten** zeigt reisende Flotten, die in dieser Runde vorgemerkten
Befehle und die geplanten Starts gemeinsamer Angriffe in einer Liste, gruppiert nach Ankunftsrunde; innerhalb einer Runde stehen
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
Reisezeit. Wenn nötig, nutzt die Verlegung vorhandene Garnison, aber nie Schiffe, die ein späterer
Start eines gemeinsamen Angriffs vorgemerkt hat. Nicht versandte
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

## Rundenbeginn: Ereignisse durchgehen

Nach dem Rundenwechsel legt sich der Rundenbericht wie ein Vorhang vor das ganze Spielfeld:
Karte, Seitenleiste und Kopf sind abgedunkelt und nehmen keine Klicks an, nur das System des
Ereignisses bleibt hell. Davor steht die Karte des ersten noch offenen Ereignisses mit Ort
(Schlachten, Eroberungen, Verluste, kosmische Meldungen wie Supernova, Beben oder Piraten und
entfallene oder verkleinerte Starts eines gemeinsamen Angriffs) mit der Runde („Runde 12
ausgewertet“) und der Stelle („Ereignis 2 von 5“); die Karte zentriert sich auf das System.
Kosmische Meldungen zeigen ihre Folge auf der Karte: Ein mysteriöses Verschwinden steht als „−3“
an der betroffenen Flotte (die Karte zentriert auf ihre Position unterwegs; ist die Flotte ganz
verloren, am Ziel), ein Raumhafenbeben als „−3“ am System. Sturm und Raum-Zeit-Erschütterung
markieren alle eigenen Flotten im Hyperraum mit „+1“ bzw. der neuen Ankunftsrunde („R12“) und
zentrieren auf die, die zuerst ankommt. Flüchtlinge und weitergesprungene Flotten sind
hervorgehoben, fliehende Piraten stehen mit ihrer Zahl an der explodierten Sonne.
Eine Schlacht bietet
**Schlacht ansehen** mit der Wiedergabe, jedes andere Ereignis **Weiter**; danach folgt das
nächste. Produktion, eintreffende Verstärkungen und sonstige Meldungen ohne Ort gehören nicht dazu.
Erst nach dem letzten Ereignis hebt sich der Vorhang; vorher gibt es keine Befehle (das Tutorial
sagt das auf der Ereigniskarte dazu): kein
Senden, keine Zielwahl, keine Verlegung und kein **Runde abgeben**. Der Fortschritt bleibt
beim Neuladen und auf anderen Geräten erhalten. Zuschauer und die Nachbetrachtung durchlaufen
nichts. Auf dem Handy steht die Ereigniskarte am unteren Rand des Vorhangs.

## Schlachten ansehen oder sofort auswerten

Eine offene Kampfmeldung zeigt zunächst den Ort. Besitzer und Ergebnis am
betroffenen System bleiben verdeckt, bis du die Schlacht bestätigt hast.
Klicke die Meldung an, um die Wiedergabe zu öffnen, und bestätige mit
**Ergebnis übernehmen**. Die Wiedergabe nennt oben hervorgehoben die Produktion des Systems,
soweit du sie kennst; hast du das System verloren, die Produktion, die es bei dir hatte. Am Ende
steht über dem Gewinner groß **Sieger**, ein Lorbeerkranz umrahmt seine verbliebenen Schiffe. Bleibt
auf keiner Seite ein Schiff, zeigt die Markierung, wer das System hält. Einen eigenen
Eroberungsdialog gibt es nicht. Die
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

In der Wiedergabe stehen Angreifer links und Verteidiger rechts; die Darstellung bleibt
bis zum Ergebnis neutral. Kurze Mannschaftstexte beschreiben die momentane
Gefechtsverfassung, die genauen Stärken stehen unter **Kampfdetails**.

## Sieg, Fortsetzung und Archiv

Gewonnen hat, wer 70 % aller Systeme kontrolliert; neutrale Systeme zählen zur
Gesamtzahl. Wenn die letzte Runde noch
offene Ereignisse enthält, führt zunächst der Bericht durch sie. Erst danach
öffnet sich das Ergebnis mit persönlichen Schiffssummen; Befehle gibt es nach
der Entscheidung keine mehr. In erweiterten Partien steht dort zusätzlich
**Industrie gebaut** mit den Kapazitätspunkten, die dein Reich in der Partie ausgebaut hat.

Der Sieger darf **Bis zur vollständigen Eroberung weiterspielen** wählen. Das Ziel
steigt dann auf 100 %, die Abgaben werden zurückgesetzt und die Fristen starten neu.
Die anderen Teilnehmer werden automatisch informiert. Das zuerst erreichte
Ergebnis bleibt in der Statistik der Plattform bestehen. Endet eine Partie nach klassischen Regeln
durch ihr Rundenlimit, gibt es keine Fortsetzung.

**Aufgeben** steht im Kartenmenü neben **Verlassen**. Nach einer Rückfrage übernimmt
der Computer dein Reich bis zum Ende der Partie. Anders als beim Verlassen kannst du
nicht zurückkehren, auch wenn die Partie Wiedereinstieg erlaubt.

## Galaxie-Ereignisse und Wertung (Klassisch)

Nach dem C64-Klassiker Galactic Empire (1983). Wie oft Ereignisse eintreten, folgt der
**Zufallsstufe** der Partie (Kein: nie, Wenig: selten, Mittel: normal, Viel: häufig). Unter
**Details** lässt sich jedes Ereignis einzeln abwählen. Meldungen erscheinen im Bericht als
Ticker-Karten:

- **Kosmische Stürme:** Alle Flotten im Hyperraum, auch Piraten, kommen eine Runde später an.
  Davon erfahren nur Reiche mit Flotten im Hyperraum.
- **Raum-Zeit-Erschütterung:** Alle Flotten im Hyperraum brauchen nur noch die halbe Restzeit.
  Auch hier erfahren es nur Reiche mit Flotten im Hyperraum.
- **Mysteriöses Verschwinden:** Eine zufällige Flotte verliert bis zur Hälfte ihrer Schiffe.
  Nur ihr Besitzer erfährt davon.
- **Raumhafenbeben:** Ein zufälliges System verliert einen Teil seiner Garnison (leichtes oder
  schweres Beben). Nur der Besitzer erfährt davon.
- **Supernova:** Der Bericht meldet allen „Sonne instabil“, die Karte zeigt das System mit
  pulsierendem Rand. Am Ende dieser Runde explodiert die Sonne; bis dahin kannst du stationierte
  Schiffe abziehen. Danach ist das System für immer zerstört (dunkel auf der Karte), zählt nicht mehr zur Siegschwelle und kann nicht
  mehr angeflogen werden. Seine Garnison flieht als Flotte zu einem zufälligen System;
  Flotten auf dem Weg dorthin springen per Panikschaltung zu einem anderen System weiter.
  Heimatwelten explodieren nie. Bei Zufall „Wenig“ gibt es höchstens eine Supernova je Partie.
- **Piraten** (nur zusammen mit der Supernova): Die Garnison einer neutralen Sonne flieht
  als Piratenflotte ohne Sitz. Gewinnt sie den Kampf am Ziel, wird das System neutral.

**Spielende nach Runden** (10 bis 500): Erreicht bis dahin niemand die Siegschwelle, endet
die Partie nach der letzten Runde. Es gewinnt die höchste Wertung: 400 Punkte je System,
25 je stationiertes Schiff, 20 je Schiff unterwegs; bei Gleichstand mehr Systeme, dann der
niedrigere Sitz. In den letzten fünf Runden kündigt der Bericht das Ende an; die Wertung
aller Reiche steht im letzten Bericht.

## Archiv und Nachbetrachtung

Beendete Partien listet die Plattform mit einem Verweis auf die Nachbetrachtung. Im eigenen
Start öffnet im **Archiv** **Ansehen** die Nachbetrachtung; Such- und Ergebnisfilter bleiben
bei Rückkehr innerhalb derselben Sitzung erhalten, die Liste führt zur zuletzt angesehenen
Partie.

In der Nachbetrachtung kannst du die beendete Karte lesend öffnen, eine Teilnehmerperspektive
wählen und den Kriegsnebel umschalten. Die Zeitleiste zeigt gespeicherte Runden ohne
Kriegsnebel; bei **Endstand** gelten wieder Perspektive und Nebelschalter. Ältere
Partien ohne gespeicherte Runden bieten nur den Endstand. Private Archive bleiben
auf Berechtigte beschränkt; öffentliche erlauben fremde Zuschauer nur bei aktivierter
Zuschaueroption. Deine Ergebnisse zeigt die **Statistik** der Plattform über alle
Spiele; Starfare selbst führt keine eigene Statistik.

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

Geschwindigkeit, Ton, Berichtsfilter, Routensuche, Seitenleistenbreite, ausgeblendete
Tutorial-Hinweise und die Wahl der letzten neuen Partie werden je Konto gespeichert und
bei einer späteren Anmeldung wieder geladen.
Sprache bleibt sitzungsbezogen; Kartenposition und Zoom werden je Partie in der
Browsersitzung behalten.

Konto, Kontolöschung, Impressum und Datenschutz gehören der Spieleplattform und stehen
in ihrem Rahmen. Im eigenen Start bündelt **Mein Bereich** Lobby und Archiv.

## Erweiterte Regeln

Erweitert ist Klassisch plus vier Dinge: ausbaubare Industrie, begrenzte Flugreichweite
mit Zwischenstationen, Bündnisse und gemeinsame Schlachten. Ereignisse, Startausgleich nach
Lage und Rundenlimit gibt es nur in Klassisch. Partien der früheren erweiterten Fassungen
1.x lassen sich nicht mehr öffnen.

### Industrie

Jedes System hat eine Kapazität, anfangs so groß wie seine Produktion, höchstens 50. In den
Systemdetails eines eigenen Systems steht unter **Industrie** die Kapazität (etwa
„Kapazität 10 / 50“) und ein einziger Regler **Ausbau**: Zahlenfeld mit Plus/Minus und
Schieberegler von 0 bis zur Kapazität. Der Rest der Kapazität baut Schiffe. Darunter steht
eine Ergebniszeile, etwa „6 Schiffe/Runde · Ausbau 4/10 · fertig in 3 Runden“: Schiffe je
Runde, der Ausbaufortschritt nach der nächsten Runde und die Runden bis zur nächsten Stufe.
Ohne Ausbau fehlt die Rundenangabe; bei Kapazität 50 steht dort „Maximale Kapazität
erreicht“. Nach der Rundenabgabe bleibt nur die Ergebniszeile.

Es gibt keinen Übernehmen-Knopf: Zahlenfeld und Plus/Minus speichern sofort, der
Schieberegler beim Loslassen. Während des Ziehens zeigt die Ansicht nur eine Vorschau.
„Gespeichert“ bestätigt die Änderung; wird sie abgelehnt, etwa nach Rundenabgabe, erscheint
„Änderung nicht gespeichert“ mit dem gespeicherten Plan.

Die Ausbaupunkte sammeln sich. Die nächste Stufe kostet bei Kapazität C
`min(2C, max(2, aufrunden(C²/10)))` Punkte: bei Kapazität 2 nur 2, bei 5 sind es 3, bei 10
sind es 10, ab 20 das Doppelte der Kapazität. Mehrere Stufen können in derselben Runde
fertig werden; Restfortschritt bleibt erhalten. Neue Kapazität wirkt ab der folgenden
Runde. Der Fortschritt bleibt bei Umschaltung und Eroberung erhalten; ein Eroberer beginnt
mit vollem Schiffbau. Neutrale Systeme bauen nicht aus.

Senkt mehr Ausbau den Schiffbau eines Systems so weit, dass seine ausgehenden Verlegungen
nicht mehr aus Schiffbau und Zulieferung gedeckt sind, werden sie anteilig gekürzt, etwa
6/4 bei 6 verfügbaren Schiffen auf 4/2; eine Route bei 0 wird aufgehoben. Die Rückmeldung
nennt jede Änderung, z. B. „Verlegung nach Vega: 6 → 4/Runde“. Späterer Mehrbau erhöht
nichts automatisch.

### Reichweite und Zwischenstationen

Jede erweiterte Partie hat eine feste Reichweite je Reiseabschnitt. Sie wird aus der
Galaxie berechnet, damit alle Systeme schrittweise erreichbar bleiben. Reisezeit und
Sensorreichweite sind davon unabhängig.

Während der Zielwahl zeigt ein gestrichelter Kreis um die Quelle die direkte Reichweite.
Ziele tragen einen Zustand: durchgezogener Rahmen für direkt erreichbar, gestrichelter
Rahmen für „nur über Stationen“, abgeblendet für „kein Weg“. Hinweistext und
Versanddialog nennen ihn als **Direkt erreichbar**, **Über 2 Stationen** oder **Kein Weg**.

Für ein entferntes Ziel sucht der Versand automatisch den schnellsten Weg über eigene
oder verbündete Systeme. Der Dialog zeigt einen Routenstreifen und einmal die
Ankunftsrunde einschließlich der Aufenthalte; lange Routen klappen mit **+… Stationen**
auf. Ein Klick auf einen bekannten Systempunkt zentriert die Karte, ohne Menge oder
Versandart zu ändern. Ohne erreichbaren Weg ist der Versand gesperrt. Unverbündete Systeme
können angegriffen werden, dienen aber erst nach ihrer Eroberung als Zwischenstationen.

An jeder Zwischenstation bleibt die Flotte eine volle Runde zum Auftanken. Sie bleibt ein
eigener Verband und wird nicht zur Garnison. Die Flottendetails zeigen den Reiseplan; mit
**Warten** lässt sich die Weiterreise verschieben, **Auflösen** fügt den Verband der
aktuellen eigenen Station hinzu. Die Regeln für Aufenthalte stehen in der
**Kartenhilfe**. Eine blockierte Reise verspricht keine Ankunft; **Stationsdetails öffnen**
zeigt den Standort, bei bekanntem fremdem Besitzer auch **Bündnisse ansehen**.

Geht eine Station während des Fluges verloren, kämpft die Flotte dort nach den normalen
Regeln; ihre Weiterreise endet. Geht sie während des Aufenthalts verloren oder ist die
nächste Station schon vor dem Abflug verloren, bleibt der Verband stehen, und seine
Ankunftsrunde ist offen, bis der Abschnitt wieder zulässig ist. Nach Rückeroberung wird
eine neue volle Auftankrunde abgewartet. Produktionsverlegungen verwenden dieselben Wege
und Reisezeiten; ohne nutzbaren Weg pausieren neue Lieferungen, der Auftrag bleibt
erhalten.

### Bündnisse und Bündnissieg

**Bündnisse** (Handschlag über den Reitern) zeigt zuerst offene Entscheidungen, danach dein
Bündnis und mögliche Partner. Zustimmen und Ablehnen stehen direkt am Vorschlag.
**Bündnis vorschlagen** am Partner sendet den Gründungsvorschlag sofort, **Beitritt
beantragen** fragt bei einem bestehenden Bündnis an. **Bündnis kündigen** verlangt eine
Bestätigung mit der genauen Austrittsrunde und den Folgen für stationierte Flotten. Die
allgemeinen Regeln stehen im aufklappbaren Bereich **Bündnisregeln**.

Ein Imperium gehört höchstens einem Bündnis an; alle Mitglieder sind miteinander verbündet.
Eine Gründung braucht beide Partner, eine Aufnahme die Zustimmung aller Mitglieder.
Ablehnen oder Zurückziehen verwirft den offenen Vorschlag. Die Kündigungsfrist beträgt fest
3 Runden: Der Austritt wird zu Beginn der Runde „aktuelle Runde plus 3“ wirksam, bis dahin
gilt das Bündnis vollständig. Bleibt nur ein Mitglied, endet das Bündnis. Nach der
Rundenabgabe sind Verträge nur noch lesbar.

Verbündete Systeme dienen als Zwischenstationen. Ankommende Unterstützung bleibt eine
eigene Flotte und übernimmt weder Garnison noch System des Partners. Verbündete kämpfen
gemeinsam (Abschnitt „Kampf“). Ein Vertrag verrät keine zusätzlichen militärischen Werte.

Nach wirksamem Austritt treten fremd stationierte Schiffe automatisch die kürzeste erlaubte
Heimreise zu einem eigenen System an, auch bereits anfliegende Unterstützung; die Kündigung
macht daraus keinen Angriff. Ohne erreichbares eigenes Ziel bleibt die Heimreise gesperrt.
Heimkehrende Flotten greifen nicht an, unterstützen keine Verteidigung und können weder
warten noch aufgelöst werden.

Computer stimmen Anfragen zu, solange sie nicht selbst einen Austritt angekündigt haben;
sie gründen keine Bündnisse von sich aus. Übernimmt der Computer einen menschlichen Sitz,
bleiben dessen Verträge samt Kündigungen gültig.

Zuerst wird der Einzelsieg geprüft (70 % aller Systeme). Danach gewinnt ein Bündnis mit
mindestens zwei Mitgliedern, wenn es zusammen 70 % hält; neutrale Systeme zählen zur
Gesamtzahl. Erfüllen mehrere Bündnisse die Bedingung, gewinnt das mit den meisten Systemen.
Alle Mitglieder gewinnen, auch solche ohne Systeme. Bei 16 Systemen reichen zwölf: Partner
mit 11 und 5 Systemen gewinnen gemeinsam. Jeder Sieger darf einmal bis zur vollständigen
Eroberung weiterspielen; dann steigen beide Schwellen auf 100 %, das erste Ergebnis bleibt.

### Kampf

Erweitert wertet Kämpfe für ganze Bündnisse gemeinsam aus. An einem System kämpfen
Besitzer, eintreffende Verbündete und dort stationierte Unterstützung als gemeinsame Seite.
Andere Bündnisse, vertragslose Imperien und eine neutrale Garnison bilden jeweils eigene
Seiten. Alle Seiten teilen ihre Feuerkraft anteilig auf die feindlichen Stärken auf;
Verluste eines Schlagabtauschs werden gleichzeitig berechnet. Überleben mehrere feindliche
Seiten, kämpfen sie weiter, bis höchstens eine übrig bleibt. Die Zufallsstufe gilt einmal
je Seite und Schlacht. Verluste werden auf ganze Schiffe aufgerundet; die Überlebenden
einer Seite verteilen sich anteilig auf die beteiligten Imperien, dann auf Garnison und
Flotten.

Ohne Zufall behalten 100 Schiffe gegen 60 Gegner 40 Schiffe. Zwei verbündete Flotten mit
je 50 Schiffen behalten gegen 60 Gegner je 20. Gleich starke Seiten können sich vollständig
vernichten; dann bleibt der bisherige Besitzer, auch mit leerer Garnison. Erfolgreiche
gemeinsame Verteidigung erhält den bisherigen Besitzer.

Im Versanddialog erscheint **Für Bündnispartner erobern**, wenn ein Partner als
Eroberungsziel in Frage kommt; Vorgabe ist das eigene Imperium. Der Befehl zeigt die Wahl,
auch nach Neuladen, Bearbeiten und Rücknahme. Gemeinsame Angriffe und Verlegungen erobern
für das eigene Imperium. Wollen Mitglieder derselben siegreichen Seite für verschiedene
Imperien erobern, entscheidet das Imperium mit den meisten überlebenden Schiffen. Besteht
das Bündnis beim Kampf nicht mehr, erobert die Flotte für ihr eigenes Imperium. Schiffe
werden nie verschenkt: verbündete Überlebende bleiben eigene Verbände.

Die Wiedergabe zeigt alle Seiten, die ausgewürfelten Stärken und die Verluste jedes
Imperiums. Bis zur Bestätigung bleiben Besitzer, Endwerte und überlebende Unterstützung in
Karte, Flottenliste und Systemdetails verborgen, auch nach Neuladen. Der Ergebnisdialog
zählt verlorene Schiffe genau und teilt Abschüsse nach dem verursachten Feuer auf, sodass
jedes zerstörte Schiff nur einmal zählt.
