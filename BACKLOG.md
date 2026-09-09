# Backlog

Offene Punkte für Starfare. Erledigtes wird hier entfernt und im
`CHANGELOG.md` festgehalten; Architektur-Hintergrund steht in
`docs/ARCHITECTURE.md`.

## Lobby und Spielverwaltung

Detaillierter Umsetzungsplan: [.agents-docs/LOBBY-PLAN.md](.agents-docs/LOBBY-PLAN.md).
Sichtbarkeit und Archiv sind umgesetzt. Aufräumen folgt erst nach gesicherter Datenaufbewahrung.

- [ ] **Aufräumen alter Partien** — beendete Spiele nach einer Frist aus
      `game_sessions` entfernen. Achtung, Reihenfolge: erst müssen
      Nachbetrachtung und Statistik geklärt sein, sonst löscht man die Daten
      weg, auf denen beide aufbauen.

## Nachbetrachtung beendeter Partien

- [ ] **Kriegsnebel umschaltbar** — in einer beendeten Partie zwischen voller
      Sicht und Nebel wechseln können. Die volle Sicht liefert
      `PlayerViewBuilder` seit dem Aufdecken nach Spielende schon; gebraucht
      wird der Schalter und der Weg zurück auf die gefilterte Sicht.
- [ ] **Perspektive wechseln** — die Partie aus der Sicht jedes Teilnehmers
      ansehen. `viewFor(playerId)` kann das bereits; es fehlt die Auswahl in der
      Oberfläche und eine Regel, ab wann das erlaubt ist (vermutlich erst nach
      Spielende).

## Oberfläche

- [ ] **Navigationsmenü** — Voraussetzung für die drei folgenden Punkte: heute
      liegt alles auf der Kartenseite. Es braucht einen Einstieg, über den
      Unterseiten erreichbar sind, ohne die Karte zu verlassen.
- [ ] **Eigene Ansicht für Produktionsverlegungen** — die eigenen Systeme als
      Knoten, Verlegungen als Kanten, die man zieht. Ersetzt die Bedienung über
      den Sendedialog; auf der Kartenseite kann die Verlegungs-Bedienung dann
      entfallen.
- [ ] **Flotten- und Befehlslisten auf eigene Seiten** — die Darstellung auf der
      Karte reicht für den Überblick; die Tabellen daneben machen die Seite eng.
      Über das Menü erreichbar.
- [ ] **Layout im Spiel-Setup überarbeiten** — der Dialog aus
      `CreateGameWizardDialog` sitzt noch nicht rund: der Dialog ist mit
      `min(1380px, 96vw)` sehr breit, die Abschnitte laufen über
      `FormLayout`-Raster mit fest gesetzten Spaltenbreiten (`13em`/`16em`) und
      die Startproduktions-Zeilen wachsen mit der Spielerzahl. Auf einem
      1366×768-Schirm wird es dadurch eng bzw. unruhig. Gebraucht wird eine
      Aufteilung, die mit wenigen wie mit vielen Spielern gleich gut aussieht.
- [ ] **Bericht und Karte verknüpfen** — Systeme, an denen etwas passiert ist,
      auf der Karte markieren und zwischen Markierung und Rundenbericht
      hin- und herspringen können.

## Future Work

- [ ] **Statistiken** — welche Partien es gab, wer teilgenommen hat, gegen
      wen man wie oft gespielt und wie oft gewonnen hat. Braucht Daten, die
      das Aufräumen alter Partien überleben: entweder eine eigene Tabelle, in
      die eine beendete Partie ihr Ergebnis schreibt, oder die Partien gar
      nicht löschen. Diese Entscheidung fällt am besten, bevor aufgeräumt
      wird.
- [ ] **Nachrichtenverwaltung** — Read-Status, Aufbewahrungsfristen und
      Löschung/Archivierung für persistente Direktnachrichten ergänzen.
- [ ] **Replay** — `TurnReport`-Events + `GameState`-Snapshots →
      Rewind-Slider über beendete Partien.
- [ ] **Spielkonzept „Diplomatie"** — eigenes Epic. In-Game-Chat
      zwischen Partie-Teilnehmern, Bündnisse, Verträge.
- [ ] **Spielkonzept „Wirtschaft"** — Schiffsproduktion eines Systems
      steigerbar.
- [ ] **Spielkonzept „ausgefeiltes System"** — Sonne(n), Planeten,
      Asteroidengürtel; Produktion verteilt.
- [ ] **Spielkonzept „Schiffstyp"** — unterschiedlich schnelle/starke
      Typen.
- [ ] **Spielkonzept „Planet"** — kolonisierbar, ausbaubar.
- [ ] **Spielkonzept „Forschung"** — Schiffe/Bauten/etc. erforschbar.
- [ ] **Zuschauer-Modus mit Perspektivwahl** statt pauschalem God-Mode —
      betrifft die *laufende* Partie, in der ein Zuschauer heute alles sieht.
      Die Perspektivwahl nach Spielende steht oben unter Nachbetrachtung.

## Später: Release und Betrieb

Ziel: ein schlankes, aber rundes Feature-Set öffentlich betreiben.

- [ ] **Betrieb klären** — der Release-Workflow baut nur Tag und GitHub-Release
      (`build.yml` sagt selbst „ohne Deploy"). Für echten Betrieb fehlen
      Hosting, Domain und TLS, eine verwaltete PostgreSQL samt Backup, der
      Mailversand über das `brevo`-Profil mit echtem Absender, sowie ein Blick
      auf Logs und `/actuator/health`. Ohne diese Entscheidung ist „1.0.0"
      nur ein Tag.
- [ ] **Version auf 1.0.0** — `appVersion` in `gradle.properties` (steht auf
      `0.1.0-SNAPSHOT`), `## Unreleased` im `CHANGELOG.md` umbenennen, Push auf
      `main`. Vorher ggf. Secret `RELEASE_TOKEN` setzen, falls „Workflow
      permissions" dem `GITHUB_TOKEN` das Release verwehrt.
- [ ] **Datenschutz-Minimum** — sobald echte Konten entstehen: Impressum bzw.
      Kontakt, Hinweis auf gespeicherte Daten (E-Mail, Anzeigename, Partien)
      und ein Weg, das Konto zu löschen. Betrifft den Identity-Baustein.
