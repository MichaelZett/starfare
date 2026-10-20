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
Spaceward supports stored versions 1.0.0, 1.1.0 and W1–W7. New games offer
`spaceward / 1.2.0-w7` by default and W6 as an alternative. Older versions remain
restoration-only. Versioned implementations share economy, navigation, diplomacy
and coalition combat services;
Spaceward is enabled for new games since M7. Orion has
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

## Configurable victory and diplomacy

`VictoryRules` groups the individual system share, alliance permission, group
victory permission and separate group system share. `GameSetup` normalizes these
at creation; `GameState` rejects changes after start. Alliance permission gates
contracts, allied station access and diplomacy views. Disabling it forces group
victory off. SectorForces remains without diplomacy; Spaceward 1.0.0 permits
alliances and individual victory only. Missing legacy snapshot fields derive
these defaults from the stored reference, without migrating its version.

`AllianceSpacewardTurnEngine` explicitly registers 1.1.0. `VictoryEvaluation`
checks individual ownership first, then effective groups of at least two
members. Neutral systems remain in the denominator; integer comparisons avoid
rounding down. The strongest qualifying group wins, then the lowest group ID.
Every effective member wins, including zero-system members. Departures become
effective through the existing round pipeline before evaluation.

`GameOutcome` freezes all winning player IDs and the optional group ID. Legacy
`winnerId` remains populated for individual wins and is null for group wins.
Snapshots, replay frames, completion events and player views carry the full
outcome; victory/defeat reports preserve group membership at resolution time.
Old constructors and nullable JSON fields retain legacy read compatibility.
Account statistics use Flyway V2_11's `game_result_winners`, backfilled from
legacy individual winners. Participants, AI names and winners are fetched with
three separate collection queries, avoiding a Cartesian product.

The internal browser acceptance route exists only in e2e sources. It previews
the wizard's Spaceward choices without modifying the production catalog, which enables W6 and W7 for new Spaceward games. The muted
`alliance_victory.feature` covers option dependencies, both winners, reload and
archive. The strategy harness can pair 1.1.0 scenarios with group victory on/off;
each member receives its own win in the report.

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

`VictoryRules.aiAlliancesAllowed` is fixed per game (setup, snapshot, templates);
missing JSON reads as `true` through the `@JsonCreator`, and the value is only
effective while alliances are allowed. When it is false,
`DefaultDiplomacyService.act` refuses FOUND with an AI party, JOIN by an AI and
APPROVE of such a proposal; NOTICE, REJECT and LEAVE stay available so existing
treaties of an AI-controlled seat can still be managed. `answerAiProposals` rejects
proposals that would bring in an AI. `DiplomacyView.aiAlliancesAllowed` lets the
dialog explain the blocked partners instead of offering them.

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
the partner system or disband into its garrison. Combined defence and combat use the separate M6 coalition path. A departing member's stationed and already inbound partner
contingents become non-combat returns. They automatically choose the shortest
currently permitted route to an owned system, or remain BLOCKED until one exists.
The first return leg may leave an inaccessible origin; subsequent stations obey
normal ownership/treaty/range checks. Return fleets cannot wait or disband and
never attack at an arrival, even if their destination is lost in flight.

Snapshots add nullable diplomacy and returningHome fields. Missing fields mean
no contracts and ordinary M4 travel. Copying, persistence and archives retain
consent, deadlines and return phases. Invalid memberships, electorate data,
unknown participants and cross-variant treaty state are rejected. SectorForces
has no diplomacy actions.

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
The BASELINE branch of `DefaultSpacewardPlanning` uses filtered views, allocating one third of capacity
to expansion in safe systems and all output to ships near known enemies.

This is Starfare's initial approximation. The long-term inspiration is Delta
Tao's [Spaceward Ho! 5 manual](https://www.deltatao.com/ho/ho/): colony suitability,
terraforming, finite metal, empire-wide budgets, ship design and technology are
separate future decisions. They are not implemented as part of M3.

## Internal metal checkpoint: Spaceward 1.2.0-w2

`RulesetRef.SPACEWARD_METALS` extends colony rules under an independent frozen
reference. `MetalSpacewardTurnEngine` is registered alongside W1 and older rules.
`MetalEconomy` holds immutable deposit, player-stock and mining-allocation maps;
`MetalGenesis` is an explicit creation boundary. Snapshots and replay frames carry
an optional `metals` field. W2 requires complete current and historical resources;
legacy games do not acquire deposits when loaded. Public creation stays disabled.

`MetalProduction.calculate` is the pure shared empire forecast. It extracts finite
local deposits, pools each player's metal, and distributes whole ships by largest
remainder with system-ID ties. `DefaultEconomyService` validates both allocations
before mutation and applies construction, expansion and growth under the game lock.
W2 dispatches ordinary orders before production, then relocates from actual
garrisons; the legacy pre-production relocation path remains unchanged.
`StarSystem.productionPerTurn` retains planned capacity minus mining and expansion
for stored state validation. Player views, empire totals and routing headroom use
the shared metal forecast for actual buildable output. Sensors expose only the
existing approximate production information; exact resources require complete visibility.
Historical views calculate from frame resources rather than the current economy.

W2 trial constants are in `MetalRules`: one metal per ship, 10 initial stock,
200 deposit metal per richness, and richness metal per mining point. W4 will assess
balance; changing these values requires another reference. All AI profiles receive
a legal common mining assignment before their existing industry and fleet policies;
new mining or recycling strategy comparisons remain a W4 task.

## Internal colony checkpoint: Spaceward 1.2.0-w1

`RulesetRef.SPACEWARD_COLONIES` is a separate, frozen internal rule reference.
The catalog supports restoration and `ColonySpacewardTurnEngine` dispatch, but
`requireCreatable` rejects public creation. The current wizard offers W6 and W7.
Subsequent metal rules must use a different reference rather than change W1.

`SystemQuality` saves stellar luminosity, orbital distance in AU, host body,
whether the colony is on a moon, atmosphere and independently generated metal
richness. `ColonyGenesis` generates this once at the explicit creation boundary.
Home systems share fertility and richness despite different star appearances.
`SystemComposition` renders the saved host for fully visible W1 systems;
older versions retain their decorative generator. Richness is not yet a deposit.

`ColonyEconomy` combines the immutable quality and population. One labor unit
is 100 population points; usable capacity is min(installed industry, whole labor
units). Suitability permits 25/35/50 labor units, with 1/2/3 percent growth per
round, rounded up to whole population points and capped at the population limit.
The stellar-flux proxy is luminosity / orbital-distance squared. Temperate
atmosphere at flux 0.75–1.5 is fertile; thin or temperate atmosphere at 0.4–2 is
habitable; other generated colonies are harsh. These thresholds are game balance
choices, not a physical climate model. The conceptual basis is the joint role of
distance and atmosphere described by [NASA](https://science.nasa.gov/exoplanets/habitable-zone/).

`GameState.colonies`, optional snapshot and replay-frame maps preserve the exact
properties and fractional growth. W1 rejects incomplete colony maps, including
historical frames; restoration never regenerates them. Older versions accept
missing fields and retain empty maps. Copying uses immutable records and separate
maps. Conquest retains the colony while resetting allocation; neutral systems
do not grow automatically. Actual production uses current labor and allocation;
growth updates the next turn's output. Allocation, routing, AI and UI previews
consume the same usable capacity. The original versions retain their outputs.

Only fully visible industry views contain `ColonyView`; sensor contacts receive
no population or environment details. Replay views use their frame's saved values.
`ColonySnapshotTest` exercises JSON and a new store instance restoring a database
party before resolving another round. The test-only `colonies.feature` journey
checks allocation, growth, reload and historical review. This checkpoint adds no
mining, resource spending, recycling, ship designs or research.

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
   default `GameConfig.VICTORY_SYSTEM_PERCENT`, 70%); optional group victory follows
   individual victory. It sends `Victory` to every winner and `Defeat` with the
   winning members to every other participant.
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

Any recorded winning member may resume a completed game once. `resumeForFullConquest`
raises both victory thresholds to 100%, clears the outcome and submissions, and
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

### Phone layout

A session builds exactly one interface. `Devices` (module `style`) decides once per
`VaadinSession` from the user agent: phones (`iPhone`, `iPod`, `Mobi`) get
`DeviceLayout.MOBILE`; tablets and everything else keep `DESKTOP`. A `layout`
query parameter overrides the choice for the session; `DeviceLayoutInitListener`
reads it from the request or from the `query`/`location` of Vaadin's init request.
For phones it adds the static stylesheet `META-INF/resources/styles/starfare-mobile.css`
and the class `starfare-mobile` on `<html>`. The stylesheet lies outside the frontend
bundle, so desktops never load it; all its rules are scoped to that class.

Lobby, archive, statistics and dialogs share their components with the desktop
and adapt through that stylesheet. The map does not: `MainView` builds either the
`SplitLayout` with map and sidebar or `MobileMapLayout`, never both. The phone layout
reuses the same `MapCanvas`, `FleetAndOrdersPanel` and controls, arranges them as
full-screen map plus a sheet, and receives the target-selection state on every refresh.
`MapHeaderBar` has a compact variant without title and language switcher. Game
logic and callbacks stay in `MainView`; only the arrangement differs.

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

## Spaceward coalition combat (M6)

RoundPipeline dispatches arrivals to CoalitionCombatService only for the fixed
Spaceward reference. SectorForces retains CombatService/CombatResolver and its
configured sequential attack order. The coalition service is stateless; its
caller holds the game lock. All battles resolve synchronously before station
access refresh, victory evaluation and replay capture.

Each location collects the garrison, arriving fleets and friendly stationed
contingents. Alliance groups form sides; unaffiliated empires and a neutral
garrison each form a side. Protected returns and inaccessible blocked stays
are excluded. CoalitionResolver sorts sides and members by stable IDs before
using an injectable roll function. Each side receives one strength factor per
battle. In each volley, source strength is divided across enemies proportional
to their current strengths. All losses use the same volley inputs. Ship losses
are rounded upward and clamped; surviving hostile sides repeat until at most
one remains. ProportionalAllocation uses integer floors and largest remainders
with ascending-ID ties, first between empires, then between garrison and fleets.
Cumulative inflicted fire allocates actual enemy losses without duplicate kills.

Successful defence retains the owner. Total annihilation retains the previous
owner, including neutral ownership. FleetOrder.Send and Fleet add nullable
beneficiaryId fields; missing old JSON fields mean the fleet owner. The launch
UI validates own/allied beneficiaries under the write lock, with a turn token
and submission guard. Editing compares and retains the stored claim; undo and
snapshots retain it. Expired claims fall back to the ship owner. Conflicting
claims use the largest surviving empire, then player ID; that empire's strongest
claim wins, then beneficiary ID. Ownership never transfers allied survivors.

The additive coalitionBattle event stores immutable CoalitionSide/Member facts,
rolled strengths, actual remaining forces and allocated kills. It also stores
ownership separately from BattleReplay's combat-only projection. Reports are
identical for physically participating empires, including zero-strength owners.
Historical playback therefore needs no current alliance state. Existing Classic
event names and replay formats remain readable. Outcome totals consume only
that player's recorded losses and kill share.

CoalitionConcealment is a presentation-only immutable view projection. Pending
coalition battles conceal ownership, industrial results and stationed survivors
in map, logistics and fleet cards, including after reload. Recorded participating
fleet IDs constrain concealment to actual combatants; protected blocked fleets
at the same location remain visible. Missing IDs in older event JSON default to
an empty immutable list. Existing durable
acknowledgements and final-outcome gating apply to the new battle subtype.
The independent frontend animation reads recorded results, reveals every side
concurrently and preserves the personal sound preference. SharedBrowser mutes
all browser acceptance tests.

## Spaceward release (M7)

`RulesetCatalog.builtIn()` enables creation for Spaceward with default
`spaceward / 1.1.0` and keeps `1.0.0` supported for stored games; SectorForces
stays first and therefore preselected in the wizard. `RulesetRef.resolvesCoalitionBattles`
(true for both Spaceward versions) hides the SectorForces attack-order section for Spaceward; such setups store the
default order, and the summary names simultaneous coalition combat. No stored data changes:
existing games keep their reference, and templates and rematches of Spaceward
games are now creatable as well. `DefaultGameRegistry.createGame` initializes
industry and the calibrated range directly after galaxy generation, so lobby
views show the final Spaceward state before the start. `SpacewardReleaseIntegrationTest`
covers creation, joining, starting, regular round resolution and a rematch through
the public services against PostgreSQL.


## Reproducible strategy experiments

`src/test/java/de/zettsystems/starfare/simulation` is executable test tooling,
excluded from the application artifact. Gradle's opt-in `strategyBenchmark`
JavaExec task uses the test runtime, but starts neither Spring nor JUnit.
`Scenario` creates an isolated in-memory game from explicit map and combat seeds.
`SimulationEngine` obtains all plans before applying any command, validates them
through FleetService and EconomyService, then invokes the normal ruleset engine
with its automatic AI planning replaced by a no-op. Application defaults keep
their historical BASELINE behavior and ThreadLocalRandom sources. Selected
alternative profiles use the shared pure `ai.domain.AiPolicy`, also called by
the benchmark adapter. `DefaultProfilePlanning` captures `AiObservation` from a
filtered player view, allies and legal route durations, then applies `AiOrders`
through the economy and fleet services under the caller's write lock. The policy
receives no mutable GameState. Classic and both Spaceward engines dispatch each
AI seat independently; Spaceward still answers diplomacy proposals before planning.

`AiStrategyFields` offers one choice per AI seat and restricts items by ruleset.
`GameSetup.aiStrategies` indexes only AI seats; normalization fills absent entries
with BASELINE, drops extra entries and resets unsupported industry profiles.
`DefaultGameRegistry` copies each choice into `Player.aiStrategy`. Both fields
are included in existing JSON snapshots, requiring no database migration.
Missing fields deserialize as the historical baseline. Player handovers retain
the profile; templates and rematches retain the setup list. The muted
`ai_strategies.feature` verifies ruleset switches, count changes, confirmation,
back navigation, creation and reload.

`Observation` exposes only PlayerViewBuilder's filtered state, diplomacy membership
and legal route durations. Alternative policies never receive GameState. The
baseline adapter invokes the existing AI on a private copy with only the current
seat marked as AI and no pending orders. Classic baseline only reads exact own
forces and distances to non-owned systems; Spaceward baseline already consumes
filtered views. Information-boundary tests perturb hidden forces and enemy orders.
Only the trusted simulator and result recorder inspect complete state.

Explicit --profiles selections in matrix and multiplayer mode are validated
against every chosen ruleset before any match. Historical default profile sets
remain unchanged. The shared `ai.domain.IndustryPolicy` supplies two allocations:
INDUSTRY_LIGHT and INDUSTRY_ADAPTIVE. They reuse INDUSTRY's military policy and
consume only own industry, available ships, reserve shortfall and route distance
to visible enemies. Unknown threats are not revealed. Integer allocation may be
zero on very small colonies; no fractional growth or new economic rule is added.

CombatService accepts a DoubleSupplier, coalition combat an IntToDoubleFunction,
and RoundPipeline a supplier for attack-order randomness. The simulator resets
separate combat and attack-order generators from the absolute turn and combat
seed. This makes continuation independent of how much entropy previous rounds
consumed, without changing any damage formula or default rule. Checkpoint forks
round-trip GameStateSnapshot to preserve fleet counters and all navigation state.
No wall-clock field participates in the gameplay fingerprint. Diplomacy sets and
maps are sorted before hashing. Route results are cached per simulation and
player, keyed by geometry, rules, range, ownership and station rights; changes
to those inputs invalidate the cache. Routes.reachableRounds batches shortest
travel durations over one shared edge table; normal route selection and fleet
validation remain unchanged. Classic observations use direct travel durations.
UI replay frames are released after metrics and the fingerprint have been recorded
to bound per-match engine history; output records grow with the requested batch.

The output keeps paired seat permutations and repeated combat seeds grouped by
map for descriptive bootstrap intervals. ROUND_LIMIT is censored, never relabelled
as a draw or a win based on territory. Decision deltas compare matching rounds;
changed future actions are legitimate consequences of the intervention. Invalid
policy orders abort the experiment rather than silently becoming losses. The
harness tests determinism, conservation, non-mutating planning, checkpoint
continuation, information boundaries and every command-line mode without a database.

## Internal recycling checkpoint: Spaceward 1.2.0-w3

`SPACEWARD_RECYCLING` has its own `RecyclingSpacewardTurnEngine`. W2 retains its
original reference. `MetalEconomy.recycling` is optional for older snapshots but
required, with complete system and player keys, in W3 current and replay state.
Explicit creation records the initial whole-galaxy material, including neutral
ships; restoration never synthesizes orders, fractions or a new balance.

`RecyclingOrders` validates ownership and the combined dispatch/recycling/reserve
budget. Single dispatch, batch edits, undo and reserve changes share the same
reservation. `GameService.planRecycling` validates the round and unsubmitted seat
inside `GameRegistry.writeState`; zero cancels or replaces the local order.
`Recycling.calculate` is pure fixed-point settlement. Seven of ten metal tenths
return per ship, with empire-wide fractions retained across rounds and reloads.
Only whole stock funds construction; split and bulk recycling conserve the same
material. `MetalProduction.forPlan` includes planned credits without mutation.
Actual production rejects unsettled recycling rather than crediting it twice.

The round pipeline resolves dispatch, recycling with structured report events,
mining/construction, relocations and arrivals in that order. The galaxy ledger
records combat losses after arrival resolution and recycling losses at settlement.
Its initial total equals remaining deposits, stock and fractions, surviving ships,
combat losses and recycling losses. All ships have the fixed one-metal W3 value.

Player views expose personal orders and fractions only with full visibility.
Recycling controls and the saved plan remain read-only in archive replay.
All eight AI profiles use the same filtered view and legal command validator;
rear-area recycling targets construction shortages while preserving eight
unreserved ships and avoiding known enemies within two travel rounds. W4 compares this trial policy; see `docs/ECONOMY-EXPERIMENTS.md`.

Restoration loads pending dispatches before validating resource reservations;
otherwise an overbooked recycling order could appear valid against an empty
order list. `RecyclingSnapshotTest` covers corrupt totals and combined budgets.

## Economy acceptance candidate: Spaceward 1.2.0-w4

`SPACEWARD_ECONOMY_PREVIEW` dispatches through its own provider and shares the
frozen W3 material formulas. `RulesetProperties.economyPreview` is false by default;
the `economy-preview` application profile enables only the current candidate.
The catalog always supports its saved reference, but creation requires the
opt-in catalog default. Older checkpoints remain creation-disabled. This is
an acceptance switch, not migration of existing snapshots or a public release.

The registry initializes industry, colony geology and resources before saving
any new candidate. Initial material includes all neutral and player ships.
`AiPolicy` subtracts mining points before validating expansion and calculating
fleet reserves; its legacy results without metal remain unchanged.

`strategyBenchmark --mode=economy` runs real production AI and round resolution
without Spring, a browser or audio. Experimental economic interventions share
CONCENTRATION fleet planning; they use filtered own-resource observations and
normal allocation/recycling commands. Rejected commands fail the experiment.
Paired environments isolate geology and growth. Every round validates exact
material conservation; snapshots are restored every 25 resolved rounds.
CSV evidence separates shortage capacity from unused labor capacity and records
initial metal and global irreversible losses. Reproducibility and interpretation
are documented in `docs/ECONOMY-EXPERIMENTS.md`.

## Economy totals in the final summary

`TurnEvent.EconomySummary` is appended once per participant and resolved Spaceward
round at the production step, before combat: metal actually extracted, metal booked
for ships (`ships × SHIP_COST`) and capacity gained from finished expansion, all
taken from the same `IndustrialProduction` results that `EconomyService.produce`
books. Zero summaries are stored so that missing history differs from a true zero.
The event adds no report line and is filtered from report lists while keeping the
original event indices used by battle acknowledgements. `EconomyTotals.of` sums the
summaries of the replay frames' resolved rounds and labels coverage as complete,
since a later round, partial or not recorded; SectorForces carries no economy.

## Economy candidate: Spaceward 1.2.0-w5

`SPACEWARD_DIRECT_ECONOMY` replaces W4 as `RulesetCatalog.ECONOMY_CANDIDATE`;
W4 is restoration-only like W1–W3. W5 keeps all W4 formulas but treats
population as atmosphere: `RulesetRef.limitsIndustryByLabor()` is false, so
`ColonyEconomy.usableCapacity(industry, false)` returns the installed capacity.
`GameState`, `MetalProduction.forPlan/calculate`, the player view and every AI
profile read this single rule; growth still changes population, never industry.
`ColonyView.laborLimited` carries the rule to the UI, which then shows neither
labor lines nor `EconomyIssues.Kind.LABOR`. Saved games are never re-labelled.

### Production drafts and authorized forecasts

`GameService.previewProduction` resolves the account's joined human seat and checks
access, round, ownership and submission under the registry read lock. It applies
the requested allocation to `GameState.copyOf`, then reuses `PlayerViewBuilder`
and `MetalProduction` for the local and empire-wide result. The original session,
orders and persisted snapshot remain untouched. `ProductionPlanPreview` contains
immutable industry and empire views; `EmpireStats.metal` is absent for non-metal
rules and unprivileged observer views.

`AllocationSplit` redistributes integer capacity shares while retaining pinned
values. `ProductionEditor` saves every completed input through
`GameService.commitProduction`; slider dragging only calls the copy-based preview.
`ProductionInputs` keeps the transient input, pins, opened editor, focused control
and the last save feedback per system of one map view; a server update always
replaces the input. `ProductionOutcome` turns an authorized forecast into points,
yield and actual ships, metal or progress. Pins are view-local and require no
snapshot migration. `MetalBalance.formattedChange` derives the header change from
`next - current`, so the rounded display matches the forecast stock.

`RelocationTrim` is the single rule for W5 relocation shortening: floor(old × limit /
total), remaining ships by largest remainder, ties by order id, zero removes the
route. `FleetService.trimRelocations` applies it with `routingCapacity` (expected
shipbuilding plus incoming routes) as limit. `DefaultGameService.allocate` runs the
production command and, only for `RulesetRef.adjustsRelocationsToShipbuilding()` and
a lower expected shipbuilding, the trim in the same write lock; the preview performs
the identical steps on its copy. `ProductionCommit` and `ProductionPlanPreview`
return the adjustments. Metal shortage alone never rewrites standing orders. Older
Spaceward references retain their two-way expansion/shipbuilding rule.

`Routes.rangeFrom` classifies every target from a permitted station as direct,
via stations or unreachable with the same `withinRange`, station and travel-edge
rules as route commands, and stores the straight-line distance as a share of the
range. `RangeShare` rounds down inside and up beyond the limit (one decimal near
100 %), so a target just outside never reads as 100 %. `GameService.rangeFor` is
read-only; `MapRenderer` draws `RangeOverlay` only during target selection in map
coordinates without click handling, and marks targets by outline, opacity,
`data-reach` and accessible text. SectorForces returns no range view.

Map interaction geometry is computed by `map-geometry.js` from the actual browser
zoom and rendered system diameter. Shared free lane intervals drive both visible
lines and interactive hit areas. Source, destination and intersecting systems
have protected circles; badges occupy free space, with the fleet list as fallback.
Pure segment and placement functions are covered by Node tests.

`BattleReplay.defenderSideId` projects the coalition containing the original owner
(or neutral member zero) into a presentation role, without exposing the subsequent
ownership transition. Missing roles in historical data remain explicitly unknown.
`CombatReadiness` derives deterministic, transient crew-condition buckets from
stored opening strength, never from survivors or the winner.
`ProductionFlow` also supplies the net retained production amount used by the
send dialog, on top of its already reserve-adjusted mode maximum.

## Equal metal candidate: Spaceward 1.2.0-w6

`SPACEWARD_EQUAL_METAL` is the new `RulesetCatalog.ECONOMY_CANDIDATE`.
W5 remains supported for restoration with proportional scarcity allocation;
W6 inherits its unrestricted installed industry and relocation shortening.
`MetalProduction` receives the complete stored rule reference for both live
and replay forecasts. Under W6, active yards receive equal shares, capped at
each yard's requested ships. Satisfied yards leave their share to the others.
Integer leftovers use system ID order. Mining is settled empire-wide first;
unused stock is retained, and separate empires never share stock.
`EqualMetalSpacewardTurnEngine` registers W6 in the ordinary round dispatcher.
No snapshot schema change or retroactive game conversion is needed.

The economy benchmark accepts `--rules=w3|w4|w5|w6|w7` and an optional
`--environments` list. `--suite=strategies` runs all eight production AI
profiles against every other profile with both seat assignments, plus two
rotating four-player groups. It uses the matching production round engine,
checks metal conservation every round and restores a snapshot every 25 rounds.
The legacy default remains W3. Raw evidence is flushed between environments
to bound memory use; commands and exact rule reference are saved with each run.


## Early industry candidate: Spaceward 1.2.0-w7

`SPACEWARD_EARLY_INDUSTRY` inherits W6 equal metal sharing, unrestricted installed
industry and relocation shortening. It is the default for new Spaceward games; W6 is also selectable.
All earlier stored versions remain restorable.
`EarlyIndustrySpacewardTurnEngine` registers its independent stored identity.

`EconomyRules.expansionCost(capacity, rules)` charges
`min(2*C, max(2, ceil(C*C/10)))` only for W7. The no-reference overload retains
the historical 2*C curve. `Industry.expandForOneTurn(rules)` spends accumulated
progress level by level, allowing multiple cheap levels. Capacity caps at 50;
new capacity contributes only next round. Capture retains progress as before.
`GameState.validateIndustry` checks saved progress against its stored rule price,
without changing snapshot shape. Live and replay `IndustryView` prices use the
same reference; AI reads those authorized prices. The editor's progress display
continues to describe the next level. No system-property multiplier is introduced.

Regression tests compare every valid capacity/allocation/progress combination
against the studied EARLY_COST model, check restored rules, historical prices,
multiple completions, capacity cap, next-round output and material conservation.


## Separate creation and restoration versions

`RulesetDefinition.creatableVersions` explicitly limits new-game references,
independently of `supportedVersions`. The built-in Spaceward entry permits only
W6 and W7, defaulting to W7; SectorForces keeps its existing entry and default.
`creationChoices()` returns the default first with stable ordering. Catalog
validation rejects unsupported choices and a non-creatable enabled default.
`requireCreatable` applies the same choices to registry creation. The retained
`economy-preview` profile and `builtIn(boolean)` compatibility overload no longer
gate availability. Earlier opt-in-profile descriptions above record past stages.

`RulesetSelector` keeps variant selection separate from a version radio group,
shown only when multiple versions can be created. Version changes notify the
wizard and update the selected card; switching variants resets to that variant's
default. Templates and rematches retain W6/W7 exactly. Older stored games still
load and continue, but `DefaultGameTemplateService` excludes their setups from
new templates. No snapshot migration or stored-rule rewrite is performed.


## Shared ship research: Spaceward 1.3.0-r1

R1 inherits W7 economics and navigation under its own stored rule reference and
round provider. `research-preview` enables creation via `RulesetCatalog.withShipResearch`;
normal W6/W7 creation is unchanged. The ordinary catalog still supports R1 for
restoration and round dispatch. No saved W6/W7 reference is migrated.

`ResearchEconomy` is an immutable optional component of `MetalEconomy`, which
already travels through snapshots, copies and replay frames. R1 requires exactly
one research record per player. Older rules require no research component.
`ShipResearch` stores a percentage, completed level and long progress, validates
cost bounds, carries remainders and stops at the initial level-20 cap. The square
cost curve is versioned in R1. Reservation is computed from installed industry,
rounded once per empire and allocated by largest remainder then system ID.
Reservations reduce the industry's usable capacity; `MetalProduction` exposes
the exact same reservation map for authorized live and historical forecasts.

`GameState.planShipResearch` clamps expansion before mining and updates planned
ship output. The application command identifies the seat from the account,
checks round/submission state and compares the expected previous percentage
under the game lock. Accepted changes publish `ProductionAllocationChanged`.
Relocations are trimmed after a budget change. Research advancement uses opening
industry and follows ship production: that round's ships keep the old level.
Expanded industry, new levels and changed ownership recompute next-round budgets.
`ResearchCompleted` is a stored, localized report event without a battle marker.

`ShipCohorts` stores immutable per-level hull counts in both `StarSystem` and
`Fleet`. Constructors check counts; missing legacy JSON defaults to level zero.
All navigation, return, dispatch, cancellation, docking, reinforcement, recycling
and survivor operations retain generations. Standing orders partition the same
newest-first selection without duplicating cohorts. Only research production can
add newly commissioned higher-level hulls. Recycling removes oldest hulls first;
casualties use proportional largest remainders. No material conversion uses
combat strength: hull counts, metal cost and recycling remain unchanged.

R1 coalition resolution uses summed generation strength per volley and each
side's actual surviving cohort mix; rolls and physical casualty limits remain
unchanged. The legacy resolver path receives no cohorts and keeps historical
math. Survivors are assigned to members and contingents by hull count, then to
generations by largest remainder. The UI projects composition only through fully
visible industry and own fleets; research progress and budgets are own-empire
views behind the account authorization boundary. Reviews use saved research and
ship generations, never the empire's later technology.

The map menu opens an ordinary Vaadin dialog; no frontend bundle, JavaScript,
CSS or tracked dependency manifests are changed. AI budgets use 10%, 5% for
RUSH, following the paired study in `RESEARCH-EXPERIMENTS.md`, through the same domain budget operation. Focused regression cases cover
completion timing, material conservation, coalition outcomes, multiple standing
routes, conquest budget changes, old JSON, account/stale-write rejection,
database reload and all AI profiles. These checks do not establish optimal balance.

Coalition reports store opening base strength separately from rolled strength.
Legacy reports default base strength to hull count; crew-condition text compares
against the technology-weighted base, so technology does not masquerade as crew
readiness. Per-volley strength uses the same member/contingent rounding as final
survivor allocation; generation strength is summed in integer tenths.
