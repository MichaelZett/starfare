# Changelog

Alle nennenswerten Änderungen an Starfare. Format nach
[Keep a Changelog](https://keepachangelog.com/de/1.1.0/), Versionierung nach
[SemVer](https://semver.org/lang/de/). Beim Release wird `## Unreleased` in
`## <appVersion> - <Datum>` umbenannt; der Release-Job extrahiert genau diesen
Abschnitt als Release-Body und bricht ohne ihn ab.

## Unreleased

- Interne Spaceward-Regelversion 1.1.0: erlaubte Bündnisse und Bündnissiege
  getrennt je Partie wählen, mit eigenen Einzel- und Bündnisschwellen.
  Gemeinsame Ergebnisse nennen alle Sieger in Bericht, Dialog, Archiv und
  Statistik; jeder Sieger darf bis zur vollständigen Eroberung fortsetzen.
  Bestehende Partien behalten ihre Regelversion und Vorgaben. Der KI-Prüfstand
  vergleicht identische Karten mit ein- und ausgeschaltetem Bündnissieg.
  Spaceward bleibt bis zur Variantenabnahme gesperrt.

- Reproduzierbarer KI-Strategieprüfstand für SectorForces und Spaceward:
  Strategiematrix mit getauschten Startplätzen, Mehrspieler- und Bündnisversuche
  sowie Vergleich einzelner Entscheidungen ab identischem Spielstand.
  Auswertungen enthalten Rundenverläufe, Befehle, Zufallsstartwerte und getrennte
  Siege, Unentschieden und Rundenlimits. Läuft ohne Oberfläche oder Datenbank;
  die reguläre KI und bestehende Spielregeln bleiben erhalten.

- Interner Spaceward-Kampf (M6): Bündnisgruppen kämpfen gemeinsam, mehrere
  feindliche Seiten gleichzeitig. Verluste und Abschüsse werden anteilig
  ohne Doppelzählung verteilt. Eroberungsziele lassen sich vor dem Versand
  benennen; verbündete Überlebende behalten ihren Eigentümer. Berichte und
  Wiedergabe zeigen alle beteiligten Seiten. Persönliche Bestätigungen,
  Neustart, Archiv und Statistik bleiben erhalten. SectorForces behält
  seine Kampfregeln; Spaceward bleibt bis zur Abnahme M7 gesperrt.

- Interne Spaceward-Diplomatie (M5): gemeinsame Bündnisgruppen mit einstimmiger
  Aufnahme und vereinbarter Kündigungsfrist. Austritte zeigen eine feste
  Wirksamkeitsrunde; Friständerungen brauchen Zustimmung aller und ändern
  laufende Kündigungen nicht. Verbündete Zwischenstationen erhalten getrennte
  Flotten; Austritte führen automatisch zur geschützten Heimreise. Verträge
  und Reisephasen überstehen Neustarts. Gemeinsamer Kampf folgt mit M6, die
  öffentliche Freigabe mit M7. SectorForces bleibt unverändert.

- Interne Spaceward-Navigation (M4): feste Reichweite je Partie, automatische
  Routen über eigene Zwischenstationen und eine volle Runde zum Auftanken.
  Reiseplan und Etappen bleiben nach Neuladen erhalten; Stationsverlust
  unterbricht die Weiterreise. Verlegungen und KI beachten dieselbe Wegprüfung.
  Vorschau, Flottendetails und Kartenlinien zeigen die tatsächlichen Etappen.
  SectorForces behält unbegrenzte direkte Flüge.

- Interne Spaceward-Wirtschaft (M3): Produktion zwischen Schiffbau und Ausbau
  verteilen; Ausbaukosten 2 × aktuelle Kapazität, Maximum 50. Teilfortschritt
  und Kapazität bleiben bei Eroberung erhalten. Industriegebäude, Verteilung,
  Fortschritt und Lieferengpässe in der Systemsicht; Speicherung und historische
  Rundenstände ergänzt. SectorForces behält seine Regeln. Spaceward bleibt
  bis zur gemeinsamen Abnahme für neue Partien gesperrt.

- Spielvarianten und feste Regelversionen: SectorForces bezeichnet die bisherigen
  Classic-Regeln (1.0.0). Der erweiterbare Katalog zeigt Spaceward als noch nicht
  spielbar. Lobby, Beitritt, Verwaltung, Archiv und Statistik zeigen die Regeln;
  Vorlagen und Revanchen übernehmen sie. Alte Spielstände und Ergebnisse behalten
  SectorForces, unbekannte Regeln werden ohne Umschreibung abgewiesen.
  Statistik nach Variante filterbar; Rundenauswertung über registrierte Regelversionen.

- Classic-Bestandsschutz: feste Vergleichsszenarien für Produktion, Reserve,
  Verlegungen, Befehle, Ankünfte, Mehrparteienkampf, Sieg und Fortsetzung.
  Synthetische alte JSON-Spielstände und Archivframes sichern Speicherkompatibilität,
  Neustart und unveränderte Daten bei lesenden Ansichten ab.

- Grafische Systemsicht für Classic: auswählbare Hauptreihensterne, Doppelsonnen,
  zwei bis fünf Planeten, Monde, Zwergplaneten und Asteroidengürtel. Stabile,
  schematische Illustrationen ohne Änderung von Spielregeln oder Spielständen;
  Kriegsnebel und offene Schlachten bleiben geschützt. Werften stellen nur die
  vorhandene Schiffsproduktion dar. Astronomische Quellen sind dokumentiert.

- Deutsche Spielanleitung ergänzt und mit der README verlinkt. Nutzer- und
  Architekturdokumentation zu Siegbedingungen, Fortsetzung, Schlachtauswertung,
  Sammelaktionen und gespeicherten Einstellungen auf den aktuellen Stand gebracht.

- Desktop-Bedienung: Eingaben und Fokus bleiben bei unveränderten Aktualisierungen
  erhalten. Sichtbare Kartenknöpfe bieten Zoom, Zurücksetzen, Galaxie und eigenes
  Reich; eine Legende erklärt die Darstellung. Kartenpunkte und Aktionskacheln
  lassen sich mit Enter und Leertaste bedienen. Kontonavigation und rechtliche
  Fußzeile sind vereinheitlicht, die rechtlichen Ansichten deutsch und englisch.
- Flotten und vorgemerkte Sendebefehle haben eine gemeinsame Ankunftsvorschau
  je Ziel und Runde; Wartebefehle verschieben die Ankunft ohne Doppelzählung.
  Sendebefehle können bearbeitet und der letzte Befehlschritt derselben Runde
  zurückgenommen werden, auch nach Neuladen oder Neustart.
- Flottenversand und Garnisonsreserven können für mehrere eigene Systeme
  gemeinsam geplant werden. Ungültige oder veraltete Sammelaktionen werden
  vollständig abgelehnt. Reserven können eine feste Zahl oder eine Produktionsrunde sein.
- Bei Schlachtgeschwindigkeit „Sofort“ können alle offenen Schlachten zusammen
  bestätigt werden. Geschwindigkeit, Ton, Berichts- und Logistikfilter,
  Routensuche und Seitenleistenbreite werden je Konto dauerhaft gespeichert.
- Neue Partien speichern ihre Ausgangseinstellungen und lassen sich aus Lobby
  und Archiv als Vorlage oder Revanche mit frischer Karte anlegen. Eine Revanche
  reserviert die bisherigen menschlichen Plätze; die Teilnehmer treten selbst bei.
  Alte Partien ohne gespeicherte Ausgangseinstellungen zeigen einen Hinweis.

- Die Rundenabgabe heißt ausdrücklich „Runde abgeben“. Nach der Abgabe ist der
  Knopf gesperrt; die Rundenleiste nennt die noch fehlenden Spieler. Der Stand
  bleibt nach Neuladen erhalten und wird beim Rundenwechsel zurückgesetzt.
- Beim Herunterfahren beenden die Benachrichtigungsdienste ihre Zustellung vor
  dem Schließen der Datenbank. Präsenzabmeldungen aktualisieren dann keine
  Lobby- oder Onlineansichten mehr.

- KI-Siege zählen in der persönlichen Statistik als Niederlage. Das gespeicherte
  erste Ergebnis bleibt auch nach Fortsetzung und erneutem Spielende erhalten.
- Neue Regressionstests sichern KI-Persistenz, Rundenfristen, Siegberichte und
  lesende Kapazitätsabfragen. Browserabläufe prüfen Logistik, Mengenwahl,
  Flottenauswahl, Kartenpositionen sowie Chat und Fortsetzung mit zwei Sitzungen.

- Logistik-Reiter ist sichtbar. Flottenauswahl zeigt zuverlässig Flottendetails;
  Zoom und Kartenposition werden je Partie gespeichert.
- Verlegungsmengen und Schnellknöpfe verwenden die freie Produktionskapazität,
  auch wenn sie größer als die aktuelle Garnison ist. Ein Moduswechsel gleicht
  Zahlenfeld, Schieberegler und Vorschau miteinander ab.
- Der geöffnete Partie-Chat zeigt fremde Nachrichten unmittelbar. Chat-Nachrichten
  bauen die Karten-Seitenleiste nicht mehr neu auf.
- Siegberichte zeigen die tatsächlich geltende Siegschwelle, einschließlich
  100 Prozent nach Fortsetzung. Ältere Berichte bleiben mit der damaligen
  70-Prozent-Vorgabe lesbar.
- Die Schlachtwiedergabe übernimmt den tatsächlichen Kampfsieger aus dem Bericht;
  die Ergebnisfarbe stimmt auch bei 0:0 Restschiffen.
- Abfragen der freien Verlegungskapazität verändern den Spielstand nicht mehr
  unter dem Leselock.

- Automatisches KI-Weiterspielen speichert jede Runde einschließlich Endstand
  und Archiv. Das erste Ergebnis wird auch auf diesem Weg in der Statistik erfasst.
- Weiterspielen nach einem Sieg beginnt mit einer neuen Rundenfrist und
  benachrichtigt alle Partieansichten. Alte Ergebnisdialoge werden geschlossen;
  beim nächsten Spielende müssen offene Schlachten wieder zuerst ausgewertet werden.

- Öffentliches Impressum und Datenschutz ergänzen den Kontolöschweg. Das
  Hetzner-Profil liest Basis-URL sowie Betreiber-, Anschrift- und Kontaktdaten
  ausschließlich aus Deployment-Variablen; ohne diese Werte kann die öffentliche
  Instanz nicht starten.
- Die CI analysiert mit SonarCloud. Zwei verbleibende Methodenreferenz-Befunde
  im Host-Test sind bereinigt.

- Der Ergebnisdialog kennzeichnet das persönliche Ergebnis und den Sieger in
  dessen Fraktionsfarbe. Nach der letzten Schlacht führt die Hauptaktion in die
  Nachbetrachtung; die zweite Aktion öffnet die Lobby. Das Archiv ergänzt einen
  Filter für eigene Siege, Niederlagen und Partien ohne Sieger.

- Die Nachbetrachtung zeigt Schlacht- und Eroberungsmarken je Perspektive.
  Ein Klick springt zur gespeicherten Runde und wählt das betroffene System;
  lückenhafte Rundenzahlen werden übersprungen. Historische Stände sind als
  vollständig aufgedeckt gekennzeichnet, am Endstand gelten Perspektive und Nebel.

- Die Statistik weist Siege, Niederlagen und Unentschieden mit Zahlen und
  Bilanzbalken aus. Gegnerbilanzen und Partienhistorie erscheinen als Karten;
  Onlinezustände haben eine Textangabe.

- Der Spielassistent fragt einen Partienamen ab und prüft vor dem Anlegen eine
  Zusammenfassung der normalisierten Regeln, Startwerte und Farben. Eine
  schematische Galaxieansicht zeigt die gewählte Verteilung; Einstellungen lassen
  sich vor der endgültigen Bestätigung weiter anpassen.

- Lobby-Partiekarten zeigen freie Plätze und Teilnehmerfarben. Ihre Hauptaktion
  richtet sich nach dem eigenen Zugang; Verwaltung und Abbruch liegen gesammelt
  unter weiteren Aktionen. Der Beitrittsdialog nennt Partie und verfügbare Sitze.

- Der Logistikmodus hebt Verlegungsrouten auf der Karte hervor. Er zeigt je System
  geplanten Zufluss und Ausgang getrennt von der nächsten möglichen Lieferung,
  markiert Lieferengpässe und bietet passende Filter sowie unmittelbares Anpassen.

- Identity-Baustein auf Version 1.2.0 aus Maven Central umgestellt; der
  GitHub-Packages-Zugang ist für Build und E2E nicht mehr erforderlich.
  Damit greift der Schutz gegen Passwortraten: Nach drei falschen Passwörtern
  in Folge ist das Konto vorübergehend gesperrt, jeder weitere Versuch wartet
  länger; „Passwort vergessen“ hebt die Sperre auf. Die Anmeldung über externe
  Anbieter (OAuth2/OIDC) bringt der Baustein mit, sie bleibt abgeschaltet.
- Nachbetrachtungs-Zeitleiste verwendet für den Endmarker und seine Bewegung
  64-Bit-Arithmetik, damit die Addition am maximalen Rundenzähler nicht überläuft.
- Der Systeminspektor zeigt Garnison, Produktion sowie – sofern für das eigene
  System verfügbar – Reserve und versendbare Schiffe als gemeinsame Kennzahlen.
- Der Sendedialog zeigt die absolute Ankunftsrunde und eine grafische
  Mengen-/Kapazitätsanzeige. Flotten und Produktionsverlegungen haben passende
  Bestätigungstexte; nach einem Rundenwechsel muss der Dialog neu geöffnet werden.
- Flotten, Befehle und Produktionsverlegungen erscheinen als Routen-Karten.
  Flotten und Befehle sind nach Ankunftsrunde gruppiert und alle Listen lassen
  sich nach Quelle oder Ziel filtern.
- Rundenansicht und Karten-Seitenleiste ordnen Ereignisse gleich: offene
  Schlachten zuerst, danach Kämpfe, Verstärkungen und Produktion. Aufklappbare
  Filter zeigen Anzahlen; „Nächste offene Schlacht“ bleibt auch bei Filtern erreichbar.

- Schlachtbestätigungen bleiben auch bei identischen Ereignissen, Sortierung
  und Filtern eindeutig; bereits ausgewertete Kämpfe in der Karten-Seitenleiste
  werden nicht wegen einer anderen offenen Schlacht desselben Systems erneut offen.
- Berichte speichern gewürfelte Kampfstärken ungerundet. Die Wiedergabe zeigt
  genügend Nachkommastellen, um den entscheidenden Unterschied zu erkennen;
  ältere Berichte bleiben lesbar.
- Die Statistik lädt Teilnehmer und KI-Gegner getrennt in zwei Abfragen,
  ohne zusätzliche Abfrage je Partie und ohne kartesisches Produkt.
- Der Kartenkopf bleibt auch bei 1024 Pixeln und nach der Schlachtauswertung
  innerhalb der Seitenbreite.

- Die Schlachtwiedergabe kennzeichnet beide Seiten klar als „Du“, Gegner oder
  neutrales System. Berichts-, Flotten- und Befehlsansichten sind größer,
  kontrastreicher und stärker mit Symbolen gestaltet.
- Der Neue-Spiel-Wizard legt den Kampfzufall für die gesamte Partie zwischen
  0 und 30 % fest (Vorgabe ±10 %). Vor jedem Gefecht würfeln Angreifer und
  Verteidiger ihre Kampfstärke unabhängig; die Schlachtwiedergabe zeigt beide
  gewürfelten Werte.
- Sieg, Niederlage und offene Karte erscheinen erst nach der Bestätigung aller
  eigenen Schlachten. Auch Neuladen umgeht die Auswertung nicht mehr; die
  Reichssummen und Systemdetails verraten vorher keine Schlachtergebnisse.
- Die Trennlinie zwischen Karte und Seitenleiste ist verschiebbar; das Layout
  wurde auch bei 1024 Pixeln Breite im Browser geprüft.
- Kampfzeilen zentrieren und markieren das zugehörige System auch beim Öffnen
  der Schlachtwiedergabe.
- Flotten und vorgemerkte Befehle zeigen eine eigene Ankunftsrunden-Spalte;
  ein vorgemerkter Wartebefehl verschiebt die angezeigte Ankunft um eine Runde.
- Systemdetails und Sendeflotten-Dialog zeigen eingehende und ausgehende
  Produktionsverlegungen als Mengen pro Runde.
- Kampfgeräusche werden direkt beim Browserklick freigeschaltet, sind deutlicher
  hörbar und reagieren sofort auf den Tonschalter. Die Schlachtwiedergabe zeigt
  unregelmäßige Verlustschritte mit unveränderten Endzahlen.

- Identity-Baustein 0.9.1 (eigene Mail-Zustellung und Kopfbereich über den
  Formularen als optionale Erweiterungspunkte; für Starfare ohne Änderung).
- Die Lobby-Tabelle passt auf Desktop-Breite: Spielname und Spielerliste
  werden bei Überlänge mit „…" gekürzt, der volle Text steht im Tooltip.
- Identity-Baustein 0.8.0: Konten, Rollen und Tokens liegen im eigenen
  Datenbankschema `identity` mit eigener Migrationshistorie; der erste Start
  nach dem Update zieht die bestehenden Tabellen einmalig dorthin um.
  `spring.flyway.out-of-order` entfällt.
- Produktionsverlegungen sind als violette Kartenkanten sichtbar und lassen
  sich durch Ziehen zwischen Systemen anlegen oder ersetzen.
- Der Reiter Produktionsverlegungen bietet je Route Anpassen im vorhandenen
  Kapazitätsdialog sowie Löschen.
- Berichtssysteme erhalten Kartenmarkierungen: Berichtszeilen zentrieren die
  Karte, Markierungen öffnen die passende Berichtszeile.
- Zuschauer laufender Partien wählen nun eine Teilnehmerperspektive und den
  Kriegsnebel. Der Zugriff bleibt vollständig lesend.
- Schlachten werden aus dem Bericht heraus in einer eigenen, abschaltbar
  vertonten Wiedergabe gezeigt. Anfangs- und Endstärke animieren getrennt von
  Besitzwechseln und übrigen Berichtsergebnissen.
- Das Ergebnis einer Schlacht bleibt bis zur bewussten Bestätigung verborgen;
  ein großer Kartenmarker markiert das betroffene System bis dahin.
- Die Kampfdarstellung lässt sich im Neue-Spiel-Wizard als Vorgabe und im
  Rundenbericht je Runde zwischen Wiedergabe und Direktauswertung umschalten.
  Der erste Klick auf eine Kampfkarte schaltet den Browserton frei.
- Kartensysteme skalieren bei vollständiger Sicht nach Produktion und bei
  Sensorreichweite nach der geschätzten Produktion; unbekannte Systeme bleiben
  gleich groß.

### Added
- Unabhängige vollständige Partiearchive in `game_archives`; eine standardmäßig
  deaktivierte Bereinigung kann operative Sitzungen nach 90 Tagen entfernen,
  ohne Nachbetrachtung, Wiederholung oder Statistik zu verlieren.
- Nachrichtenverwaltung mit Lesestatus, nutzerbezogenem Archivieren von
  Unterhaltungen und einer Aufbewahrungsgrenze von 365 Tagen.
- Wiederholung im Archiv: Die Kartenansicht besitzt für neu beendete Partien
  eine Zeitleiste über die gespeicherten Kartenstände jeder Runde.
- Garnisonsreserve je eigenem System: reservierte Schiffe bleiben bei direkten
  Flotten und Produktionsverlegungen zurück. Der Sendedialog bietet „Alle
  außer Produktion“ als Schnellwahl.
- Persönliche Statistik unter `/statistics`: abgeschlossene Partien, Siege,
  Niederlagen und Bilanz je Gegner. Ergebnisse liegen dauerhaft in
  `game_results`, unabhängig von später bereinigten Spielsitzungen.
- Karten-Seitenleiste mit umschaltbaren Ansichten für Kontakte, Details,
  Flotten, Befehle, Produktionsverlegungen und Rundenbericht. System- und
  Flottenauswahl zeigt die Details neben der Karte, einschließlich Besitz- und
  Reisehistorie; neue Befehle und Züge wählen automatisch den passenden Reiter.
- Neue KI-Gegner tragen sprechende Namen. Der Spiel-Wizard nutzt auf breiten
  Bildschirmen ein zweispaltiges Layout, damit seine Erklärungen und Felder
  lesbar bleiben.
- Beim Sieg einer anderen Fraktion erhalten alle übrigen Teilnehmer eine
  Niederlagenmeldung im Rundenbericht. Startsysteme werden bei jeder
  Galaxieverteilung mit maximalem Abstand gewählt.
- Nachbetrachtung beendeter Partien: Perspektive jedes Teilnehmers einschließlich
  KI auswählen und Kriegsnebel umschalten. Ohne Nebel sind alle Systeme und
  Flotten sichtbar; die Ansicht bleibt lesend.
- Neue Partien sind privat. Der Host kann sie vor dem Start veröffentlichen;
  private Partien bleiben auf Host, Sitzinhaber und Eingeladene beschränkt.
  Direkte Kartenaufrufe prüfen die Zugriffsrechte ebenfalls.
- Archiv unter `/archive`: beendete Partien verlassen die Lobby und bleiben
  mit Sieger, Abschlusszeit und lesender Kartenansicht erreichbar. Alte
  Spielstände ohne Abschlusszeit zeigen einen unbekannten Zeitpunkt.

- Konten, Login, Registrierung mit E-Mail-Bestätigung und Passwort-Reset
  über den externen Baustein `de.zettsystems:identity-core`/`identity-vaadin`.
- Direktnachrichten werden persistiert (`direct_messages`); Einladungen
  überleben einen Server-Neustart.
- Mailpit im lokalen `docker-compose.yml`; Profil `brevo` für echten
  SMTP-Versand.
- Dependabot für GitHub Actions und Gradle (wöchentlich, gruppiert);
  Actions auf aktuelle Major-Versionen gehoben.
- Release-Workflow: Push auf `main` mit Nicht-SNAPSHOT-`appVersion` erzeugt
  Tag `v<version>` und GitHub-Release mit `starfare.jar`, danach Auto-Bump
  auf das nächste Patch-SNAPSHOT. Kein Deploy.

- Cucumber/Selenium-Smoke-Test (`./gradlew e2eTest`, CI-Job `e2e`): Konto
  anlegen, bestätigen, anmelden, Spiel anlegen, starten, eine Runde spielen.
- Sensorreichweite: eigene Systeme und Flottenziele decken Systeme in bis zu
  zwei Reiserunden auf. Dort sind Besitzer und Farbe aktuell, die Garnison nur
  grob (`~G:`); ohne Sensorkontakt bleibt die exakte, aber alte Aufklärung aus
  vergangenen Kämpfen (`G:` plus „zuletzt gesehen" im Tooltip).
- Farbwahl je Sitz im Neue-Spiel-Wizard statt einer Farbe für den Ersteller;
  doppelt gewählte Farben werden auf freie Palettenwerte ausgewichen.
- Lobby: Freitextsuche über Partie- und Spielernamen sowie ein Filter
  (alle / meine / offene / laufende / beendete Partien).
- Rundenfristen je Partie: Rundenlimit ab Rundenbeginn (Standard 5 min) und
  Nachzügler-Limit, sobald nur noch einer fehlt (Standard 1 min), beide im
  Wizard von 30 s bis 7 Tage wählbar. Ab zwei Menschen endet die Runde mit der
  ersten abgelaufenen Frist; wer sie verpasst, zieht mit den bis dahin
  erteilten Befehlen. Erst nach drei verpassten Runden in Folge übernimmt die
  KI den Sitz; erlaubt die Partie den Wiedereinstieg, kann der Spieler ihn
  zurückholen. Ersetzt `starfare.game.inactivity-timeout`.
- Eine Leiste unter dem Kopf zeigt alle Mitspieler mit Haken nach der Abgabe
  und den Countdown beim letzten Fehlenden bzw. bis zum Rundenende.
- Die KI plant ihre Züge parallel zu den Menschen auf demselben Stand und hat
  keinen Informationsvorsprung mehr; ihre Befehle laufen zusammen mit denen
  der Menschen ein.
- Neue Spielregel „Reihenfolge mehrerer Angreifer" an einem System: zufällig
  (Standard) oder stärkster zuerst.
- Rundenbericht: Kampfereignisse lassen sich per Klick mit kurzer Animation
  und Ton aufdecken.
- Wizard: Produktionsverteilung (gleichmäßig oder um die Mitte gehäuft) und
  Sternenverteilung (zufällig oder gleichmäßig) sind wählbar. Bei
  gleichmäßiger Verteilung liegt ein System je Rasterzelle und die
  Heimatsysteme werden maximal auseinandergelegt.
- Der Wizard begrenzt Produktion auf 1–20 und die Startgarnison auf 1–50;
  eine Galaxie enthält 8–120 Systeme und mindestens ein System pro Teilnehmer.
- Karte: für die laufende Runde geplante Flüge erscheinen als gestrichelte
  Bahn; Zoom und Ausschnitt überleben den Rundenwechsel.
- Produktionsverlegungen haben eine feste Größe statt „die ganze Produktion".
  Ein System darf insgesamt höchstens seine eigene Produktion plus alles,
  was per Verlegung hereinkommt, weiterleiten; mehrere Ziele teilen sich
  dieses Budget. Die Karte zeigt bei belegten Systemen die noch freie
  Produktion in Klammern, der Rest bleibt als Garnison liegen.
- Nach Spielende deckt die Karte alle Systeme auf.

### Changed
- Abhängigkeiten und Build-Werkzeuge aktualisiert, darunter Vaadin 25.2.8,
  Identity 0.7.1, Sonar-Plugin 7.5.0, JaCoCo 0.8.15,
  NullAway 0.14.1 und Gradle 9.7.1.
- Die Wiederholungszeitleiste besitzt einen expliziten Endstand; dort sind
  Teilnehmerperspektive und Kriegsnebel wieder verfügbar.
- Sonar prüft zusätzlich Frontend und Browser-Testquellen.
- OpenRewrite läuft mit einem kuratierten Satz (`ZettSystemsRecipes`,
  `CodeCleanup`, `RemoveUnusedImports`) statt der Migrations-Composites; die
  abgelehnten Recipes stehen begründet im `rewrite`-Block. Einmal über den
  Code gelaufen: Importe vereinheitlicht, qualifizierte Klassennamen ersetzt.
- `spring-security-test` durch `spring-boot-starter-security-test` ersetzt.
- Eigene Theme-Farben ersetzen undefinierte Lumo-Variablen; Statusanzeigen
  und Kartenmarkierungen erhalten kontrastreiche Hintergründe.
- Keine fest eingetragenen Datenbankzugangsdaten in der Anwendung; lokal
  liefert Docker Compose die Verbindung, der Datenbankport ist nur lokal erreichbar.
- Siegbedingung von „mehr als die Hälfte" auf 70 % aller Systeme angehoben
  (`GameConfig.VICTORY_SYSTEM_PERCENT`); neutrale Systeme zählen weiterhin
  in die Gesamtzahl. Knappe 13:11-Ausgänge entscheiden damit keine Partie
  mehr.
- Spieler werden überall über die Konto-ID referenziert; der öffentliche
  Spielername ist nur Anzeige (`zs.identity.name-mode: DISPLAY_NAME`).
  Anzeigenamen kommen aus `PlayerDirectory` (kurz gecacht).
- UI arbeitet ausschließlich auf View-Modellen (`PlayerViewState`,
  `GameSummary`); `GameService.snapshot()` entfällt, ArchUnit erzwingt
  `ui` ↛ `domain`.

### Fixed
- Schlacht-Bestätigungen werden je Konto gespeichert (`battle_acknowledgements`)
  statt nur in der Browsersitzung. Eine beendete Partie öffnet in einer neuen
  Sitzung oder auf einem anderen Gerät wieder direkt die Nachbetrachtung, statt
  erneut durch Schlachten und Ergebnisdialog zu führen.
- Der Kampfzufall entscheidet auch kleine Gefechte: Verglichen werden die
  ungerundeten Kampfstärken, gerundet wird nur für die Wiedergabe. Bisher
  endete etwa 3 gegen 3 bei ±10 % stets zugunsten des Verteidigers.
- Einladungen und Freundschaftsanfragen legen ihre Knöpfe in eine eigene
  Zeile; die Lobby scrollte mit offener Einladung seitwärts.
- Das Entladen untätiger Einzelspieler-Partien verklemmt sich nicht mehr mit
  einem gleichzeitigen Speichern; wieder geladene Partien werden erneut
  entladen. Die Lobby liest nur noch die Partie-IDs statt aller Snapshots.
- Befehle auf reinen Archivpartien werden abgelehnt statt mit „Unknown game“
  zu scheitern.
- Zwei gleiche Schlachten einer Runde lassen sich beide bestätigen.
- Die Statistik lädt die KI-Gegner aller Partien in einer Abfrage.
- Beendete Partien öffnen aus dem Archiv wieder lesend: Sitzinhaber landeten
  im Wartezustand vor dem Ergebnisdialog, mit sichtbarem „Nächste Runde" und
  ohne Nachbetrachtung. Der Wartezustand gilt nur noch für eine Ansicht, die
  das Spielende selbst miterlebt hat.
- Der Ergebnisdialog erscheint auch ohne eigene Schlacht in der letzten Runde;
  ein unbeteiligter Verlierer blieb sonst im Wartezustand hängen.
- Host-Wechsel werden dauerhaft gespeichert und nach Neustarts wiederhergestellt.
- Befehle aus alten Dialogen können beendete Partien nicht mehr verändern.
- Sitzvergabe und Persistenz laufen unter demselben Partie-Lock, damit
  gleichzeitige Beitritte keine konkurrierenden Snapshots speichern.

- Nach einem Kampf zeigt die Karte die exakte Garnison aus dem Rundenbericht
  statt der gröberen Sensorschätzung: die Reichweitenprüfung lief vor der
  Auswertung der Aufklärung, und ein angegriffenes System liegt fast immer
  in Reichweite.
- Der Siegtext nannte weiterhin 50 %; er nennt jetzt die konfigurierte Schwelle.
- Systeme am Kartenrand wurden angeschnitten: die Punkte streuten über die
  volle Fläche, sind per CSS aber um ihren halben Durchmesser zentriert.
  Neue Konstante `GameConfig.SYSTEM_MARGIN`.
- „Starten" in der Lobby führt direkt zur Karte, statt in der Lobby zu bleiben.
- Flottenbahnen zeigen beim Überfahren, dass sie anklickbar sind; Systeme
  erklären im Tooltip, ob ein Klick die Quelle wählt oder dorthin sendet.
- Lobby scrollte bei 1366 px seitwärts: die Sidebar (`VerticalLayout`) setzte
  inline `width: 100%` und überstimmte die 260-px-Regel. Der Smoke-Test
  prüft jetzt nach jedem Schritt, dass keine Ansicht seitwärts scrollt.
- Tests liefen mit einer eigenen `application.yaml`, die die
  Haupt-Konfiguration komplett verdeckte (u. a. `open-in-view`,
  Actuator-Exposure); jetzt Profil `test`.
- App-Migrationen laufen im Versionsraum `V2_x`, der Identity-Baustein in
  `V1_x` (`spring.flyway.out-of-order: true`).

### Removed
- Ungenutzte Abhängigkeiten `spring-boot-starter-validation` und Lombok.
- Eigene Benutzerverwaltung (`user_accounts`, `RegisterView`, `LoginView`).
