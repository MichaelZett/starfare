# Changelog

Alle nennenswerten Änderungen an Starfare. Format nach
[Keep a Changelog](https://keepachangelog.com/de/1.1.0/), Versionierung nach
[SemVer](https://semver.org/lang/de/). Beim Release wird `## Unreleased` in
`## <appVersion> - <Datum>` umbenannt; der Release-Job extrahiert genau diesen
Abschnitt als Release-Body und bricht ohne ihn ab.

## Unreleased

### Added
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
- Karte: für die laufende Runde geplante Flüge erscheinen als gestrichelte
  Bahn; Zoom und Ausschnitt überleben den Rundenwechsel.
- Produktionsverlegungen haben eine feste Größe statt „die ganze Produktion".
  Ein System darf insgesamt höchstens seine eigene Produktion plus alles,
  was per Verlegung hereinkommt, weiterleiten; mehrere Ziele teilen sich
  dieses Budget. Die Karte zeigt bei belegten Systemen die noch freie
  Produktion in Klammern, der Rest bleibt als Garnison liegen.
- Nach Spielende deckt die Karte alle Systeme auf.

### Changed
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
- Eigene Benutzerverwaltung (`user_accounts`, `RegisterView`, `LoginView`).
