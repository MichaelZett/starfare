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

## Oberfläche

- [ ] **Grafische Produktionsverlegungen** — die eigenen Systeme als
      Knoten, Verlegungen als Kanten, die man zieht. Ersetzt die Bedienung über
      den Sendedialog; die Karten-Seitenleiste zeigt und verwaltet bis dahin die
      bestehenden Verlegungen als Tabelle.
- [ ] **Bericht und Karte verknüpfen** — Systeme, an denen etwas passiert ist,
      auf der Karte markieren und zwischen Markierung und Rundenbericht
      hin- und herspringen können.
- [ ] **Persönliche Reiter-Voreinstellung** — konfigurierbar machen, ob neue
      Befehle automatisch die Befehle und eine neue Runde den Bericht öffnen.

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
      Die Perspektivwahl nach Spielende ist bereits umgesetzt.

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
