# Changelog

Alle nennenswerten Änderungen an Starfare. Format nach
[Keep a Changelog](https://keepachangelog.com/de/1.1.0/), Versionierung nach
[SemVer](https://semver.org/lang/de/). Beim Release wird der Abschnitt
`## Unreleased` in `## <appVersion> - <Datum>` umbenannt; der Release-Job
verwendet diesen Abschnitt als Release-Notes und bricht ohne ihn ab.

## Unreleased

- **Spring Boot 4.1.3:** gleiche Version wie die Plattform und Frontier Lords.
- **Plattform-Vertrag 0.4.0:** Starfare baut gegen games-platform-api 0.4.0. Wer wegen verpasster
  Runden hinausfliegt, erscheint im Ergebnis als „wegen Nichtzug ausgeschieden“ (`TIMED_OUT`) mit
  dem Platz, den er beim Rauswurf hielt; gewinnt danach der Computer mit seinem Reich, zählt das
  nicht als sein Sieg. Bisher galt er als aufgegeben und wurde nach dem Endstand eingeordnet.
- **Beendete Partien für die Plattform:** Die Plattform kann die eigenen beendeten Partien eines
  Kontos seitenweise abfragen, neueste zuerst, mit Ende, Ergebnis und Link zur Nachbetrachtung.
  Fremde öffentliche Partien gehören nicht dazu.
- **Chat-Rechte für die Plattform:** Die Plattform fragt Starfare, wer im Chat einer Partie lesen
  oder schreiben darf, nach denselben Regeln wie Starfares eigener Chat. Karte und Warteraum
  melden, welche Partie sie zeigen.
- **Tempostufen:** Wer eine Partie für mehrere Menschen anlegt, wählt statt zweier Fristen ein
  Tempo: Live (5 Minuten je Runde, 1 Minute für den Letzten), Zügig (1 Stunde / 15 Minuten),
  Täglich (1 Tag / 6 Stunden) oder Gemütlich (3 Tage / 12 Stunden). Das Tempo steht nach dem Anlegen
  fest; die Plattform zeigt es in den Partielisten. Ältere Partien behalten ihre Fristen.
- **Hinweise der Plattform:** Starfare meldet der Plattform, wer in welcher Runde bis wann
  abgeben muss, den Start einer Partie, Einladungen und jede verpasste Rundenfrist (mit Zähler und
  ob sie zum Rauswurf führte). Daraus macht die Plattform Hinweise und Erinnerungsmails.
- **Einstellungen über die Plattform:** Geschwindigkeit, Ton, Filter und Seitenleistenbreite hält
  künftig die Plattform je Konto, damit sie auf jedem Gerät gelten. Der eigene Start speichert sie
  weiter in seiner Datenbank.
- **Als Vorlage in der Nachbetrachtung:** „Als Vorlage / Revanche“ steht bei beendeten Partien jetzt
  in der Leiste über der Karte statt auf der Archivkarte. Das Archiv ist nur noch Teil des eigenen
  Starts; in der Plattform listet künftig die Plattform die beendeten Partien.
- **Verpasste Runden zählen über die ganze Partie:** Wer zum zweiten Mal eine Rundenfrist verpasst,
  fliegt hinaus; der Computer übernimmt den Sitz endgültig, ohne Wiedereinstieg. Bisher übernahm
  der Computer erst nach drei verpassten Runden in Folge, und der Sitz ließ sich zurückholen.
- **Kein falsches Angebot zum Wiedereinstieg:** Die Lobby bietet aufgegebene und hinausgeworfene
  Sitze nicht mehr zum Beitreten an.
- **Partie-Chat nur noch lesen:** Wer eine Partie verlassen oder aufgegeben oder seinen Sitz durch
  verpasste Runden an den Computer verloren hat, liest den Chat weiter, schreibt aber nicht mehr.
  Nach dem Ende der Partie ist der Chat für alle nur noch lesbar. Der Chat zeigt dann einen Hinweis
  statt des Eingabefelds.
- **Plattform-Einbindung aufgeräumt:** Starfare meldet seine Einstellungen selbst an, ein Host
  braucht keinen Einstellungs-Scan der Starfare-Pakete mehr. Lobby, Sprachwahl, Kontomenü,
  Rechtshinweise und das Merken der Sprache gehören nur noch zum eigenen Start und fehlen im
  Spielartefakt. Starfares Stylesheets gelten nur noch auf Starfare-Seiten, nicht mehr auf den
  übrigen Seiten eines Hosts. Anzeigenamen in Listen kommen in einer Abfrage von der Plattform;
  der eigene Zwischenspeicher entfällt, Umbenennungen erscheinen sofort.
- **Aufgeben:** Neben **Verlassen** gibt es **Aufgeben**. Der Computer übernimmt das Reich bis zum
  Ende der Partie; anders als beim Verlassen gibt es keinen Weg zurück, auch wenn die Partie
  Wiedereinstieg erlaubt.
- **SectorForces nach Galactic Empire (1983):** Im Spielassistenten gibt es den neuen Abschnitt
  **Galaxie-Ereignisse und Wertung**. Alles ist zunächst aus; ältere Partien bleiben unverändert.
  - Zufallsereignisse **aus / selten / normal**, jedes Ereignis einzeln abwählbar: kosmische
    Stürme (alle Flotten eine Runde später), Raum-Zeit-Erschütterung (halbe Restflugzeit),
    mysteriöses Verschwinden von Schiffen, Raumhafenbeben und Supernova.
  - Supernova: Eine Runde vorher meldet der Bericht „Sonne instabil“, auf der Karte pulsiert das
    System. Danach ist es für immer zerstört, zählt nicht mehr zur Siegschwelle und ist kein Ziel
    mehr. Die Garnison flieht zu einem zufälligen System, Flotten auf dem Weg dorthin springen
    weiter. Heimatwelten explodieren nie.
  - Piraten (nur mit Supernova): Die Garnison einer neutralen Sonne flieht als Piratenflotte ohne
    Sitz. Gewinnt sie, wird das angegriffene System neutral.
  - **Startausgleich nach Lage:** Wer nah an Gegnern startet, bekommt mehr Schiffe; wer viele
    neutrale Systeme in der Nähe hat, weniger (zwischen halber und doppelter Startgarnison).
  - **Spielende nach Runden** (10–500): Erreicht niemand die Siegschwelle, gewinnt nach der
    letzten Runde die höchste Wertung (400 je System, 25 je stationiertes Schiff, 20 je Schiff
    unterwegs). Die letzten fünf Runden werden angekündigt; danach gibt es keine Fortsetzung.
  - Die Zusammenfassung des Assistenten nennt die aktiven Ereignisse.

- Die wirkungslosen Startprofile `economy-preview` und `research-preview` sind entfernt; neue
  Partien nutzen ohnehin immer die aktuellen Regeln.
- Diplomatie, Fortsetzung nach dem Sieg und offene Schlachten am Spielende werden ohne Browser
  bei jedem Build geprüft; die Oberfläche zählt jetzt zur Testabdeckung.

## 0.2.11 - 2026-11-19

- Die alten Sozial- und Ergebnistabellen (Freundschaften, Direktnachrichten, Sichtbarkeit, Teilnehmer,
  Sieger und Gegner der Ergebnisse) werden gelöscht; Ergebnisse, Statistik und Kontakte führt die
  Plattform. Starfare merkt sich je Partie nur noch, wann sie zuerst endete.
- Tests: Beitrittsdialog, Förderautomatik, Rundenfristen und Kartenlegende haben eigene Tests.
- Lokale Datenbank und Tests laufen auf PostgreSQL 18.7 (Version zentral als `postgresVersion` in
  `gradle.properties`, geprüft durch `verifyPostgresVersion`). Die lokale Compose-Datenbank nutzt das
  neue Volume `postgres_data18`; vorhandene Daten per Dump und Restore umziehen.

## 0.2.10 - 2026-11-18

- Starfare fasst die alten Sozial- und Ergebnistabellen nicht mehr an; ein späteres Release löscht
  sie. Die Kontolöschung entfernt weiterhin die persönlichen Anzeige-Einstellungen.
- Dokumentation beschreibt die aktuelle Spaceward-Regelfassung 1.3.0-r1.

## 0.2.9 - 2026-11-18

- Ein offener Verwalten-Dialog hält eine geschlossene Browserseite nicht mehr am Leben: Ereignisse
  anderer Partien schlugen danach mit einem Fehler fehl.
- Der Verwalten-Dialog bietet zum Einladen nur noch an, wen der Host sehen darf; wer sich vor ihm
  verbirgt, erscheint nicht mehr in der Liste und wird nicht erst beim Einladen abgelehnt.
- Intern: Der nicht mehr genutzte Abbruch-Zustand einer Partie (`active`) entfällt; ältere Spielstände
  laden unverändert.
- Tests: Diplomatie-Dialog, Archiv- und Lobbyfilter, Kartenkopf, Nachbetrachtung, Rundenbericht
  und Wirtschaftsliste haben eigene Tests; Mutationstests (PIT) für die Regelpakete stehen bereit.

## 0.2.8 - 2026-11-18

- Starfare führt keine eigene Statistik mehr (Plattform-M14): Die Seite `/starfare/statistics`, ihr
  Eintrag in der Spielnavigation und im Kontomenü des eigenen Starts entfallen. Ergebnisse und
  Statistik führt die Plattform, die das Partieende über `MatchEnded` erfährt. Archiv und
  Ergebnisdialog am Partieende bleiben.
- `game_results` merkt sich nur noch, dass eine Partie geendet hat (`V2_13` macht `game_name`
  optional). Die nicht mehr gelesenen Ergebnisdaten bleiben bis zu ihrer Löschung nach einer Sicherung
  stehen; die Kontolöschung bereinigt sie weiter.
- Das Spielartefakt enthält keine Kopie von `platform.css` und keinen Klassifikator `e2e-fixtures`
  mehr; `syncPlatformStyles` entfällt (Plattform-Z4). Starfares Kopie gilt nur noch im eigenen Start.
- Starfares eigenes Sozialmodul entfällt auch im eigenen Start: keine Freundesliste, Anwesenheit,
  Sichtbarkeitswahl und Direktnachrichten mehr, keine Seite `/starfare/social`. Der Partie-Chat
  bleibt. Die alten Tabellen bleiben bis zu ihrer Löschung nach einer Sicherung; `LegacySocialRecords`
  hält Kontolöschung und die Frist von 365 Tagen weiter ein.
- Der eigene Start braucht keine Anmeldung mehr (Plattform-Z3 b): Wer ihn startet, spielt als
  Standardkonto „Commander“ gegen den Computer. zs-identity, Mailversand, Registrierung, Kontolöschung
  und die Profile `hetzner` und `brevo` entfallen; der eigene Start ist nur noch für lokales Spielen da.
  Im Host ändert sich nichts.
- Das Partieende meldet Starfare der Plattform genau einmal je Partie, auch wenn eine Partie nach dem
  Ende noch einmal gespeichert oder geladen wird.
- Ein Release geht nur noch in Produktion, wenn seine Commit-Nachricht `[deliver]` enthält; sonst nimmt
  das nächste Host-Release die Version mit.
- Werkzeuge aktualisiert: NullAway 0.14.4, ErrorProne 2.53.0, Cucumber 8.1.0, Sonar-Plugin 7.6.

## 0.2.7 - 2026-11-17

- Das Spiel fragt Konten nur noch über games-platform-api 0.3.0 (`PlatformAccounts`: angemeldetes Konto,
  Anzeigenamen, Testerrolle; `AccountRemoved` beim Löschen eines Kontos). zs-identity gehört nur noch zum
  eigenen Start und steckt nicht mehr im Spielartefakt; im Host gilt dieselbe Testerregel wie bisher.

## 0.2.6 - 2026-11-17

- Die Migration zu den Anzeige-Einstellungen heißt `V2_12` statt `V2_10`: In Produktion ist `V2_11` seit
  Oktober eingespielt, und Flyway lehnt eine darunter einsortierte Migration ab (der Plattform-Deploy 0.1.8
  startete deshalb nicht). Ein Test hält fest, dass neue Migrationen die nächste Nummer bekommen. Lokale
  Datenbanken, die `V2_10` schon eingespielt haben, neu anlegen oder dort die Zeile `2.10` aus
  `flyway_schema_history` löschen.

## 0.2.5 - 2026-11-16

- games-platform-api 0.2.0 aus Maven Central.
- Freunde, Anwesenheit, Sichtbarkeit und Chat gehören im Plattform-Host der Plattform
  (games-platform-api 0.2.0, `PlatformFriends` und `PlatformPresence`). Das Spiel fragt nur diese
  Schnittstellen; Starfares eigene Freundesliste, Anwesenheit und Chat bleiben im eigenen Start
  (`standalone.social`) und stecken nicht mehr im Spielmodul. `/starfare/social` und der Eintrag
  „Freunde & Chat“ in der Spielleiste entfallen im Host.
- Einladen im Verwalten-Dialog: Freunde auch, wenn sie gerade nicht online sind, dazu sichtbare
  Spieler, die online sind; sortiert nach Namen. Wer sich vor dem Gastgeber verbirgt oder ihn
  blockiert, ist nicht einladbar.
- Offene Einladungen meldet Starfare an die Plattform (`MatchDirectory.invitationsOf`); die
  Plattform-Lobby zeigt sie und führt in den Warteraum.
- Anzeige-Einstellungen (Tempo, Ton, Filter, Seitenleiste) sind ein eigener Dienst des Spiels und
  brauchen keinen Eintrag in den Sozial-Einstellungen mehr (Migration V2_10). Beim Löschen eines
  Kontos gehen sie mit; Nachrichten in Starfares alten Tabellen verfallen weiter nach einem Jahr,
  auch im Host.

## 0.2.4 - 2026-11-16

- Nur der Gastgeber startet eine wartende Partie; geprüft im Dienst, nicht nur am Knopf. Hat die
  Partie keinen Gastgeber mehr, startet sie jemand mit Sitz. Vorher konnte jeder Mitspieler starten.
- Warteraum je Partie (`/starfare/match/<id>`): Plätze mit Reich und Namen, Eingeladene und freie
  Plätze, Beitreten, Einladung annehmen oder ablehnen, Starten (Gastgeber), Verwalten, Vorlage und
  Abbrechen; aktualisiert sich ohne Neuladen und führt beim Start auf die Karte. Die Plattform
  verlinkt eine noch nicht gestartete Partie dorthin, die Karte einer solchen Partie ebenso.
- Assistent „Neues Spiel“: Ein Klick daneben schließt ihn nicht mehr (nur „Abbrechen“ und Esc).
  Wahl offen (alle können beitreten) oder privat (nur Eingeladene), Vorgabe privat. Der Ersteller
  sitzt immer mit am Tisch; ohne eigenen Sitz anlegen ist ein ausdrücklicher Haken. Die
  Zusammenfassung zeigt den eigenen Namen und die gewählte Sichtbarkeit. Danach geht es in den
  Warteraum (Einzelspiel gegen die KI wie bisher direkt auf die Karte).
- Eigene Seiten für den Plattform-Host: `/starfare/new` öffnet den Assistenten, `/starfare/social`
  zeigt Freundschaftsanfragen, Einladungen, wer online ist und den Chat. Die Spielbeschreibung
  nennt `newMatchPath` und `platformLobby=true`; im eigenen Start bleibt die Lobby unverändert.
- Wer eine Einladung annimmt, erscheint mit Reich und Namen statt als „P2“.
- Eine Partie, die jemand ohne eigenen Sitz organisiert, steht in der Plattform-Lobby unter
  dessen eigenen Partien (vorher fehlte sie dort ganz).
- Lobby: „Wartet auf den Start“ statt „Wartet“, wenn alle Plätze belegt sind.
- Plattform-Vertrag `de.zettsystems:games-platform-api` (0.1.0 von Maven Central) umgesetzt: Starfare meldet dem
  Plattform-Host seine offenen und laufenden Partien je Konto (wer am Zug ist, freie Plätze,
  Sprung zur Karte), kündigt Änderungen an und meldet das Partieende mit Platzierungen (Sieger
  zuerst, dann nach gehaltenen Systemen; Aufgabe und Ausscheiden vermerkt) oder als Abbruch.
  Im eigenen Start ändert sich nichts.
- Einladen und Zurückziehen einer Einladung melden sich als Änderung der Partie; die Lobby der
  Plattform zeigt eingeladenen Konten die Partie dadurch sofort.
- Plattformschrift DM Sans wird selbst ausgeliefert (npm-Paket @fontsource-variable/dm-sans)
  statt auf die Systemschrift zurückzufallen; platform.css mit den allgemeinen Handy-Regeln
  der Plattform übernommen. starfare-mobile.css bleibt unverändert.
- Spaceward ist im Plattform-Host eine Testvariante: Neue Spaceward-Partien (Wizard,
  Vorlage, Revanche) legen nur Tester an (globale Rolle `SYSTEM_ADMIN` oder
  `GAME_TESTER` für Starfare). Andere sehen offene Spaceward-Partien nicht in der Lobby
  und können weder beitreten, zuschauen noch eingeladen werden. Eigene laufende und
  archivierte Partien bleiben vollständig spielbar. Im eigenen Start ändert sich nichts.
- Kurzbeschreibung `game.description` (DE/EN) für die Spielkarte der Plattform und
  aktuelle `platform.css`-Kopie (Willkommen, Verwaltung, Sprachwahl ohne Konto).
- Alle Seiten liegen jetzt unter `/starfare` (Lobby `/starfare`, Karte
  `/starfare/map/<id>`, Archiv und Statistik unter `/starfare/archive` und
  `/starfare/statistics`); `/` leitet zur Lobby weiter. Alte Lesezeichen auf eine
  Karte oder das Archiv sind damit ungültig. Vorbereitung für den Betrieb auf der
  gemeinsamen Spieleplattform.
- Das Spielartefakt beschreibt sich selbst (`games/starfare/game.properties`);
  Texte liegen unter `games/starfare/i18n`. Ein einbettender Host braucht keine
  Starfare-Einstellungen mehr in seiner Konfiguration.

- Die Lobby lädt entladene Einzelspieler-Partien nicht mehr vollständig aus der
  Datenbank nach, nur um sie aufzulisten. Sie zeigt die beim Entladen gemerkte
  Zusammenfassung; bisher hielt jeder Lobby-Besuch alle diese Partien wieder im Speicher.
- Die Zeitmessung zählt SQL-Abfragen je Anfrage statt über die globale
  Hibernate-Statistik, in die gleichzeitige Anfragen und Hintergrundläufe einflossen.
  Neu gemeldet werden auch schnelle Anfragen mit vielen (ab 20) oder wiederholten
  Abfragen (dieselbe ab dreimal), mit den häufigsten Abfragen im Log.
- Mit der Systemeinstellung „Bewegung reduzieren“ entfallen das Einblenden der
  Berichtskarten, das Pulsieren in der Schlachtwiedergabe und die Übergänge an den
  Sternsystemen.
- Code-Aufräumrunde nach Sonar: komplexe Methoden zerlegt (Produktionsvorgaben,
  Bergbauplanung, Karte, Routenleiste, Flottenversand), doppelte Literale als
  Konstanten, Tests auf AssertJ-Idiome, nachgetragene CSS-Regeln in ihre Selektoren
  zusammengelegt (Darstellung unverändert, per Screenshot-Vergleich geprüft).
  SpotBugs-Befunde erscheinen jetzt unter „Code scanning“ auf GitHub.

## 0.2.3 - 2026-11-02

- Der Spielassistent sieht jetzt wie ein Spiel aus: Regelvarianten erscheinen als
  Bildkarten, das Setup zeigt die Sternenverteilung live, Abschnitte tragen
  Embleme. Die Zusammenfassung zeigt Name, Variante und Kennzahlen im Kopf,
  darunter kompakte Zeilen mit Symbolen und einen Sprung zum jeweiligen Schritt.
- Kartenpanel aufgeräumt: Rückgängig, Versand, Reserven und Bündnisse sind
  Symbolknöpfe mit Tooltip in einer Reihe, die Beschriftung „Ansicht“ entfällt.
  Die Kennzahlen eines Systems sind kompakte Symbole und schließen bündig mit
  der Produktionsbox ab.
- Der Reichweitenkreis endet vor jedem System, das nicht direkt erreichbar ist.
  Maßgeblich bleibt die Markierung am Ziel: Der Kreis kann dadurch kleiner
  ausfallen als die tatsächliche Reichweite.
- Flottenkästchen tauschen bei Aktionen nicht mehr die Plätze und stehen auch
  auf gemeinsam genutzten Routen an der tatsächlichen Flugposition.
- Angesehene Schlachten tragen auf der Karte keinen Marker mehr und bleiben im
  Bericht zum Wiederansehen. Ungesehene Schlachten sind rot mit gekreuzten
  Schwertern, das gelbe „!“ steht nur noch für Engpässe der Wirtschaft. Ein solcher
  Engpass wird auch auf einem System mit angesehener Schlacht angezeigt.
- Forschung steht als Schaltfläche mit Stufe und Budget im Kopf der Karte.
- Ein gedämpftes Kästchen am Systemrand zeigt freie Schiffe, sobald es mehr sind,
  als das System pro Runde baut. Eine höhere Reserve oder geplante Flüge lassen es
  verschwinden.
- Das Produktionsbundle enthält keine Altlasten früherer Builds mehr und schrumpft
  von rund 26 MB auf rund 1 MB.

## 0.2.2 - 2026-11-02

- Der Health-Endpunkt unterscheidet jetzt `UP`, `DEGRADED` und `DOWN`: Ein nicht
  erreichbarer Mailserver meldet `DEGRADED` mit HTTP 200, das Spiel gilt nicht als
  ausgefallen. Öffentlich steht nur der Stand, Einzelheiten sieht der Administrator.
  Der Mailserver wird höchstens alle 3 Stunden geprüft, echter Versand zählt mit.
  Mailverbindungen brechen nach 10 Sekunden ab, statt unbegrenzt zu warten.
- Abhängigkeiten aktualisiert: Spring Boot 4.1.2, Vaadin 25.3.4, Identity 1.6.1,
  Spring Modulith 2.1.2, Error Prone 2.52.0, NullAway 0.14.3, SpotBugs 4.10.5,
  JaCoCo 0.8.16. Spring Boot 4.1.2 schließt veröffentlichte Sicherheitslücken in
  Tomcat und Jackson. OpenRewrite bleibt auf 7.39.0, weil die benötigte
  `rewrite-bom` 8.91.0 weiterhin nicht in Maven Central liegt.
- Neun Karten- und Flottentests laufen jetzt mit Vaadin Browserless bei jedem
  Build: Versand, Sammelbefehle, Revanche, Logistik, Stationsreisen und Bündniskämpfe.
  Zwei vollständige Selenium-Szenarien entfallen; Navigation und Bündniskämpfe
  behalten gezielte Browserprüfungen für Geometrie und JavaScript-Wiedergabe.
- Sieben Wirtschaftsabläufe laufen jetzt mit Vaadin Browserless bei jedem Build:
  Ausbau, Kolonien, Metall, Recycling, Produktionsplanung, Neuanlage und Forschung.
  Sechs vollständige Selenium-Szenarien entfallen. Handy-Forschung sowie Prüfungen
  von Wirtschaftslayout, Mond-Trefferfläche und Tastaturbedienung bleiben im Browser.

- Nach dem Anlegen einer Einzelspielerpartie öffnet sich die Karte zuverlässig,
  auch wenn sich der Wizard beim Schließen sofort von der Oberfläche löst.
- Browserless-Grundlage im normalen Testlauf: Kartenversand, Rundenaktualisierung,
  Kontowechsel und Wizard-Weiterleitung. Ein separater Browser-Rundgang sichert
  Layout und Prüfbilder bei 1366/1024 Pixeln; Smoke und Rundgang prüfen Konsolenfehler.
- Ein SVG-Favicon verhindert die bisherige fehlgeschlagene Browseranfrage nach
  einem nicht vorhandenen Symbol.

- Die eigenständige Rundenansicht unter der Adresse `round/<Spiel>` entfällt,
  sie war in der Oberfläche nirgends mehr verlinkt. Rundenbericht und
  Schlachtwiedergabe bleiben in der Kartenansicht verfügbar.

## 0.2.1 - 2026-10-29

- Neue Partien verwenden ausschließlich die aktuelle Regelfassung ihrer Variante:
  Spaceward 1.3.0-r1 mit Metallförderung, Recycling, Industrieausbau und
  Schiffsforschung, ohne Vorschauprofil oder Versionsauswahl. Vorlagen und
  Revanchen übernehmen die aktuelle Fassung; gespeicherte Partien behalten
  ihre bisherigen Regeln.

- Individuelle, reproduzierbare Modellradien im Systembild, ausdrücklich als
  fiktive Werte gekennzeichnet; gemeinsame Planet-/Mondskala und separate Sternskala.
- Freiwillige dauerhafte Förderautomatik mit globaler Vorschau und einstellbarem
  Zielvorrat. Einstellungen und Schutzvorgaben bleiben über Neustarts erhalten;
  manuelle Produktionsänderungen nehmen ein System aus der Automatik. Bestehende
  Wirtschaftsregeln, Forschungsbudgets und geschützte Verlegungen bleiben erhalten.

- Karten-Navigation mit denselben sieben Ansichten auch bei schmaler Seitenleiste;
  Sammelbefehle und Rücknahme direkt an der Seitenleiste. Partiechat direkt im
  Kartenkopf, mit erhaltenem Nachrichtenentwurf beim Schließen und ohne
  automatisches Ans-Ende-Springen beim Lesen älterer Nachrichten.
- Archiv mit hervorgehobenem Ansehen und nachgeordneter Vorlage/Revanche;
  Suchfilter und zuletzt angesehene Partie bleiben bei Rückkehr erhalten.
  Zehn-Sitz-Zusammenfassungen bieten zunächst offene, einklappbare Sitzdetails.

- Gemeinsame Schiffsforschung in der aktuellen Regelfassung 1.3.0-r1: reichsweiter Industrieanteil, steigende Stufenkosten und
  stärkere Neubauten ab der folgenden Runde. Baustufen bleiben bei Versand,
  Verlegung, Recycling, Rückkehr und Kampf erhalten. Gemischte Bestände werden
  angezeigt; Abschlüsse erscheinen im Bericht. Geschwindigkeit, Reichweite,
  Metallkosten und bestehende W6-/W7-Partien behalten ihre Regeln.

- Schiffsforschung auf Desktop und Handy geprüft; Forschungsreserve wird nicht mehr
  als Arbeitskräftemangel gemeldet. Eroberungen gleichen die Produktionspläne
  beider Reiche vor der neuen Schiffbauvorschau ab.
- Forschungsbudgets in 768 kontrollierten Partien verglichen; normales KI-Budget
  auf 10 Prozent abgestimmt, Rush bleibt bei 5 Prozent. Details und Grenzen in
  `docs/RESEARCH-EXPERIMENTS.md`.

## 0.2.0 - 2026-10-18

- **Eigene Handy-Oberfläche.** Handys erhalten eine eigene Kartenansicht: kompakter
  Kopf, Karte im Vollbild mit Zoomknöpfen und Zwei-Finger-Zoom, die Seitenleiste als
  Vollbild-Übersicht und eine Leiste zum Wechseln zwischen Karte und Übersicht. Die
  Zielwahl zeigt einen Hinweis mit Abbrechen. Der Server wählt die Oberfläche je
  Sitzung anhand des Browsers und baut nur diese; Tablets und Desktop-Rechner bleiben
  bei der gewohnten Ansicht. `?layout=mobile` bzw. `?layout=desktop` legt sie fest.
- Lobby, Spielassistent, Archiv, Statistik und alle Dialoge sind auf dem Handy
  bedienbar: nichts ragt seitlich heraus, Dialoge füllen den Bildschirm, ihre Knöpfe
  bleiben unten erreichbar, Bedienelemente sind mindestens 44 px hoch. Desktops laden
  das Handy-Stylesheet nicht.
- „Eigenes Reich“ zoomt bei wenigen Systemen nicht mehr übermäßig heran; die
  Startansicht zentriert das Heimatsystem auch, wenn die Karte erst verzögert ihre
  Größe erhält.
- Neuer Browsertest `@mobile`: Ein emuliertes iPhone bedient Assistent und Karte per
  Fingertipp, daneben prüft eine Desktop-Sitzung die gewohnte Ansicht.

## 0.1.1 - 2026-10-16

- Die Variantenkarte von SectorForces erklärt das Spiel ohne Vorwissen und nennt
  das Vorbild, das C64-Spiel Sector Forces aus den 80er-Jahren.
- Der Ergebnisdialog bietet **Bis zur vollständigen Eroberung weiterspielen** nicht
  mehr an, wenn ohnehin schon alle Systeme gefordert waren, etwa beim zweiten
  Spielende nach einer Fortsetzung. Auch der Server lehnt diese Fortsetzung ab.
- Browsertests warten auf den tatsächlichen Zustand statt auf vorab erfüllte
  Bedingungen: Klicks und Eingaben wiederholen sich, wenn Vaadin zwischendurch neu
  rendert. Das beseitigt sporadische Fehlschläge im CI.

## 0.1.0 - 2026-10-16

- Erste öffentliche Version auf hetzner-zs: Release-Pushes bauen ein Docker-Image
  (GHCR) und stoßen den Deploy in Coolify an; ein Rollback auf eine frühere Version
  ist per Workflow-Aufruf möglich.
- Registrierungs- und Passwort-Mails werden tatsächlich verschickt (Mail-Starter fehlte).
- Langsame Seiten, Bedienaktionen, Rundenauswertungen und SQL-Abfragen erscheinen
  als Warnung im Log (Schwellen `starfare.timing.slow-page-ms`, `slow-round-ms`).
- README und Anleitung nennen den Spielserver, den Plan mit SectorForces,
  Spaceward und einer Variante nach Master of Orion II sowie den Weg zu Issues.
  Neue Issue-Formulare für Fehlerberichte und Ideen; Sicherheitslücken werden
  laut `SECURITY.md` vertraulich gemeldet.

- Spaceward-Neuanlage auf W6 und W7 beschränkt; W7 ist die Vorgabe. Der Assistent
  bietet eine eigene Regelfassungswahl. SectorForces bleibt als eigene Variante
  vorausgewählt. Die Auswahl gilt auch für Vorlagen und Revanchen, ohne besonderes
  Startprofil. Ältere Spielstände bleiben unter ihren bisherigen Regeln fortsetzbar.

- Neuer interner Wirtschaftskandidat 1.2.0-w7 mit günstigen ersten Industriestufen:
  min(2C, max(2, aufrunden(C²/10))) Punkte je Stufe, ab C=20 der bisherige Preis.
  Mehrere Stufen pro Runde sind möglich; neue Kapazität wirkt ab der Folgerunde.
  Vorschau und KI verwenden den gespeicherten Regelpreis. Ältere Partien behalten
  ihre Kosten. Neue Partien bieten W7 und W6; ältere Fassungen bleiben fortsetzbar.

- Gezielter Wirtschaftsvergleich mit weiteren 1.280 Partien: frühe Einzelstufen,
  moderater und später Ausbau bei festen militärischen Heuristiken, Kosten- und
  Eroberungsvarianten sowie unabhängige Kontrollkarten. Günstige kleine Ausbauten
  wurden als W7 übernommen; übrige Modelle bleiben im Testprüfstand.

- Strategieprüfstand für W4/W5/W6 erweitert und 2.240 Studienpartien verglichen:
  Starker Dauerausbau verliert alle entschiedenen breiten W6-Zweikämpfe;
  zurückhaltender und adaptiver Ausbau gewinnen viele Partien. Ergebnisse,
  offene Verläufe und Grenzen stehen in `docs/ECONOMY-EXPERIMENTS.md`.

- Neuer interner Wirtschaftskandidat 1.2.0-w6: Bei Metallknappheit erhalten
  Werften gleiche Anteile bis zu ihrem Bedarf; übrige Anteile werden erneut
  verteilt. Vorschau, Rundenauflösung und historische Ansichten rechnen identisch.
  Gespeicherte W5-Partien behalten ihre proportionale Verteilung. Das Profil
  W6 bleibt neben W7 wählbar; ältere Fassungen bleiben fortsetzbar.

- Neue Spaceward-Partieoption **KI darf Bündnisse eingehen** (Vorgabe an). Ist sie
  aus, treten KI-Imperien keinen neuen Bündnissen bei; bestehende Verträge nach
  einer KI-Übernahme bleiben gültig. Ältere Partien behalten die bisherige Teilnahme.

- Der Spaceward-Abschlussbericht zeigt gebaute Industrie und, mit Metall,
  gefördertes und verbrauchtes Metall der Partie. Die Werte werden je Runde am
  Produktionsschritt erfasst; ältere Partien zeigen „nicht erfasst“, späterer
  Beginn oder Lücken werden gekennzeichnet.

- Spaceward-Zielwahl zeigt die direkte Reichweite als Kreis um die Quelle und
  kennzeichnet Ziele als direkt erreichbar, nur über Stationen oder ohne Weg. Der
  Versanddialog nennt die Entfernung als Anteil der direkten Reichweite.

- Spaceward-Produktion gilt sofort: Zahlenfelder, Plus/Minus und Vorgaben speichern
  direkt, der Schieberegler beim Loslassen. Entwürfe, Übernehmen, Neu prüfen und
  die Warnung vor offenen Entwürfen entfallen. Jede Funktion zeigt Punkte und Ertrag
  je Runde (z. B. „2 Punkte × 3 → 6 Metall/Runde“), Metallmangel und erschöpfte
  Vorkommen direkt am Ergebnis. Der Kopf zeigt Metallbarren, Vorrat und Änderung
  zur nächsten Runde; die Logistik nennt Fördereignung und Restvorkommen.
- 1.2.0-w5 kürzt bei weniger Schiffbau die ausgehenden Verlegungen des Systems
  anteilig und meldet jede Änderung; ältere Fassungen bleiben unverändert.

- Interner Spaceward-Wirtschaftskandidat 1.2.0-w5 für das lokale Erprobungsprofil:
  Bevölkerung wächst weiter und beschreibt die Kolonie, begrenzt aber nicht mehr
  die nutzbare Industrie. Gespeicherte Partien behalten ihre Regelversion; 1.2.0-w4
  bleibt fortsetzbar, neue Erprobungspartien verwenden 1.2.0-w5.

- Systeme bleiben zwischen Flottenstrecken erreichbar, auch bei kreuzenden Routen
  und wechselndem Zoom. Flottenlinien erhalten eine eigene Farbe; Ereignis- und
  Engpasszeichen sind größer und getrennt bedienbar.
- Kampfseiten folgen Angriff und Verteidigung. Mannschaftstexte vermitteln die
  momentane Gefechtsverfassung; Zahlen sind unter Kampfdetails erreichbar.
- Verlegungen zeigen Bearbeiten direkt neben Aufheben, auch in der Systemsicht.
  Die Schnellmenge Alle außer Produktion berücksichtigt eingehende und ausgehende
  Verlegungen zusätzlich zur Reserve.

- Metallförderung zeigt eine vereinfachte Industriezeche mit großem Seilrad
  und niedriger Werkhalle.

- Grafische Routenstreifen in Flottenversand, Flottenkarten und Details,
  sichtbare Stationszustände und passende Einstiege bei Blockaden.
  Bündniskarten zeigen Entscheidungen zuerst, erhalten Fristeingaben bei
  Aktualisierungen und bestätigen Kündigungen samt Austrittsrunde und Folgen.

- Produktionsbedienung direkt am System: fünf Schnellvorgaben, unterscheidbare
  Werft-, Förder- und Ausbauaktionen, lokale Entwürfe über Systemwechsel,
  Konfliktprüfung vor Übernahme und bewusste Abgabe mit gespeicherten Plänen.
  Reichshinweise öffnen gefilterte Logistiklisten; offene Schlachten bleiben geschützt.

- Systemgrafik mit angeschnittenen Sternen, festen Typgrößen und unterscheidbaren
  Planetenoberflächen. Gespeicherte Spaceward-Bahnen zeigen echte Entfernungsunterschiede;
  besiedelte Monde sind sichtbar und öffnen direkt die Kolonieangaben.

- Spielanzeigen verwenden Imperiumsnamen; der Kartenkopf zeigt das eigene Reich.
  Produktion zeigt eingehende Verlegungen unmittelbar in Klammern. Verlegungen
  lassen sich direkt an Quelle und Ziel zurücknehmen, auch in Spaceward.

- Wiederholbare UI-Abnahmeprüfung für Zusammenfassungskorrektur, Lobby und
  Archiv, Fristen sowie Schlachten; aktuelle Ansichtsaufnahmen für die
  persönliche Prüfung werden unter `build/ui-acceptance/` erzeugt.
- Leere Nebenaktionen in Lobbykarten ausblenden; das Ergebnisraster nutzt
  die Dialogbreite, damit Kennzahlen auf Desktop nebeneinander erscheinen.

- Flottenversand trennt Abbruch und Erfolg, behält die Quelle und vermeidet
  erzwungene Reiterwechsel. Warten und zulässiges Auflösen sind direkt an
  Flottenkarten und Details erreichbar; Reisedaten und Ankunftsliste aufklappbar.

- Systemdetails zeigen versendbare Schiffe und Versandaktionen vor dem Bild.
  Logistik, Reserve und Kolonieangaben sowie Hintergrundwissen sind lokal
  aufklappbar; geöffnete Bereiche bleiben bei Systemwechseln erhalten.

- Rechtsklick auf ein eigenes Kartensystem beginnt die Flottenzielwahl direkt,
  ohne ein zusätzliches Menü mit nur einem Eintrag.

- Verlegungsvorschau auch bei großen Garnisonen ohne Ganzzahlüberlauf anzeigen;
  Wiederherstellung und Wirtschaftsplanung vereinfachen. Vollständige lokale
  SonarQube-Prüfung mit null offenen Befunden bestanden.

- Wirtschaftsabnahme-Kandidat Spaceward 1.2.0-w4 über das lokale Startprofil
  economy-preview anlegen; normale Neuanlage bleibt bei 1.1.0. Stummer
  Wirtschaftsprüfstand mit gepaarten Karten, Förder- und Recyclingvergleichen,
  strenger Befehlsprüfung, Metallbilanz und reproduzierbaren Ergebnisdateien.
  Alle KI-Profile berücksichtigen belegte Förderkapazität bei Ausbau und Reserve.

- Interner Wirtschafts-Zwischenstand Spaceward 1.2.0-w3: bearbeitbare
  Recyclingbefehle, 70 Prozent Metallrückgewinnung mit gespeicherten Zehntelresten,
  gemeinsame Schiffsreservierung und vollständige Metallbilanz einschließlich
  Kampfverlusten. Alle acht KI-Profile nutzen legales Recycling im Hinterland.
  Bestehende Regelfassungen behalten ihr Verhalten.

- Interner Wirtschafts-Zwischenstand Spaceward 1.2.0-w2: endliche örtliche
  Metallvorkommen, reichsweite Vorräte und gemeinsame Kapazitätszuweisung für
  Förderung, Ausbau und Schiffbau. Ein Schiff kostet 1 Metall. Bei Knappheit
  erfolgt die Zuteilung proportional; Verlegungen verwenden nur tatsächlich
  vorhandene Schiffe. Systemsicht, Speicherung und historische Nachbetrachtung
  erhalten die Ressourcen. W1 und ältere Partien behalten ihre Regeln;
  öffentliche Neuanlage bleibt bei 1.1.0. Recycling folgt gesondert.
- Spielstandkopien erhalten Rundenuhr, Abgaben und Flottenzähler vollständig,
  ohne beim Kopieren künstliche Rundenwechsel auszuführen.

- Interner Wirtschafts-Zwischenstand Spaceward 1.2.0-w1: gespeicherte
  Systemqualität und Koloniewachstum begrenzen die nutzbare Industrie.
  Systemsicht, KI, Speicherung und historische Nachbetrachtung berücksichtigen
  Bevölkerung und Arbeitskräfte. Öffentliche Neuanlage bleibt bei 1.1.0;
  bestehende Partien behalten ihre Regeln. Metallförderung und Recycling folgen
  in gesonderten Regelversionen.

- KI-Strategie je Sitz im Partiewizard wählen: Basisstrategie, früher Angriff,
  Expansion, Kräftekonzentration und Verteidigung; Spaceward ergänzt drei
  Industrieprofile. Auswahl und Zusammenfassung bleiben beim Zurückgehen
  erhalten. Speicherung, Vorlagen, Revanchen und Sitzübergaben übernehmen das
  Profil; alte Spielstände verwenden weiterhin die Basisstrategie. Spiel-KI
  und Strategieprüfstand teilen die alternative Planungslogik.

- Strategieprüfstand erweitert: zurückhaltender und situationsabhängiger
  Industrieausbau sowie ausdrücklich wählbare Profile für Zweikämpfe und
  Mehrspielerläufe. Die bisherigen Versuchsvorgaben bleiben erhalten;
  Spielregeln bleiben unverändert; die Profile sind nun auch je KI-Sitz wählbar.

- Abhängigkeiten aktualisiert: Vaadin 25.3.1, Identity 1.5.0, Cucumber 8.0.4,
  WebDriverManager 6.4.0, NullAway 0.14.2 und Plugin 3.3.0, SpotBugs-Plugin
  6.5.12, ArchUnit 1.5.1 sowie Gradle 9.8.0 und JUnit-Berichtsaction 6.6.0.
  Spring-Boot-verwaltete Versionen bleiben unverändert. OpenRewrite bleibt
  wegen der nicht auflösbaren neueren Rezept-BOM auf der geprüften Paarung.

- Spaceward freigegeben (M7): Die Variante ist im Dialog „Neues Spiel“ mit
  Regelversion 1.1.0 wählbar; SectorForces bleibt vorausgewählt. Die
  Angriffsreihenfolge entfällt dort, da alle Seiten gleichzeitig kämpfen. Wirtschaft,
  Navigation, Bündnisse und gemeinsame Schlachten stehen damit in neuen Partien,
  Vorlagen und Revanchen bereit. Bestehende Partien behalten ihre Regeln;
  gespeicherte 1.0.0-Partien laufen unverändert weiter.
- Stabilerer Browser-Abnahmetest für Bündnis-Gefechte: Nach dem Neuladen wartet
  er auf den gerenderten Bericht.

- Spaceward-Regelversion 1.1.0: erlaubte Bündnisse und Bündnissiege
  getrennt je Partie wählen, mit eigenen Einzel- und Bündnisschwellen.
  Gemeinsame Ergebnisse nennen alle Sieger in Bericht, Dialog, Archiv und
  Statistik; jeder Sieger darf bis zur vollständigen Eroberung fortsetzen.
  Bestehende Partien behalten ihre Regelversion und Vorgaben. Der KI-Prüfstand
  vergleicht identische Karten mit ein- und ausgeschaltetem Bündnissieg.

- Reproduzierbarer KI-Strategieprüfstand für SectorForces und Spaceward:
  Strategiematrix mit getauschten Startplätzen, Mehrspieler- und Bündnisversuche
  sowie Vergleich einzelner Entscheidungen ab identischem Spielstand.
  Auswertungen enthalten Rundenverläufe, Befehle, Zufallsstartwerte und getrennte
  Siege, Unentschieden und Rundenlimits. Läuft ohne Oberfläche oder Datenbank;
  die reguläre KI und bestehende Spielregeln bleiben erhalten.

- Spaceward-Kampf (M6): Bündnisgruppen kämpfen gemeinsam, mehrere
  feindliche Seiten gleichzeitig. Verluste und Abschüsse werden anteilig
  ohne Doppelzählung verteilt. Eroberungsziele lassen sich vor dem Versand
  benennen; verbündete Überlebende behalten ihren Eigentümer. Berichte und
  Wiedergabe zeigen alle beteiligten Seiten. Persönliche Bestätigungen,
  Neustart, Archiv und Statistik bleiben erhalten. SectorForces behält
  seine Kampfregeln.

- Spaceward-Diplomatie (M5): gemeinsame Bündnisgruppen mit einstimmiger
  Aufnahme und vereinbarter Kündigungsfrist. Austritte zeigen eine feste
  Wirksamkeitsrunde; Friständerungen brauchen Zustimmung aller und ändern
  laufende Kündigungen nicht. Verbündete Zwischenstationen erhalten getrennte
  Flotten; Austritte führen automatisch zur geschützten Heimreise. Verträge
  und Reisephasen überstehen Neustarts. SectorForces bleibt unverändert.

- Spaceward-Navigation (M4): feste Reichweite je Partie, automatische
  Routen über eigene Zwischenstationen und eine volle Runde zum Auftanken.
  Reiseplan und Etappen bleiben nach Neuladen erhalten; Stationsverlust
  unterbricht die Weiterreise. Verlegungen und KI beachten dieselbe Wegprüfung.
  Vorschau, Flottendetails und Kartenlinien zeigen die tatsächlichen Etappen.
  SectorForces behält unbegrenzte direkte Flüge.

- Spaceward-Wirtschaft (M3): Produktion zwischen Schiffbau und Ausbau
  verteilen; Ausbaukosten 2 × aktuelle Kapazität, Maximum 50. Teilfortschritt
  und Kapazität bleiben bei Eroberung erhalten. Industriegebäude, Verteilung,
  Fortschritt und Lieferengpässe in der Systemsicht; Speicherung und historische
  Rundenstände ergänzt. SectorForces behält seine Regeln.

- Spielvarianten und feste Regelversionen: SectorForces bezeichnet die bisherigen
  Classic-Regeln (1.0.0). Der Katalog ist für weitere Varianten erweiterbar.
  Lobby, Beitritt, Verwaltung, Archiv und Statistik zeigen die Regeln;
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
