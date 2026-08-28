# Changelog

Alle nennenswerten Änderungen an Starfare. Format nach
[Keep a Changelog](https://keepachangelog.com/de/1.1.0/), Versionierung nach
[SemVer](https://semver.org/lang/de/). Der Release-Job (sobald vorhanden)
extrahiert den Abschnitt der aktuellen `version` als Release-Body.

## Unreleased

### Added
- Konten, Login, Registrierung mit E-Mail-Bestätigung und Passwort-Reset
  über den externen Baustein `de.zettsystems:identity-core`/`identity-vaadin`.
- Direktnachrichten werden persistiert (`direct_messages`); Einladungen
  überleben einen Server-Neustart.
- Mailpit im lokalen `docker-compose.yml`; Profil `brevo` für echten
  SMTP-Versand.

### Changed
- Spieler werden überall über die Konto-ID referenziert; der öffentliche
  Spielername ist nur Anzeige (`zs.identity.name-mode: DISPLAY_NAME`).
  Anzeigenamen kommen aus `PlayerDirectory` (kurz gecacht).
- UI arbeitet ausschließlich auf View-Modellen (`PlayerViewState`,
  `GameSummary`); `GameService.snapshot()` entfällt, ArchUnit erzwingt
  `ui` ↛ `domain`.

### Fixed
- Tests liefen mit einer eigenen `application.yaml`, die die
  Haupt-Konfiguration komplett verdeckte (u. a. `open-in-view`,
  Actuator-Exposure); jetzt Profil `test`.
- App-Migrationen laufen im Versionsraum `V2_x`, der Identity-Baustein in
  `V1_x` (`spring.flyway.out-of-order: true`).

### Removed
- Eigene Benutzerverwaltung (`user_accounts`, `RegisterView`, `LoginView`).
