# Starfare Architecture

Deep dive into the architecture. The README has the user-facing overview;
this document covers the structural decisions.

The decorative system inspector uses `game.values.SystemComposition`, a pure,
versioned generator seeded by game and system IDs. It receives only immutable
visible values and refuses incomplete visibility. `game.ui.DetailedSystemPanel`
renders the illustration; `FleetAndOrdersPanel` retains it across ordinary
updates and excludes it while battle results are pending. No game-state,
snapshot, map geometry or turn-rule changes are involved. Research and deliberate
simplifications are documented in [System illustrations](SYSTEM-ILLUSTRATIONS.md).

## Goals

- Domain-oriented packages with a thin technical sub-layer per module
  (`ui | application | domain | values | config`).
- Clear flow: UI → `GameService` → services → `GameState`; no feedback
  loop back into the UI.
- Thread safety centralised per game session (`GameSession`,
  `ReentrantReadWriteLock`).
- Module boundaries verified by Spring Modulith (`ModulithTest`).

## Game variants and immutable rule references

`RulesetRef` stores a stable variant ID and rule version independently of the
application release. SectorForces uses `classic / 1.0.0`; the display name does
not change its stored identity. `RulesetCatalog` holds localized name and
description keys, supported versions and availability for new-game creation.
Spaceward (`spaceward / 1.0.0`) has internal economy, navigation and diplomacy implementations
but remains unavailable for new games until its combined acceptance. Orion has
no implementation or selectable entry.

`RulesetConfiguration` supplies the shared catalog. `GameSetup`, `GameState`,
`GameStateSnapshot`, `GameSummary`, templates and compact result history retain
the reference. Missing legacy snapshot/setup references default to SectorForces;
explicit incomplete identities, unsupported versions and conflicting setup/state
references do not fall back. Session and archive stores leave rejected rows
untouched while continuing to load other games. Writes validate support before
mutating the registry state or saving a new game. Creation availability does not
disable an already supported stored version.

`RulesetTurnEngine` is the primary `TurnEngine` used by `GameService` and autoplay.
It dispatches by the full reference to registered `RulesetRoundImplementation`
beans. `DefaultTurnEngine` implements SectorForces 1.0.0 and keeps the frozen
Classic pipeline through `RoundPipeline`. `SpacewardTurnEngine` uses the same
order/arrival/victory sequence with its own AI planning and production. Every
supported reference needs an explicit registered round implementation; incomplete registrations fail during application startup.
The isolated third-variant tests verify extensibility without shipping another
game. Later variants may add their own commands, state and views as required.

Flyway V2_9 adds variant and version columns to `game_results` with legacy
SectorForces defaults. Statistics filter by variant before aggregating outcomes
and opponents; participant and AI collections still use separate fetches.
Rule references remain in the compact result after detailed sessions are removed.

## Spaceward navigation (M4)

The `navigation` module owns pure range calibration and route planning,
immutable `NavigationSettings`/`FlightJourney`/`RoutePreview` values, and the
station lifecycle service. A game's range is the rounded longest edge of its
minimum spanning tree, calculated after final galaxy placement and frozen in
its optional snapshot field. This is the smallest whole range connecting the
galaxy's geometric graph. Expansion still requires capturing stations; travel
speed and sensor coverage keep their existing independent rules. Calibration
checks exercise both layouts, 8/24/60/120 systems and twenty seeds each.

`Routes.plan` selects the shortest elapsed-time route using owned or allied intermediate
systems, adding one full round per stop. Foreign final destinations are allowed.
Fleet acceptance, atomic order editing/undo, production transfers, AI and view
previews use the same planner. Lost routes pause existing transfers without
consuming ships. Each newly launched Spaceward fleet stores its complete path,
current leg, flying/station/blocked phase, earliest departure and final ETA.
The existing fleet endpoints and arrival field describe its physical current leg.
Classic fleets retain their old direct representation and constructor.

`RoundPipeline` applies commands, departs ready stationed fleets, launches
transfers and produces, applies waiting, and resolves arrivals. Owned
intermediate arrivals remain independent fleets and refuel for one full round;
final arrivals use ordinary reinforcement/combat. Arrival at a station already
lost uses ordinary combat and terminates the itinerary there. Loss while docked
blocks departure. A lost next intermediate station also blocks a new leg before departure. Post-combat access checks use the resolved round number, so
recapture cannot bypass a full refuelling round. Docked fleets do not contribute
to the system garrison; disbanding at an owned station explicitly merges them.

Snapshots, copying, archived states and replay frames retain the journey.
Missing navigation fields remain readable: old Classic saves have no settings;
old internal Spaceward states derive a range once and legacy direct fleets can
finish their existing flights. New invalid itinerary/state combinations are
rejected. Arrival summaries count only final destinations, include waits and
exclude blocked fleets. UI cards explain route and phase, use unknown arrival
for blocked travel, and map lines follow actual legs. No rule-dependent state
leaks through foreign-system industry views.

## Spaceward diplomacy (M5)

The diplomacy module owns immutable treaty state, unanimous proposals and
application services. GameState keeps DiplomacyState independently of military
intelligence. Each empire belongs to at most one AllianceGroup; a lone remaining
member becomes contractless. Founding requires both partners, admission requires
all current members plus the applicant, and notice changes require all members.
Overlapping votes are rejected. Roster changes discard stale electorates.

Notice periods are 0–50 whole rounds. A departure takes effect at current turn
plus max(1, notice), processed at the resolution boundary before arrivals.
Already announced dates are never changed by a later agreement. Account-facing
commands run under the game write lock and verify the actual human seat, expected
turn and submission state. Views expose group contracts and only proposals in
which the account votes; they add no military intelligence. Read paths never
write. AI answers pending requests unless it has already announced departure,
and refrains from targeting allies. It does not initiate treaties.

Navigation permits allied intermediate stations. Allied final arrivals use the
PARTNER journey phase and retain independent fleet ownership; they cannot capture
the partner system or disband into its garrison. Combined defence and combat
remain M6 work. A departing member's stationed and already inbound partner
contingents become non-combat returns. They automatically choose the shortest
currently permitted route to an owned system, or remain BLOCKED until one exists.
The first return leg may leave an inaccessible origin; subsequent stations obey
normal ownership/treaty/range checks. Return fleets cannot wait or disband and
never attack at an arrival, even if their destination is lost in flight.

Snapshots add nullable diplomacy and returningHome fields. Missing fields mean
no contracts and ordinary M4 travel. Copying, persistence and archives retain
consent, deadlines and return phases. Invalid memberships, electorate data,
unknown participants and cross-variant treaty state are rejected. SectorForces
has no diplomacy actions. Spaceward remains unavailable until M6/M7 acceptance.

## Spaceward industrial economy (M3)

Spaceward keeps immutable per-system `Industry` records in a separate
`GameState.industries` map. Classic retains an empty map and the existing
`StarSystem` behavior. Capacity starts at the configured system production.
Integer points go to expansion; the remainder builds ships. The next capacity
unit costs twice the current capacity, with a maximum of 50. Partial and surplus
progress survive allocation changes and conquest. At the cap, the allocation
returns to full shipbuilding. Growth changes output for the following turn only.

`StarSystem.productionPerTurn` always contains actual ship output, so existing
routing limits, delivery checks and logistics previews consume the same value.
Standing orders retained after an allocation change may use the source garrison
above its reserve; incoming rates never count as already delivered ships.
Defense losses may leave a garrison below its reserve: production continues
while transfers wait for sufficient surplus. `IndustryView.reserveShortfall`
also accounts for that deficit in the editable allocation preview.
`DefaultEconomyService` produces only the output available before this turn's
growth. Capture resets allocation to full shipbuilding and reserve to zero while
retaining industrial capacity and progress, directly in `GameState.updateSystem`.
Neutral systems never expand automatically.

`allocateExpansion` validates rules, owner, human seat, expected turn and
submission state under the game write lock. `ProductionAllocationChanged`
refreshes subscribed views. Economy data is optional at the snapshot boundary;
old Classic saves and replay frames remain valid. New frames store their own
industry map so historical views never read present-day progress. Invalid or
inconsistent industrial snapshots are rejected during restoration.

`PlayerViewBuilder` exposes an `IndustryView` only with full visibility. The
system illustration adds industry buildings; the adjacent `IndustryPanel`
shows allocation, progress, time to growth and next outgoing delivery. Edits
preview shortages before they are applied. Foreign sensor views and unresolved
battles do not reveal the industrial details. Observers and archives are read-only.
`DefaultSpacewardPlanning` uses filtered views, allocating one third of capacity
to expansion in safe systems and all output to ships near known enemies.

This is Starfare's initial approximation. The long-term inspiration is Delta
Tao's [Spaceward Ho! 5 manual](https://www.deltatao.com/ho/ho/): colony suitability,
terraforming, finite metal, empire-wide budgets, ship design and technology are
separate future decisions. They are not implemented as part of M3.

## Classic compatibility contract

The Classic reference baseline is commit `c5df8d1` (2026-10-02), before ruleset
selection is introduced. `ClassicTurnContractTest` executes the real turn,
fleet, combat and report services with human-only fixtures, zero combat
randomness and strongest-first attack order. Fixed expected values protect
the existing pipeline; they are not calculated with the implementation under
test. The normal random attack order and non-zero strength rolls remain
covered by `TurnEngineRoundRulesTest` and `CombatResolverTest`.

| Reference case | Required result |
| --- | --- |
| Production, reserve, routing, queued send and wait | Four systems have garrisons `6/6/12/7`, `6/11/14/7`, then `6/20/16/7`; the source retains reserve 6, neutral production does not grow its garrison, delayed and routed fleets merge on arrival. |
| Friendly arrivals and two equal attackers | Friendly fleets merge before combat. Production plus reinforcement gives 12 defenders; player 2 loses a 12:12 tie leaving zero defenders, then player 3 captures with 12 ships. Capture clears the old reserve. |
| Victory and continuation | Three of four systems win at 70%. Further turns do nothing. Continuation requires full conquest; the next win records 100%, while the earlier replay still records 70%. |

`src/test/resources/classic/legacy-running.json` and `legacy-archive.json` are
hand-authored, synthetic old-format fixtures, not production exports. They
omit newer settings, `originalSetup`, previous orders, reserve and empire-name
fields. The running fixture also omits replay frames, reinforcement totals,
standing-order quantities and intel garrisons. The archive includes historical
owners and fleets, missing combat strengths, integer combat strengths, and a
victory event without its threshold. These files must not be regenerated from
the current serializer: that would silently erase the compatibility cases.
`nextLocalFleetNo` stores the last assigned number despite its name; a stored
value of 2 must allocate fleet F3 next.

`ClassicSnapshotCompatibilityTest` reads them with the application's mapper,
continues the running game, checks replay perspectives, writes and reads the
current format, and reloads raw fixtures from PostgreSQL through a fresh
`JpaGameSessionStore`. Read-only views and system illustrations must preserve
both the state snapshot and the database JSON/version. An absent completion
date stays absent, an absent visibility defaults to public, and missing
original settings do not become an invented game template.
The retained-archive case also exercises `GameService` review/replay and the
template service after the operative session is absent. It preserves archive
JSON and version, and cannot continue an archived game or invent a rematch.

Run the focused contract with `./gradlew test --tests '*ClassicTurnContractTest'
--tests '*ClassicSnapshotCompatibilityTest'`. Before changing Classic or
persistence, also run `./gradlew build`,
`node --test src/test/frontend/battle-replay.test.mjs` and `./gradlew e2eTest`.
The browser suite is muted. Future rule implementations must retain these
Classic outcomes; deliberate behavior changes require an explicit rule/version
decision, not replacement of the old expected values.

## Package layout

Base package: `de.zettsystems.starfare.<domain>.<technical>`.

- `game` — Core. `application` (`GameService`,
  `PlayerViewBuilder`, `GameRegistry`, `AutoplayRunner`, `Broadcaster`),
  `domain` (`GameState`), `values` (records for setup/views:
  `StarSystem`, `Fleet`, `PlayerViewState`, `VisibleSystem`, `FleetView`,
  `GameSetup`, `GameConfig`, `PlannedOrder`, `StandingOrderView`, …),
  `ui` (Vaadin views: `LobbyView`, `MainView`, `RoundView`, panels,
  dialogs, `UiMapper`, `UiTexts`).
- `turn` — Ruleset dispatch and shared `RoundPipeline`.
- `combat` — Combat resolution (`CombatService`, `CombatResolver`).
- `fleet` — Fleet commands and validation (`FleetService`,
  `FleetOrder`).
- `economy` — Spaceward capacity, allocation and progress (`Industry`),
  production (`EconomyService`) and the immutable inspector (`IndustryView`, `IndustryPanel`).
- `diplomacy` — alliance groups, unanimous consent and round-based departures.
- `ai` — Classic and Spaceward AI decisions (`AiService`, `SpacewardPlanning`).
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
- `social` — Presence, friendship, direct and game messages, invitations and
  persistent personal display settings.
- `legal` — Imprint, privacy information and account deletion views.
- `style` — `CssProperties`, `HtmlAttributes` (central constants for
  Vaadin styling and attributes).

Every top-level module has a `package-info.java` with
`@ApplicationModule(type = OPEN)`, so that sub-layers are visible to
other modules. New modules need the same declaration.

## Dependencies

- `game.ui` → `GameService` → services
  (`turn/combat/fleet/ai/report/economy`) → `GameState`.
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

1. `AiService.planAiTurns` queues the AI's orders, exactly like a human's.
   The AI plans on the state the humans saw during the round, so it has no
   information advantage and its fleets launch in the same round.
2. Apply all queued orders (send, wait, disband), then standing relocations.
3. Production on owned, non-neutral systems.
4. Apply `waitThisTurn` (arrival +1, clear flag).
5. Group all arrivals of the round by `toSystemId` + owner before any combat;
   reinforce the owner first, then resolve the attackers one after another via
   `CombatService`, each against the garrison left by the previous one. The
   order is a game rule (`RoundRules.attackOrder`): random (default) or
   strongest first.
6. Victory check (>= the configured system share, neutrals included;
   default `GameConfig.VICTORY_SYSTEM_PERCENT`, 70%); it sends `Victory` to the winner and
   `Defeat(winnerId, winnerName)` to every other participant.
7. `state.nextTurn()`.

If `state.gameOver()` is true, `advanceTurn` is a no-op.

`TurnEvent.Victory` stores the threshold at resolution time. Reports therefore
retain the correct threshold even after continuation or during archive replay;
JSON without this field defaults to the historical 70% rule.
`BattleReplay.Outcome` carries survivor counts and the explicit attacker result
from the event type. Playback colours use that result, including victories with
zero survivors on both sides, rather than comparing rounded ship counts.

`DefaultAutoplayRunner` resolves unattended AI games through
`GameRegistry.writeState`, so each turn is persisted under the session lock
and completion also creates the archive. It records the first result through
`GameStatisticsService` before publishing the final turn and completion events.

## Round limits

A round ends once every human has submitted, or at the round deadline
(`GameState.roundDeadline`): the round limit counts from the start of the
round, the straggler limit from the moment only one human is still missing;
whichever comes first. Both are chosen per game in the wizard (defaults 5 min
and 1 min). Games with fewer than two humans have no deadline.
The header uses **Submit turn** for human players and disables the action after
submission. `RoundStatus.hasSubmitted` and `pendingPlayerLabels` drive the
persistent **Submitted — waiting for …** notice below the header. Both derive
from the current server state, including after reload or a pushed round change.
AI-only spectators retain **Next turn** as an advance action.
`RoundDeadlineRunner` checks every `starfare.game.round-check-interval` (5 s).
At the deadline, late players move with the orders they had queued; after
`GameConfig.MAX_MISSED_ROUNDS` (3) missed deadlines in a row the AI takes the
seat, which can be reclaimed if the game allows re-entry.

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
`game_results.ai_victory` distinguishes an AI winner without an account from
a game without a winner. Such victories count as losses for human participants.
Older results default to false because a missing account alone cannot establish
whether the original game ended with an AI victory or no winner.

Only the recorded winner may resume a completed game. `resumeForFullConquest`
raises the victory threshold to 100%, clears the outcome and submissions, and
restarts the round and straggler clocks without advancing the turn.
`GameContinued` refreshes every subscribed view and clears its outcome
acknowledgement and any open outcome dialog. A subsequent game end therefore
waits for its own pending battles before opening the result or review again.
The initial statistical result remains unchanged when play continues.

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
Fleets, Orders, Relocations, Logistics and Report. Contacts derive their last hostile
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
The Relocations cards use that same dialog for edits and offer direct deletion
per route.

The fleet dialog updates its shared amount model, numeric input, slider,
shortcuts and preview when switching between direct fleets and relocations.
Direct fleets use available ships; relocations use `routingHeadroom` even when
production exceeds the current garrison. Routing capacity reads never create
standing-order collections; only an accepted write initializes them.

Fleet selection clears the previous system selection and opens fleet details.
`MapCanvas.useGame` binds the viewport storage key to the resolved route ID
before installing browser handlers. Delayed storage writes are discarded if
the canvas has since switched to another game.

The game chat subscribes to `ChatMessage` while its dialog is open and attached,
and removes its subscription on close or detach. Leaving the map closes the
dialog as well. Chat events refresh only the messages, preserving the draft
and the map sidebar's current inputs.

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
report value used by the UI dialog: it transports initial and remaining fleets
and rolled combat strengths, but deliberately carries no ownership transition.
`BattleAcknowledgements` loads durable acknowledgements through
`BattleAcknowledgementService`, keyed by account, game, round and original event
index. Its view-local cache is reloaded when the view rebuilds; filtering and
sorting never change event identity. Instant mode can acknowledge every pending
battle in the round, including filtered-out events, before one view refresh.
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
  The setup also persists the default for battle presentation. An explicit
  personal speed choice overrides it across rounds and sessions via
  `user_display_settings`; until then the game default applies.
  Combat randomness (0–30%, default ±10%) and the victory threshold (10–100%,
  default 70%) are game rules. Only the winner's full-conquest continuation
  changes the victory threshold after the start, to 100%.
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
Both `DefaultSocialBroadcaster` and `DefaultBroadcaster` stop delivery and
release their listeners on `ContextClosedEvent`, before bean destruction.
UI detachment can still update presence counts during shutdown, but its events
no longer enqueue view refreshes against a closed database.

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

## Command planning and personal display choices

`OrderPlanning` validates a complete replacement of one player's pending orders
under `GameRegistry.writeState`. Edits compare the displayed command with the
current index and use a turn token. Batch dispatch rejects duplicate sources,
foreign systems, missing destinations and overcommitted garrisons before any
mutation. Batch reserves similarly check all selected systems and pending sends.
`GameState.previousOrders` stores one undo step per player, is part of the optional
snapshot fields, and is cleared by `nextTurn`. Undo revalidates current reserves.
`ArrivalPreview` combines fleets and queued sends by destination and arrival turn;
wait commands affect the existing fleet rather than adding a second arrival.

`GameState.originalSetup` retains the normalized starting setup. The optional
snapshot field is absent in older saves; those games cannot supply an exact
template. `GameTemplateService` restricts copying to hosts and participants,
generates a fresh private game, and optionally reserves the previous human seats.
Reservations do not join participants or start the game. Archived source games
use the same access check.

`user_display_settings` (Flyway V2_8) belongs to the existing `user_preferences`
aggregate. Its enum keys store speed, sound, event/logistics filters, route search
and sidebar position. `DisplayPreferences` loads an account-bound session cache;
each explicit changed choice is saved transactionally. Reopening a map refreshes
the cache. These values never modify game setup or game state.

Map and sidebar rendering skip unchanged content. Route filters are stable Vaadin
components while their result containers are replaced independently. Custom click
targets use `KeyboardActions` with Enter/Space activation and visible focus styles.
