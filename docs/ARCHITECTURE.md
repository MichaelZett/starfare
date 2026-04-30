# Starfare Architecture

Deep dive into the architecture. The README has the user-facing overview;
this document covers the structural decisions.

## Goals

- Domain-oriented packages with a thin technical sub-layer per module
  (`ui | application | domain | values | config`).
- Clear flow: UI → `GameService` → services → `GameState`; no feedback
  loop back into the UI.
- Thread safety centralised in the repository (`GameStateRepository`,
  `ReentrantReadWriteLock`).
- Module boundaries verified by Spring Modulith (`ModulithTest`).

## Package layout

Base package: `de.zettsystems.starfare.<domain>.<technical>`.

- `game` — Core. `application` (`GameService`, `GameStateRepository`,
  `PlayerViewBuilder`, `GameRegistry`, `AutoplayRunner`, `Broadcaster`),
  `domain` (`GameState`), `values` (records for setup/views:
  `StarSystem`, `Fleet`, `PlayerViewState`, `VisibleSystem`, `FleetView`,
  `GameSetup`, `GameConfig`, `PlannedOrder`, `StandingOrderView`, …),
  `ui` (Vaadin views: `LobbyView`, `MainView`, `RoundView`, panels,
  dialogs, `UiMapper`, `UiTexts`).
- `turn` — `TurnEngine` (turn pipeline).
- `combat` — Combat resolution (`CombatService`, `CombatResolver`).
- `fleet` — Fleet commands and validation (`FleetService`,
  `FleetOrder`).
- `ai` — AI player decisions (`AiService`).
- `report` — Round reports (`TurnReport`, `TurnEvent`).
- `auth` — User management plus Spring Security integration.
- `i18n` — `StarfareI18NProvider` (Vaadin `I18NProvider`),
  `I18n.t(...)` facade, `LocaleServiceInitListener` (restores the UI
  locale from the session).
- `social` — Presence, friendship, direct messages, invitations.
- `style` — `CssProperties`, `HtmlAttributes` (central constants for
  Vaadin styling and attributes).

Every top-level module has a `package-info.java` with
`@ApplicationModule(type = OPEN)`, so that sub-layers are visible to
other modules. New modules need the same declaration.

## Dependencies

- `game.ui` → `GameService` → services
  (`turn/combat/fleet/ai/report`) → `GameState`.
- `game.application.GameStateRepository` owns the only `GameState` and
  all locks; access exclusively through `readState(fn)` /
  `writeState(fn)`.
- Services operate on the `GameState` instance passed in, never hold
  references between calls and never reach for the repository
  themselves. `GameService` is the only orchestrator.
- The UI never mutates `GameState` directly; it consumes immutable view
  models from `game.values`, built by `GameService.viewFor(...)`
  including the fog-of-war filter via `state.intel()`.

## Turn pipeline (`TurnEngine.advanceTurn`)

Fixed sequence, fully inside one `writeState`:

1. Production on owned, non-neutral systems.
2. Apply `waitThisTurn` (arrival +1, clear flag).
3. Group arrivals by `toSystemId` + owner; reinforce the owner first,
   then resolve attackers strongest-first via `CombatService`.
4. `AiService.doAiTurns`.
5. Victory check (> 50% of systems).
6. `state.nextTurn()`.

If `state.gameOver()` is true, `advanceTurn` is a no-op.

## Lifecycle and lobby

`GameState` carries `active` (game exists), `started` (turns running)
and `joinedHumanPlayerIds`. `LobbyView` walks
`newGame → joinGame → canStartGame → startGame → MainView`.
`PlayerContext` holds the player ID in the Vaadin session.
`MainView.onAttach` redirects back to the lobby when no game is
running.

## Configuration and setup

- Tunables: constants in `game.values.GameConfig`.
- Spring configs: `@ConfigurationProperties` (records or classes), never
  `@Value`. `@Configuration` classes live in the `<module>/config/`
  package (they fit neither `ui` nor `application/domain/values`).
- New game: wizard in `LobbyView` → `GameSetup` (`game.values`);
  `GameSetup.normalized()` clamps and defaults inputs.
  `GameStateRepository.newGame(GameSetup)` is the only entry point (a
  legacy overload just builds a default setup).

## i18n

- Bundles under `src/main/resources/vaadin-i18n/`:
  `translations.properties` (default, DE) and
  `translations_en.properties` (EN).
- `de.zettsystems.starfare.i18n.StarfareI18NProvider` is a Spring
  `@Component` and implements Vaadin's `I18NProvider`;
  `getProvidedLocales()` returns `[GERMAN, ENGLISH]`.
- UI code uses **keys** from `game.ui.UiTexts` exclusively and resolves
  via `I18n.t(UiTexts.X, args...)` (a wrapper around
  `UI.getCurrent().getTranslation(...)`, with a fallback to the
  provider for non-UI contexts such as plain unit tests).
- Plurals and variants are modelled as `{0}` parameters
  (`map.duration.singular/plural`, `map.fleetBadge.etaIn*`).
- Layer-clean: `PlayerViewBuilder` (application) emits **keys** instead
  of strings (`map.orderType.send/wait/disband`); the UI translates at
  the rendering point.
- Language switcher: `game.ui.LanguageSwitcher` ComboBox in the lobby
  toolbar and the map header; stores the `Locale` in the
  `VaadinSession`, calls `ui.setLocale(...)` and reloads the page.
- `i18n.LocaleServiceInitListener` (Vaadin `ServiceInitListener`)
  re-applies the session locale on every UI init, so the choice
  survives navigation and reload.

## Module `social`

`de.zettsystems.starfare.social` covers presence, friendship, direct
messages and invitations. Sub-layers as usual (`values`, `domain`,
`application`, `ui`).

### Data model

- `friendships` is canonicalised on `(user_a, user_b)` with
  `user_a < user_b` — one row per pair. Columns: `status`
  (`PENDING | ACCEPTED | BLOCKED`), `requested_by` (the requester for
  `PENDING`, the blocker for `BLOCKED`), `created_at`, `updated_at`.
- `user_preferences` stores the per-user `visibility`
  (`ALL | FRIENDS_ONLY | NONE`, default `ALL`).
- Direct messages and invitations are **ephemeral** (in-memory, no
  schema). Persistence is a backlog item.

### Visibility rule

`sees(a, b) = canSee(a, b) ∧ canSee(b, a)` with
`canSee(x, y) = x.visibility == ALL ∨ (x.visibility == FRIENDS_ONLY ∧ friends(x, y))`.
Any existing `BLOCKED` row hides the pair in both directions.
`VisibilityFilter` encapsulates the rule — the UI never checks it
itself.

### Presence

`PresenceTracker` rides on Vaadin `UI` attach/detach with a ref-count
per username (multiple tabs count). `SocialBroadcaster` is the global
fan-out for `SocialEvent`s (`PresenceChanged`, `FriendRequestReceived`,
`FriendshipUpdated`, `VisibilityUpdated`, `DirectMessage`,
`InviteReceived/Withdrawn/Accepted/Declined`).

### Owner mechanics and invites

- `GameSession.hostUsername` is the owner; kick/abort/invite are
  owner-only.
- Auto-transfer on `leaveGame`: next human by seat ID. If there are
  neither humans nor observers left, the game plays itself out via
  `AutoplayRunner`.
- An invite reserves the seat (`GameState.invitedSeats`, ephemeral;
  reset in `resetForNewGame` / `resetForAbort`). `canStartGame` blocks
  while reserved seats have not yet been joined.
- `kickHuman` works pre-start **and** during a running game: the seat
  becomes AI; host transfer plus the autoplay check are identical to
  `leaveGame`.

## Copy semantics

`GameState.copyOf` is a **shallow** copy of the collections (elements
like `StarSystem` and `Fleet` are records and effectively immutable),
with a **deep** copy of the `intel` maps. `GameService.snapshot()`
returns such a copy for read-only UI queries that need more than the
`PlayerViewState` (e.g. `waitThisTurn`). Snapshots must not be mutated.

## Invariants

- `GameState` is mutated exclusively inside a `writeState` context.
- View-model records are immutable and meant for the UI only.
- `CombatResolver` is stateless; the only effect goes through
  `CombatService`.
- When `gameOver()` is true, `TurnEngine` stops all further steps.
- `StarSystem` and `Fleet` are records; changes go through expressive
  domain methods and `state.updateSystem(id, updater)` — never via
  field mutation or generic `withX` setters.

## Thread safety

- Only `GameStateRepository` knows about locks.
- Mutating operations always go through `repository.writeState(...)`;
  reads through `readState(...)` or `snapshot()`.

## Test strategy

- Unit tests against services with hand-built `GameState` instances
  (JUnit 5, plain).
- Integration tests inherit from `AbstractIntegrationTest`: a shared
  PostgreSQL Testcontainer via static `start()` plus
  `@ServiceConnection` (not `@Testcontainers` / `@Container`, otherwise
  one container per class); `SyncAsyncTestConfig` makes `@Async` run
  synchronously.
- No Vaadin UI tests; the core logic stays independent of the UI.
- Architecture check: `ModulithTest` verifies module boundaries on
  every `./gradlew test`.
