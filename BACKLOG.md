# Backlog

Offene Punkte für Starfare. Erledigtes wird hier entfernt und im
`CHANGELOG.md` festgehalten; Architektur-Hintergrund steht in
`docs/ARCHITECTURE.md`.

## Release

- [ ] **Erstes Release 0.1.0** — `appVersion` in `gradle.properties` auf
      `0.1.0` (ohne `-SNAPSHOT`), `## Unreleased` in `CHANGELOG.md` zu
      `## 0.1.0 - <Datum>`, Push auf `main`; der Workflow taggt `v0.1.0`,
      erstellt das GitHub-Release mit `starfare.jar` und bumpt auf
      `0.1.1-SNAPSHOT`. Vorher ggf. Secret `RELEASE_TOKEN` setzen, falls
      „Workflow permissions" dem `GITHUB_TOKEN` das Release verwehrt.

## Kleine Features

## Future Work

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
- [ ] **Zuschauer-Modus mit Perspektivwahl** statt pauschalem
      God-Mode.
