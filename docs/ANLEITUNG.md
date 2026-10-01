# Starfare – Spielanleitung

Stand: 1.0.0-SNAPSHOT, 1. Oktober 2026. Die Anleitung beschreibt die
Desktop-Oberfläche. Technische Einrichtung und Start der Anwendung stehen in
der [README](../README.md).

## Schnellstart

1. Registriere dich mit E-Mail-Adresse, öffentlichem Spielernamen und Passwort.
   Bestätige die E-Mail-Adresse und melde dich an.
2. Erstelle in der Lobby ein **Neues Spiel** oder tritt einer verfügbaren Partie bei.
3. Der Gastgeber startet die Partie, sobald alle menschlichen Plätze besetzt sind.
4. Wähle auf der Karte ein eigenes System und danach ein anderes System als Ziel.
   Lege die Schiffsanzahl fest und merke den Sendebefehl vor.
5. Prüfe den Reiter **Befehle** und klicke auf **Runde abgeben**.
6. Sobald alle Menschen abgegeben haben oder die Frist abläuft, wird die Runde
   ausgewertet. Sieh dir anschließend den **Bericht** an und bestätige offene Schlachten.

## Partie erstellen und beitreten

Im Assistenten wählst du unter anderem:

- 8–120 Systeme; mindestens so viele wie Teilnehmer.
- Bis zu acht menschliche und sieben KI-Spieler, insgesamt höchstens zehn.
- Sitzfarben, Galaxieform, Produktionsverteilung und Startwerte.
- Produktion von 1–20 Schiffen je Runde und Startgarnison von 1–50 Schiffen.
- Zuschauer und Wiedereinstieg, Rundenfristen sowie die Angriffsreihenfolge.
- Den zum Sieg nötigen Systemanteil: 10–100 %, Vorgabe 70 %.
- Zufall bei der Kampfstärke: 0–30 %, Vorgabe ±10 % je Seite.
- Ob Schlachten zunächst mit Wiedergabe dargestellt werden. Eine gespeicherte
  persönliche Geschwindigkeitswahl hat Vorrang vor dieser Vorgabe.

Neue Partien sind **privat**. Der Gastgeber kann vor dem Start Spieler einladen
oder die Partie veröffentlichen. Eine Einladung reserviert einen Platz; der
eingeladene Spieler muss selbst beitreten. Nach dem Start ist die Sichtbarkeit fest.
Öffentliche Partien sind für andere angemeldete Spieler auffindbar.

Die Lobby zeigt offene und laufende Partien. Suche nach Partie- oder Spielernamen
und nutze den Filter für eigene, offene oder laufende Partien. Beendete Partien
findest du im **Archiv** über **Mein Bereich**.

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
- Nutze die sichtbaren Kartenknöpfe für Zoom, Zurücksetzen, die ganze Galaxie
  oder das eigene Reich. Die **Legende** erklärt Farben und Verbindungslinien.
- Klicke auf ein System oder eine Flotte, um **Details** zu öffnen.
- Fokussierbare Kartenpunkte und Aktionskacheln lassen sich mit **Enter** oder
  **Leertaste** auslösen. Mit **Tab** wechselst du den Fokus.
- Verschiebe die Trennlinie zwischen Karte und Seitenleiste, um die Breite anzupassen.
- Ein Berichtsmarker führt zur zugehörigen Meldung. Eine Meldung zentriert die Karte
  auf das betroffene System.

Die Seitenleiste enthält **Kontakte, Details, Flotten, Befehle, Verlegungen,
Logistik und Bericht**. Ein neuer Befehl öffnet den Befehlsreiter, eine ausgewertete
Runde den Bericht. Aktualisierungen unveränderter Inhalte erhalten Eingaben und
Fokus; neue Chat-Nachrichten löschen keinen begonnenen Nachrichtenentwurf.

## Flotten und Befehle

### Einzelne Flotte senden

Wähle zuerst ein eigenes System und dann das Ziel. Stelle die Menge per Regler,
Eingabefeld oder Schnellknopf ein. **Alle** nutzt die verfügbaren Schiffe nach
Abzug von Reserve und bereits vorgemerkten Sendungen. **Alle außer Produktion**
behält zusätzlich die aktuelle Produktionsmenge zurück.

Der Auftrag erscheint unter **Befehle**. Solange du die Runde noch nicht abgegeben
hast, kannst du ihn **Stornieren** oder mit **Befehl bearbeiten** Menge und Ziel
ändern. Ein Sendebefehl darf weder die Reserve unterschreiten noch mehr Schiffe
vormerken, als am Ausgangssystem verfügbar sind.

**Letzten Befehlschritt zurücknehmen** stellt die eigene Befehlsliste vor der
letzten Änderung wieder her. Das gilt auch für einen Sammelversand. Es gibt einen
Rücknahmeschritt, keine mehrstufige Historie. Er bleibt nach Neuladen und Neustart
erhalten, endet aber mit dem Rundenwechsel. Eine inzwischen erhöhte Reserve wird
dabei weiterhin eingehalten. Reserveänderungen und Verlegungen selbst gehören
nicht zu dieser Befehlsrücknahme.

### Flotten gesammelt senden

1. Öffne **Flotten gesammelt senden** und wähle mehrere eigene Systeme.
2. Wähle ein gemeinsames Ziel, das keines der ausgewählten Ausgangssysteme ist.
3. Nutze **Alle verfügbaren Schiffe** oder gib eine feste Menge **je System** an.
4. Prüfe die Vorschau mit den Summen je Ankunftsrunde und bestätige den Versand.

Die Aktion wird als Ganzes geprüft. Ist eine Menge ungültig oder inzwischen die
Runde gewechselt, wird keine Teilmenge der Befehle übernommen.

### Unterwegs und Ankunftsvorschau

Der Reiter **Flotten** zeigt eigene Flotten als Karten. **Gemeinsame Ankünfte**
summiert reisende Flotten und vorgemerkte Sendungen nach Ziel und Ankunftsrunde.
Die Routensuche schränkt die angezeigten Karten ein.

**Warten** verschiebt eine reisende Flotte um eine Runde; die Vorschau berücksichtigt
das ohne Doppelzählung. **Auflösen** gibt die Schiffe auf. Beide Aktionen werden
als Befehle für die Rundenabgabe vorgemerkt.

## Reserven und Produktionsverlegungen

Die Garnisonsreserve legt fest, wie viele Schiffe am eigenen System zurückbleiben
sollen. Direkte Sendungen, vorgemerkte Befehle und Verlegungen beachten sie.
Beim Besitzerwechsel wird die Reserve auf null zurückgesetzt.

Mit **Reserven gesammelt setzen** wählst du mehrere Systeme und entweder eine
feste Reserve oder **Eine Produktionsrunde zurückbehalten**. Letzteres setzt je
System dessen aktuelle Produktionsmenge als Reserve. Die Reserve wird höchstens
auf die aktuelle Garnison gesetzt. Bereits vorgemerkte Sendungen müssen weiterhin
dazu passen; andernfalls wird die gesamte Änderung abgelehnt.

Eine **Produktionsverlegung** sendet jede Runde eine feste Anzahl Schiffe.
Ziehe ein eigenes System auf das Ziel oder wähle die Verlegung im Sendedialog.
Unter **Verlegungen** kannst du die Menge **Anpassen** oder die Route **Löschen**.

Das gemeinsame Limit aller ausgehenden Verlegungen ist die eigene Produktion plus
die eingehenden Verlegungsmengen. Eingehende Schiffe brauchen trotzdem ihre normale
Reisezeit. Wenn nötig, nutzt die Verlegung vorhandene Garnison, lässt aber die Reserve
stehen. Nicht versandte Produktion bleibt am System. Die Karte zeigt die Routen
violett gestrichelt mit ihrer Menge; **Logistik** hilft bei der Übersicht.

## Runden, Fristen und Kommunikation

Nach **Runde abgeben** ist der Knopf gesperrt. Die Rundenleiste zeigt abgegebene
Spieler und nennt diejenigen, auf die noch gewartet wird. Sobald alle Menschen
abgegeben haben, plant die KI auf demselben Ausgangsstand. Dann werden Befehle,
Produktion, Ankünfte und Kämpfe ausgewertet.

Bei mindestens zwei Menschen gelten zwei Uhren: das Rundenlimit ab Rundenbeginn
(Vorgabe fünf Minuten) und das Nachzüglerlimit, sobald nur noch eine Abgabe fehlt
(Vorgabe eine Minute). Die frühere Frist beendet die Runde. Säumige spielen mit
ihren vorhandenen Befehlen; nach drei versäumten Abgaben in Folge übernimmt die KI.
Der Gastgeber kann über das Kartenmenü die **Rundenfristen** anpassen.
Eine Partie mit nur einem Menschen hat keine automatische Rundenfrist.

Über das Kartenmenü erreichst du den **Partie-Chat**. **Zur Lobby** verlässt nur die
Kartenansicht. **Partie verlassen** übergibt deinen Sitz an die KI. Erlaubt die Partie
den Wiedereinstieg, kannst du später zurückkehren.

## Schlachten ansehen oder sofort auswerten

Eine offene Kampfmeldung zeigt zunächst den Ort. Besitzer und Ergebnis am
betroffenen System bleiben verdeckt, bis du die Schlacht bestätigt hast.
Klicke die Meldung an, um die Wiedergabe zu öffnen, und bestätige mit
**Ergebnis übernehmen**. Die Auswahl verändert ausschließlich die Darstellung;
die Runde ist zu diesem Zeitpunkt bereits berechnet.

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
ein- oder ausschalten. **Ton testen** prüft die Ausgabe; der Browser benötigt zuvor
eine Nutzeraktion, um Ton freizugeben.

Bestätigungen bleiben je Konto, Partie, Runde und Ereignis gespeichert. Ein
Neuladen oder anderes Gerät setzt sie nicht zurück. Geschwindigkeit und Ton ändern
weder die Kampfstärke noch Rundenfristen oder Ergebnisse.

## Sieg, Fortsetzung und Archiv

Gewonnen hat, wer den eingestellten Anteil aller Systeme kontrolliert; neutrale
Systeme zählen zur Gesamtzahl. Die Vorgabe ist 70 %. Wenn die letzte Runde noch
offene Schlachten enthält, erscheint zunächst der Bericht. Erst nach deren
Bestätigung öffnet sich das Ergebnis mit persönlichen Schiffssummen.

Der Sieger darf **Bis zur vollständigen Eroberung weiterspielen** wählen. Das Ziel
steigt dann auf 100 %, die Abgaben werden zurückgesetzt und die Fristen starten neu.
Die anderen Teilnehmer werden automatisch informiert. Das zuerst gespeicherte
Ergebnis bleibt in der Statistik bestehen.

Im **Archiv** kannst du die beendete Karte lesend öffnen, eine Teilnehmerperspektive
wählen und den Kriegsnebel umschalten. Die Zeitleiste zeigt gespeicherte Runden ohne
Kriegsnebel; bei **Endstand** gelten wieder Perspektive und Nebelschalter. Ältere
Partien ohne gespeicherte Runden bieten nur den Endstand. Private Archive bleiben
auf Berechtigte beschränkt; öffentliche erlauben fremde Zuschauer nur bei aktivierter
Zuschaueroption. Die **Statistik** zeigt deine Ergebnisse und Gegnerbilanzen.

## Vorlagen und Revanche

Öffne bei einer eigenen Partie in Lobby oder Archiv **Als Vorlage / Revanche**.
Vergib einen Namen. Mit **Bisherigen Teilnehmern ihre Plätze reservieren** werden
die früheren menschlichen Plätze reserviert; ohne Haken entsteht nur eine Vorlage
mit denselben Ausgangseinstellungen.

Die neue Partie erhält eine frische Karte, ist privat und startet nicht automatisch.
Die Teilnehmer müssen selbst beitreten, der Gastgeber startet anschließend.
Alte Partien ohne gespeicherte Ausgangseinstellungen zeigen einen Hinweis und
können nicht als Vorlage dienen.

## Persönliche Einstellungen und Konto

Geschwindigkeit, Ton, Berichts- und Logistikfilter, Routensuche und Seitenleistenbreite
werden je Konto gespeichert und bei einer späteren Anmeldung wieder geladen.
Sprache bleibt sitzungsbezogen; Kartenposition und Zoom werden je Partie in der
Browsersitzung behalten.

**Mein Bereich** bündelt Lobby, Statistik, Archiv und die Kontolöschung.
Impressum und Datenschutz sind über die gemeinsame Fußzeile erreichbar.
Die Kontolöschung ist dauerhaft; Einzelheiten zu Daten und Aufbewahrung stehen
in der Datenschutzansicht der Anwendung.
