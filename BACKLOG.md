# Backlog

Offene Punkte für Starfare. Erledigtes wird hier entfernt und im
`CHANGELOG.md` festgehalten; Architektur-Hintergrund steht in
`docs/ARCHITECTURE.md`.

## Lobby und Spielverwaltung

Detaillierter Umsetzungsplan: [.agents-docs/LOBBY-PLAN.md](.agents-docs/LOBBY-PLAN.md).
Sichtbarkeit, Archiv und die optionale Bereinigung nach gesicherter Datenaufbewahrung sind umgesetzt.

## Future Work

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
