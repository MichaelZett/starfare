# Starfare Architecture

Deep dive into the architecture. The README has the user-facing overview;
this document covers the structural decisions.

## Goals

- Domain-oriented packages with a thin technical sub-layer per module
  (`ui | application | domain | values | config`).
- Clear flow: UI → `GameService` → services → `GameState`; no feedback
  loop back into the UI.
- Thread safety centralised per game session (`GameSession`,
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
- `auth` — Starfare adapter around the reusable identity building block
  (`de.zettsystems:identity-core` / `identity-vaadin` from the `zs-identity`
  repository, consumed as Maven artifacts). Authentication uses email; game and
  social references use the immutable account ID, while the public player name
  (`zs.identity.name-mode: DISPLAY_NAME`) is presentation-only. The building
  block migrates its own database schema `identity` (own Flyway history, run
  before the application's); Starfare's schema lives in `V2_x` under
  `db/migration`.
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
- `game.application.GameRegistry` locates games. Each `GameSession` owns
  exactly one `GameState` and its lock; access goes exclusively through
  `GameRegistry.readState(gameId, fn)` / `writeState(gameId, fn)`.
- `JpaGameSessionStore` caches sessions in memory and persists a
  `GameStateSnapshot` JSON document after every write. At application
  startup it recreates all sessions from `game_sessions`.
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
5. Victory check (>= 70% of all systems, neutrals included;
   `GameConfig.VICTORY_SYSTEM_PERCENT`); it sends `Victory` to the winner and
   `Defeat(winnerId, winnerName)` to every other participant.
6. `state.nextTurn()`.

If `state.gameOver()` is true, `advanceTurn` is a no-op.

## Lifecycle and lobby

`GameState` carries `active` (game exists), `started` (turns running)
and `joinedHumanPlayerIds`. `LobbyView` walks
`newGame → joinGame → canStartGame → startGame → MainView`.
`PlayerContext` holds the player ID in the Vaadin session.
`MainView.onAttach` redirects back to the lobby when no game is
running.

### Game visibility and archive

`GameAccessPolicy` centralizes visibility, spectator and completed-game access.
`GameVisibility` is independent of the social visibility preference. New states
are private; snapshots without the optional visibility field restore as public.
`visibleGamesFor(account, scope)` separates the active lobby from the archive;
`summaryFor` and `viewForAccount` enforce account access before returning UI data.
The UI resolves accounts from the authenticated session, not route parameters.

Only the current host may change visibility, and only before the start. Making
a game private removes unrelated spectator registrations. Invitations grant
lobby visibility and a reserved seat, but do not independently reveal the map.
After completion, invitations no longer grant private archive access.

`GameOutcome` carries winner and completion time to the archive. `finishedAt`
is set at the first game end and preserved by snapshots and copies; old archives
without the timestamp retain an unknown date. Archive opening calls `reviewFor`
and does not register spectators or save state. Map actions and service commands
reject edits after game end. At the first game end, `GameStatisticsService` writes
the compact result plus human participants to `game_results`; this data remains
after `game_sessions` is later pruned.

`JpaGameSessionStore` also writes a complete idempotent copy to `game_archives`.
The optional `GameArchiveCleanupRunner` removes only operative `game_sessions`
older than `starfare.game.archive-cleanup.retention` (90 days by default), and
only when cleanup is enabled. It rechecks that the session is completed before
deleting it; archive review, replay and statistics then read from their durable
stores.

`GameState.replayFrames` stores immutable systems, fleets and reports at game
start and after every resolved turn. It is optional in `GameStateSnapshot` so
older archives still load. During archive review, the map's timeline selects a
frame through `replayFor`; replay data is read-only and is rendered without fog.
The final timeline position returns to `reviewFor`, where participant perspective
and fog remain selectable.

### Map sidebar

`FleetAndOrdersPanel` is a view-local, switchable sidebar for Contacts, Details,
Fleets, Orders, Relocations and Report. Contacts derive their last hostile
intelligence from fog-filtered `VisibleSystem` values; Report renders the player's
turn events beside the map. It receives only `PlayerViewState` and
the view-local map selection from `MainView`; selecting a system or fleet never
mutates the game. New commands select Orders and a resolved turn selects Report;
the report stays in the sidebar. Making that automatic choice a user preference
remains future work.

`TurnEvent.affectedSystemId()` is the shared link between the report and the
map. Every event with a system ID gets a marker. Selecting a report card centres
the map and highlights its marker; selecting the marker returns to and
highlights the matching report card. The marker handles its click independently
so it cannot create a fleet command.

Standing orders render as purple dashed map edges with their fixed per-turn
amount. Drag-and-drop starts only at an owned, fully visible system and opens
the existing fleet dialog in relocation mode. It delegates the amount limit and
same-route replacement to `GameService.routingHeadroom` and `addStandingOrder`.
The Relocations table uses that same dialog for edits and offers direct deletion
per row.

`reviewFor(id, account, perspective, fogOfWar)` checks completed-game access and
participant existence inside the read lock. `PlayerViewBuilder.forReview` reuses
the normal sensor/intel filter even after completion when fog is enabled; without
fog it returns all systems and fleets. `ReviewControls` stores selection only in
the view and resets it on route entry. The selected participant never replaces
the authenticated account or its seat. Rendering remains read-only in both modes.

For a running spectator, `observerViewFor(id, account, perspective, fogOfWar)`
uses the same perspective builder after checking the observer registration and
the access policy. `SpectatorControls` keep the selection in the current view;
they never create a seat or expose commands. `BattleReplay` is a combat-only
report value used by the UI dialog: it transports initial and remaining fleets,
but deliberately carries no ownership transition. `BattleAcknowledgements`
stores a per-browser-session acknowledgement for the report's battle systems.
Until acknowledgement, report text remains neutral and the map uses a battle
marker that hides the affected system's result; this presentation state never
changes the persisted game state.

Host transfers and snapshot persistence use the session write lock. Saving an
existing entity updates both its snapshot and current host, so a restart cannot
restore obsolete host permissions.

## Configuration and setup

- Tunables: constants in `game.values.GameConfig`.
- Spring configs: `@ConfigurationProperties` (records or classes), never
  `@Value`. `@Configuration` classes live in the `<module>/config/`
  package (they fit neither `ui` nor `application/domain/values`).
- New game: wizard in `LobbyView` → `GameSetup` (`game.values`);
  `GameSetup.normalized()` clamps and defaults inputs. The fixed limits are
  8–120 systems, 0–8 human and 0–7 AI participants (at most 10 combined),
  production 1–20 and starting garrison 1–50. The system count is raised to
  the participant count when necessary.
  The setup also persists the default for battle presentation. Each player can
  override it for the currently shown round in their Vaadin session; the next
  round starts from the game's stored default.
  `GameRegistry.createGame(GameSetup, hostPlayerId, name)` is the main
  entry point (the short overload builds a default, unnamed game).
- `GameState.ownershipHistory` records each system ownership change and is
  persisted in the snapshot. `PlayerViewBuilder` exposes it only for fully
  visible systems, so system details never reveal historical information
  through fog of war.
- `DefaultGameRegistry.homeSystems` uses greedy farthest-next placement for
  every galaxy layout. Random layout controls system distribution, not the
  separation of player starts.

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
- `direct_messages` stores the chronological history between two users;
  `SocialBroadcaster` still delivers newly sent messages immediately to
  attached UIs. Sending remains limited to visible, online recipients. Opening
  a conversation records incoming messages as read. Each participant can archive
  a conversation independently; the other participant retains it. The store also
  supports removing messages older than the one-year retention period.
- Invitations are stored as `GameState.invitedSeats` in the persisted
  `game_sessions.state_json` snapshot, so a restart retains the seat
  reservation.

### Visibility rule

`sees(a, b) = canSee(a, b) ∧ canSee(b, a)` with
`canSee(x, y) = x.visibility == ALL ∨ (x.visibility == FRIENDS_ONLY ∧ friends(x, y))`.
Any existing `BLOCKED` row hides the pair in both directions.
`VisibilityFilter` encapsulates the rule — the UI never checks it
itself.

### Presence

`PresenceTracker` rides on Vaadin `UI` attach/detach with a ref-count
per player id (multiple tabs count). `SocialBroadcaster` is the global
fan-out for `SocialEvent`s (`PresenceChanged`, `FriendRequestReceived`,
`FriendshipUpdated`, `VisibilityUpdated`, `DirectMessage`,
`InviteReceived/Withdrawn/Accepted/Declined`).

### Owner mechanics and invites

- `GameSession.hostPlayerId` is the owner; kick/abort/invite are
  owner-only.
- Auto-transfer on `leaveGame`: next human by seat ID. If there are
  neither humans nor observers left, the game plays itself out via
  `AutoplayRunner`.
- An invite reserves the seat (`GameState.invitedSeats`, persisted with the
  game session and reset in `resetForNewGame` / `resetForAbort`). `canStartGame` blocks
  while reserved seats have not yet been joined.
- `kickHuman` works pre-start **and** during a running game: the seat
  becomes AI; host transfer plus the autoplay check are identical to
  `leaveGame`.

## Copy semantics

`GameState.copyOf` is a **shallow** copy of the collections (elements
like `StarSystem` and `Fleet` are records and effectively immutable),
with a **deep** copy of the `intel` maps; it backs the persistence
snapshots. The UI never sees a `GameState`: it consumes the immutable
view models in `game.values` only — `PlayerViewState` (including
`EmpireStats` and `waitingFleetIds` for the map) and `GameSummary` (lobby
and manage dialog), built by `PlayerViewBuilder` and
`GameService.summaryOf`. The ArchUnit rule `ui_must_not_depend_on_domain`
enforces this.

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

- Only `GameSession` knows about the lock. `GameRegistry` is the sole
  entry point to it.
- Mutating operations always go through `registry.writeState(...)`;
  reads through `registry.readState(...)`, which hands the UI-facing
  service an immutable view model.

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
