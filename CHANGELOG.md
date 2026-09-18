# Changelog

Alle nennenswerten Änderungen an Starfare. Format nach
[Keep a Changelog](https://keepachangelog.com/de/1.1.0/), Versionierung nach
[SemVer](https://semver.org/lang/de/). Beim Release wird `## Unreleased` in
`## <appVersion> - <Datum>` umbenannt; der Release-Job extrahiert genau diesen
Abschnitt als Release-Body und bricht ohne ihn ab.

## Unreleased

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
- Echtzeit-Timeout: Sitze, die einen Zug nicht abgeben, übernimmt nach
  `starfare.game.inactivity-timeout` (Standard 5 min) die KI. Erlaubt die
  Partie den Wiedereinstieg, kann der Spieler seinen Sitz zurückholen.
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
