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

## Game variants and immutable rule references

`RulesetRef` stores a stable variant ID and rule version independently of the
application release. Starfare implements SectorForces (after Galactic Empire, 1983) with two
variants. Classic ("Klassisch") uses
`classic / 1.0.0`, Extended ("Erweitert") `extended / 2.0.0`; the display names do
not change the stored identity. `RulesetCatalog` holds localized name and
description keys, supported versions and availability for new-game creation.
Each variant has exactly one supported version; the extended versions 1.0.0 to
1.3.0-r1 were removed with their turn engines, and their stored games no longer load
(`RetiredRulesPlatformTest`). Inside the platform host each variant has a
stage the platform keeps (games-platform-api 0.5.0; `variants=classic:PRODUCTION,
extended:ALPHA` in `game.properties` is the stage at first start): `RulesetAccess` asks
`PlatformAccounts.mayUseVariant` and marks alpha and beta variants on the rules card of the new-game dialog
(`variantStage`). An account that may not use a variant reaches none of its games, running
and finished ones included: `DefaultGameAccessPolicy` and the planning and submission
checks refuse map, review, chat and orders. `MatchSummary.variant` and `MatchEnded.variant`
carry `classic` or `extended`.

`RulesetConfiguration` supplies the shared catalog. `GameSetup`, `GameState`,
`GameStateSnapshot`, `GameSummary`, templates and compact result history retain
the reference. Missing legacy snapshot/setup references default to SectorForces;
explicit incomplete identities, unsupported versions and conflicting setup/state
references do not fall back. Session and archive stores leave rejected rows
untouched while continuing to load other games. Writes validate support before
mutating the registry state or saving a new game.

`RulesetTurnEngine` is the primary `TurnEngine` used by `GameService` and autoplay.
It dispatches by the full reference to registered `RulesetRoundImplementation`
beans. `DefaultTurnEngine` implements Classic 1.0.0 and keeps the frozen
Classic pipeline through `RoundPipeline`. `ExtendedTurnEngine` implements Extended
2.0.0 with the same order/arrival/victory sequence, its own AI planning and industrial
production. Every
supported reference needs an explicit registered round implementation; incomplete registrations fail during application startup.
The isolated third-variant tests verify extensibility without shipping another
game. Later variants may add their own commands, state and views as required.

Results and statistics belong to the platform. Starfare writes only the game
id and the time of the first end to `game_results`; the older result columns and
tables are no longer read and are dropped later, after a backup.

## Configurable victory and diplomacy

`VictoryRules` groups the individual system share, alliance permission, group
victory permission and separate group system share. `GameSetup` normalizes these
at creation; `GameState` rejects changes after start. Alliance permission gates
contracts, allied station access and diplomacy views. Disabling it forces group
victory off. Classic remains without diplomacy. `VictoryRules.forNewGame` fixes
every new game at 70%; Extended games always allow alliances, computer alliances
and an alliance victory at 70%. The new-game dialog offers no victory options.

`VictoryEvaluation` checks individual ownership first, then effective groups of at least two
members. Neutral systems remain in the denominator; integer comparisons avoid
rounding down. The strongest qualifying group wins, then the lowest group ID.
Every effective member wins, including zero-system members. Departures become
effective through the existing round pipeline before evaluation.

`GameOutcome` freezes all winning player IDs and the optional group ID. Legacy
`winnerId` remains populated for individual wins and is null for group wins.
Snapshots, replay frames, completion events and player views carry the full
outcome; victory/defeat reports preserve group membership at resolution time.
Old constructors and nullable JSON fields retain legacy read compatibility.
The platform learns every winner from `MatchEnded` (place 1).

`AllianceVictoryJourneyTest` covers both winners, reload and archive. The strategy
harness can pair Extended scenarios with group victory on/off; each member receives
its own win in the report.

## Extended navigation

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
consuming ships. Each launched Extended fleet stores its complete path,
current leg, flying/station/blocked phase, earliest departure and final ETA.
The fleet endpoints and arrival field describe its physical current leg.
Classic fleets keep the direct representation.

`RoundPipeline` applies commands, departs ready stationed fleets, launches
transfers and produces, applies waiting, and resolves arrivals. Owned
intermediate arrivals remain independent fleets and refuel for one full round;
final arrivals use ordinary reinforcement/combat. Arrival at a station already
lost uses ordinary combat and terminates the itinerary there. Loss while docked
blocks departure. A lost next intermediate station also blocks a new leg before departure. Post-combat access checks use the resolved round number, so
recapture cannot bypass a full refuelling round. Docked fleets do not contribute
to the system garrison; disbanding at an owned station explicitly merges them.

Snapshots, copying, archived states and replay frames retain the journey.
Classic saves have no navigation settings. Invalid itinerary/state combinations
are rejected. Arrival summaries count only final destinations, include waits and
exclude blocked fleets. UI cards explain route and phase, use unknown arrival
for blocked travel, and map lines follow actual legs. No rule-dependent state
leaks through foreign-system industry views.

## Extended diplomacy

The diplomacy module owns immutable treaty state, unanimous proposals and
application services. GameState keeps DiplomacyState independently of military
intelligence. Each empire belongs to at most one AllianceGroup; a lone remaining
member becomes contractless. Founding requires both partners, admission requires
all current members plus the applicant.
Overlapping votes are rejected. Roster changes discard stale electorates.

`VictoryRules.aiAlliancesAllowed` is fixed per game (setup, snapshot, templates);
new Extended games always set it, missing JSON reads as `true` through the
`@JsonCreator`, and the value is only effective while alliances are allowed. When it
is false,
`DefaultDiplomacyService.act` refuses FOUND with an AI party, JOIN by an AI and
APPROVE of such a proposal; REJECT and LEAVE stay available so existing
treaties of an AI-controlled seat can still be managed. `answerAiProposals` rejects
proposals that would bring in an AI. `DiplomacyView.aiAlliancesAllowed` lets the
dialog explain the blocked partners instead of offering them.

Every new group gets the fixed notice period `AllianceGroup.NOTICE_ROUNDS` (3).
Stored groups keep their saved period and stored `NOTICE` proposals stay readable;
they change nothing, are hidden and lapse at the next round start. A departure
takes effect at current turn plus max(1, notice), processed at the resolution
boundary before arrivals. Account-facing
commands run under the game write lock and verify the actual human seat, expected
turn and submission state. Views expose group contracts and only proposals in
which the account votes; they add no military intelligence. Read paths never
write. AI answers pending requests unless it has already announced departure,
and refrains from targeting allies. It does not initiate treaties.

Navigation permits allied intermediate stations. Allied final arrivals use the
PARTNER journey phase and retain independent fleet ownership; they cannot capture
the partner system or disband into its garrison. Combined defence and combat use the separate coalition path. A departing member's stationed and already inbound partner
contingents become non-combat returns. They automatically choose the shortest
currently permitted route to an owned system, or remain BLOCKED until one exists.
The first return leg may leave an inaccessible origin; subsequent stations obey
normal ownership/treaty/range checks. Return fleets cannot wait or disband and
never attack at an arrival, even if their destination is lost in flight.

Snapshots add nullable diplomacy and returningHome fields. Missing fields mean
no contracts and ordinary travel. Copying, persistence and archives retain
consent, deadlines and return phases. Invalid memberships, electorate data,
unknown participants and cross-variant treaty state are rejected. SectorForces
has no diplomacy actions.

## Extended industrial economy

Extended keeps immutable per-system `Industry` records in a separate
`GameState.industries` map. Classic retains an empty map and the existing
`StarSystem` behavior. Capacity starts at the configured system production.
Integer points go to expansion; the remainder builds ships.
`EconomyRules.expansionCost` prices the next level at `min(2*C, max(2, ceil(C*C/10)))`
for capacity C (cheap first levels, twice the capacity from 20 on), with a maximum of
`EconomyRules.MAX_CAPACITY` (50). `Industry.expandForOneTurn` spends accumulated
progress level by level, so several cheap levels may complete in one round; partial and
surplus progress survive allocation changes and conquest. At the cap, the allocation
returns to full shipbuilding. Growth changes output for the following turn only.

`StarSystem.productionPerTurn` always contains actual ship output, so existing
routing limits, delivery checks and relocation balances consume the same value.
Standing orders retained after an allocation change may use the source garrison, except the
ships reserved for a later joint start (`GameState.reservedShips`, read in
`DefaultFleetService.routeOneOrder`);
incoming rates never count as already delivered ships.
`DefaultEconomyService` produces only the output available before this turn's
growth. Capture resets allocation to full shipbuilding while
retaining industrial capacity and progress, directly in `GameState.updateSystem`.
Neutral systems never expand automatically.

`allocateExpansion` validates rules, owner, human seat, expected turn and
submission state under the game write lock. `ProductionAllocationChanged`
refreshes subscribed views. Economy data is optional at the snapshot boundary;
old Classic saves and replay frames remain valid. New frames store their own
industry map so historical views never read present-day progress. Invalid or
inconsistent industrial snapshots are rejected during restoration.

`PlayerViewBuilder` exposes an `IndustryView` only with full visibility. The
`IndustryPanel` in the system details shows the capacity, the expansion control and one
result line (ships per round, progress after the next round, rounds to the next level). Foreign sensor views and unresolved
battles do not reveal the industrial details. Observers and archives are read-only.
The BASELINE branch of `DefaultExtendedPlanning` uses filtered views, allocating one third of capacity
to expansion in safe systems and all output to ships near known enemies.

Extended 2.0.0 has no metal, mining, recycling, colonies, population or ship research;
expansion is the only economic decision.

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
| Production, routing, queued send and wait | Four systems have garrisons `6/6/12/7`, `6/11/14/7`, then `6/20/16/7`; neutral production does not grow its garrison, delayed and routed fleets merge on arrival. |
| Friendly arrivals and two equal attackers | Friendly fleets merge before combat. Production plus reinforcement gives 12 defenders; player 2 loses a 12:12 tie leaving zero defenders, then player 3 captures with 12 ships. |
| Victory and continuation | Three of four systems win at 70%. Further turns do nothing. Continuation requires full conquest; the next win records 100%, while the earlier replay still records 70%. |

`src/test/resources/classic/legacy-running.json` and `legacy-archive.json` are
hand-authored, synthetic old-format fixtures, not production exports. They
omit newer settings, `originalSetup`, previous orders and empire-name
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
`JpaGameSessionStore`. Read-only views must preserve
both the state snapshot and the database JSON/version. An absent completion
date stays absent, an absent visibility defaults to public, and missing
original settings do not become an invented game template.
The retained-archive case also exercises `GameService` review/replay and the
template service after the operative session is absent. It preserves archive
JSON and version, and cannot continue an archived game or invent a rematch.

Run the focused contract with `./gradlew test --tests '*ClassicTurnContractTest'
--tests '*ClassicSnapshotCompatibilityTest'`. Before changing Classic or
persistence, also run `./gradlew build`,
`node --test "src/test/frontend/*.test.mjs"` and `./gradlew e2eTest`.
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
  `ui` (Vaadin views: the entry route `LobbyView`, `MainView`, panels, dialogs,
  `UiMapper`, `UiTexts`, and `OwnFrameParts`, through which the views ask for the
  own start's lobby, language switch, account menu and legal footer).
- `turn` — Ruleset dispatch, shared `RoundPipeline` and planned joint-attack starts
  (`JointDepartures`).
- `combat` — Combat resolution (`CombatService`, `CombatResolver`).
- `fleet` — Fleet commands and validation (`FleetService`,
  `FleetOrder`).
- `economy` — Extended capacity, allocation and progress (`Industry`),
  production (`EconomyService`) and the immutable inspector (`IndustryView`, `IndustryPanel`).
- `diplomacy` — alliance groups, unanimous consent and round-based departures.
- `ai` — Classic and Extended AI decisions (`AiService`, `ExtendedPlanning`).
- `report` — Round reports (`TurnReport`, `TurnEvent`).
- `auth` — the signed-in player (`UserContext`) and display names
  (`PlayerDirectory`), both read from games-platform-api's `PlatformAccounts`.
  Game references use the immutable account ID; the public player name is
  presentation-only. `auth.config` holds the own start's security chain: no
  sign-in, every page open (plan Z3 b). Starfare's schema lives in `V2_x` under
  `db/migration`; the schema `identity` of the former zs-identity building block
  stays unread in old databases.
- `i18n` — `StarfareI18NProvider` (Vaadin `I18NProvider`),
  `I18n.t(...)` facade.
- `standalone` — Starfare's own start only (left out of the game artifact): the
  entry view and its answers to the platform interfaces (`standalone.platform`:
  the standard account everybody plays as without signing in, no friends, nobody
  else online, everybody visible). `standalone.ui` holds the parts a host's frame
  replaces: the lobby (`StandaloneLobby`), language switch, account menu, legal
  footer and `LocaleServiceInitListener`. `StandaloneFrameParts` hands them to the
  game's views as the bean `OwnFrameParts`; without it (inside a host) the views
  leave them out and the entry route `LobbyView` stays empty, since the host
  forwards it to its own game overview (`platformLobby=true` in `game.properties`).
- `legal` — Imprint and privacy information views.
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
2. `JointDepartures.launchDue` turns the planned joint-attack starts due this round
   (`GameState.scheduledDepartures`, `ScheduledDeparture`) into fleets at the same
   step as queued sends; then all queued orders (send, wait, disband) apply, ready
   stationed Extended fleets depart, and standing relocations launch.
3. Production on owned, non-neutral systems (Extended: industrial production and the
   `EconomySummary` event).
4. Apply `waitThisTurn` (arrival +1, clear flag).
5. Group all arrivals of the round by `toSystemId` + owner before any combat;
   reinforce the owner first, then resolve the attackers one after another via
   `CombatService`, each against the garrison left by the previous one. The
   order is a game rule (`RoundRules.attackOrder`): random (default) or
   strongest first. Extended resolves arrivals through the coalition path instead
   and refreshes station access afterwards.
6. Classic random events (`CosmicEvents`, only with `SectorOptions`, see below).
7. `JointDepartures.cancelLost` cancels planned starts whose source the planner no
   longer owns, with a `TurnEvent.JointDeparture` report line.
8. Victory check (>= the configured system share, neutrals included, destroyed
   systems excluded; default `GameConfig.VICTORY_SYSTEM_PERCENT`, 70%); optional group
   victory follows individual victory. It sends `Victory` to every winner and `Defeat`
   with the winning members to every other participant. Without a winner, `RoundLimit`
   warns during the last five rounds and ends a game with a round limit by `SectorScore`.
9. Replay frame capture and `state.nextTurn()`.

If `state.gameOver()` is true, `advanceTurn` is a no-op.

Joint attacks: `GameService.planJointAttack` validates the whole attack under the write
lock (`OrderPlanning.jointAttack`); the slowest sources become ordinary queued sends, faster ones a
`ScheduledDeparture` with a later departure round so that all arrive together. Until
then the ships stay in the garrison and defend it, but count as reserved for further
sends. At launch a start sends what is left; it is cancelled with a report line when
nothing is left, the source changed hands or the target is unreachable or destroyed.
A target that became the player's own or an ally's is still flown to. Without planned
starts both steps do nothing, so the frozen Classic results are unchanged. The UI sees
them as `PlayerViewState.plannedDepartures` (`PlannedDeparture`) in the Fleets tab and
can cancel them. The attack plan (`JointAttackPlan`, K9) sits above the details of a
selected target system in `FleetAndOrdersPanel` (`jointPlan`); `MainView` builds it from
`JointArrival` and keeps picked sources and typed amounts view-local, so map badges and
ticks pick the same sources and typing only updates the plan. While it is shown the map
offers no button of its own (`JointArrival.inSidebar`); a selected fleet keeps the button
and the confirmation dialog in `PlanningDialogs`. On phones `MobileMapLayout.halfSheet`
leaves the upper half of the map visible; the single undo step (`GameState.previousOrders` and
`previousDepartures`) covers them too.

`TurnEvent.Victory` stores the threshold at resolution time. Reports therefore
retain the correct threshold even after continuation or during archive replay;
JSON without this field defaults to the historical 70% rule.
`BattleReplay.Outcome` carries survivor counts and the explicit attacker result
from the event type. Playback colours use that result, including victories with
zero survivors on both sides, rather than comparing rounded ship counts.

`DefaultAutoplayRunner` resolves unattended AI games through
`GameRegistry.writeState`, so each turn is persisted under the session lock
and completion also creates the archive. It records the first end through
`GameEndRecorder` before publishing the final turn and completion events.

## SectorForces events and scoring (after Galactic Empire, 1983)

`GameSetup.sectorOptions` (`SectorOptions`: `EventFrequency` off/rare/normal, the switched-on
`CosmicEvent`s, start balance, round limit) exists only for SectorForces; other variants and
setups without the field normalize to `SectorOptions.none()`. It is part of the stored
`originalSetup`, so rules stay 1.0.0 and older snapshots read as off.

Runtime state lives in `GameState.cosmic` (`CosmicState`: home systems, unstable suns with
their explosion round, destroyed systems, `PirateFleet`s), a nullable snapshot component that
reads as `CosmicState.EMPTY`. `CosmicEvents` runs after the arrivals of a round, so every
remaining fleet is in hyperspace:

1. Due suns explode (`StarSystem.explode`: neutral, no ships, no production). The garrison
   leaves via `GameState.launchEventFleet` (owner) or as pirates (neutral, needs `PIRATES`).
2. Fleets and pirates bound for any destroyed system make a panic jump to a random system.
3. Landing pirates fight with `CombatResolver`; a win calls `StarSystem.fallToPirates`.
4. Each switched-on event rolls `SectorOptions.chance`: storm, space-time quake,
   disappearance, spaceport quake, and a new unstable sun (never a home) due next round.
   With rare events (`EventFrequency.RARE`) `SectorOptions.allowsSupernova` allows one
   unstable or exploded sun per game; the roll still happens, so the random sequence of
   the other events stays the same. Storm and space-time quake are reported only to
   players with a fleet in flight; a supernova goes to everyone.

Reports carry `TurnEvent.Cosmic` (kind `CosmicNews`) and, at the end, `TurnEvent.FinalScore`;
a victory by score is `TurnEvent.Victory` with `BY_SCORE` (0). Destroyed systems are no target:
`Routes.plan` and `DefaultFleetService` reject them, the baseline AI skips them, and
`PlayerViewState.sky` (`SkyMarks`) marks destroyed and unstable systems on the map regardless
of fog. `VictoryEvaluation` divides by `GameState.livingSystemCount`. `StartBalance` adjusts the
home garrisons after the galaxy is spaced out. `GameState.roundLimitReached` blocks continuing
after a victory and makes `StarfareMatches.result` rank the non-winners by `SectorScore` instead of
held systems.

## Round limits

A round ends once every human has submitted, or at the round deadline
(`GameState.roundDeadline`): the round limit counts from the start of the
round, the straggler limit from the moment only one human is still missing;
whichever comes first. A game with several humans picks a tempo in the new-game dialog
(`GameTempo`: Live 5 min / 1 min, Brisk 1 h / 15 min, Daily 24 h / 6 h, Relaxed
72 h / 12 h), which sets both limits and fixes them for the whole game; the
platform learns the tempo from `MatchSummary.tempo` and the levels from
`tempos` in `game.properties`. Games with fewer than two humans have no tempo
and no deadline; older games keep their stored limits, which the host may
still change. All state changes of a game run under the session's write lock,
so a deadline and the last submission give exactly one round change; the map
submits with the round it shows (`submitTurn(id, seat, expectedTurn)`), so a
click that arrives after the deadline ended that round is refused instead of
submitting the next one.
For the platform each person still to submit has an open decision `round-<n>`
due by that deadline (`MatchSummary.pendingDecisions`, games-platform-api 0.4.0).
Each treaty proposal that still lacks a person's vote adds an `ANSWER`
`round-<n>-answer-<proposal>` with the same deadline (`GameSummary.pendingAnswers`).
Proposals do not lapse in Starfare, but a vote is part of planning the round: the
answer disappears with the submission and comes back as a new decision in the next
round while the proposal stays open; a missed answer never counts as a missed turn.
`MatchEventPublisher` announces the start (`MatchStarted`), the open turns and
answers with a deadline (`DecisionsOpened`, again after each submission, lost seat
or treaty change, since the deadline may move earlier) and each missed deadline (`TurnMissed`, from
`GameEvent.RoundMissed`); `DefaultInvitationService` announces invitations
(`InvitationSent`). The platform turns them into notices and reminder mails.
The header uses **Submit turn** for human players and disables the action after
submission. `RoundStatus.hasSubmitted` and `pendingPlayerLabels` drive the
persistent **Submitted — waiting for …** notice below the header. Both derive
from the current server state, including after reload or a pushed round change.
AI-only spectators retain **Next turn** as an advance action.
`RoundDeadlineRunner` checks every `starfare.game.round-check-interval` (5 s).
At the deadline, late players move with the orders they had queued. `missedRounds`
counts per seat over the whole game and is never reset by submitting; at
`GameConfig.MAX_MISSED_ROUNDS` (2) the seat times out (`GameState.timedOut`): the AI
takes it for good. A timed-out seat stays apart from a resigned one (`resignedSeats`)
and from one that was left; both timed-out and resigned seats are `closedSeats()`,
which neither `claimSeat` nor `GameSummary.canJoin` reopens. The seat binding stays,
so the former player still reads chat and review. At the removal the seat keeps the place
it holds then (`GameState.keepTimedOutPlace`: one more than the players holding more systems,
computer opponents included; stored in `timedOutPlaces`). `StarfareMatches` reports that place
with `Departure.TIMED_OUT` (games-platform-api 0.4.0), so a computer opponent winning with the
seat is no win of the removed account.

## Lifecycle and lobby

A game exists while the registry holds it; aborting deletes it.
`GameState` carries `started` (turns running), `gameOver` and
`joinedHumanPlayerIds`. Older snapshots still contain an always-true `active`
field, which `GameStateSnapshot` ignores. `LobbyView` walks
`newGame → joinGame → canStartGame → startGame → MainView`.
`UserContext` provides the signed-in account id from `PlatformAccounts`.
`MainView.onAttach` redirects back to the lobby when no game is
running.

### Game visibility and archive

`GameAccessPolicy` centralizes visibility, spectator and completed-game access.
`GameVisibility` is independent of the social visibility preference. New states
are private; snapshots without the optional visibility field restore as public.
`visibleGamesFor(account, scope)` separates the active lobby from the archive;
`summaryFor` and `viewForAccount` enforce account access before returning UI data.
The lobby scope is one pass over the registry: summaries from memory (the parked
summary of an unloaded game), no archive lookups, and the variant rule asked once
per variant (`RulesetAccess.usableBy`). Within one HTTP request the list is read
once per account while `GameRegistry.version()` stays unchanged, because the
platform builds a page from several of Starfare's lists. `MatchDirectoryQueryBudgetTest`
keeps the cost independent of the number of games.
The UI resolves accounts from the authenticated session, not route parameters.

Only the current host may change visibility, and only before the start. Making
a game private removes unrelated spectator registrations. Invitations grant
lobby visibility and a reserved seat, but do not independently reveal the map.
After completion, invitations no longer grant private archive access.

`GameOutcome` carries winner and completion time to the archive. `finishedAt`
is set at the first game end and preserved by snapshots and copies; old archives
without the timestamp retain an unknown date. Archive opening calls `reviewFor`
and does not register spectators or save state. Map actions and service commands
reject edits after game end. At the first game end of a game with human
accounts, `GameEndRecorder` writes a row to `game_results` and announces
`MatchEnded` with the places in the same transaction; the platform keeps the
statistics. The row keeps a continued game from ending a second time.

Any recorded winning member may resume a completed game once. `resumeForFullConquest`
raises both victory thresholds to 100%, clears the outcome and submissions, and
restarts the round and straggler clocks without advancing the turn.
`GameContinued` refreshes every subscribed view and clears its outcome
acknowledgement and any open outcome dialog. A subsequent game end therefore
waits for its own pending battles before opening the result or review again.
The first end remains the reported one when play continues.

`JpaGameSessionStore` also writes a complete idempotent copy to `game_archives`.
`GameArchiveCleanupRunner` (daily, `starfare.game.archive-cleanup.enabled`, on by
default) deletes a game `starfare.game.archive-cleanup.retention` (90 days) after
its end: `game_sessions`, `game_archives` and the battle acknowledgements go together,
so the archive lists only what can still be reviewed. A game continued after its
victory runs again and stays. The deletion publishes no platform event; who played
whom and the result stay in the platform's statistics.
The platform lists an account's finished games from the same store
(`MatchDirectory.finishedMatchesOf`, games-platform-api 0.4.0): only games the
account held a seat in, latest end first (the `finished_at` of the first
archiving), each with its ranking from `StarfareMatches.result` and the map as
review link. A game continued after its victory runs again and is left out.
No snapshot is read for the list: when a game is archived, Starfare also stores
what the list shows (`ArchiveListing`): the columns `tutorial` and `aborted` of
`game_archives` and one row per seat holder in `game_archive_seats` (account, seat,
place, departure; indexed by account). The page is cut in SQL (offset, limit), its
seats come with a second query. Removing an account rewrites the listing together
with the snapshot. Archives stored before the listing (`listed = false`, migration
`V2_16`) are filled once when the store starts, because places and departures follow
the rules in Java; an unreadable archive is listed without seats.

`GameState.replayFrames` stores immutable systems, fleets and reports at game
start and after every resolved turn. It is optional in `GameStateSnapshot` so
older archives still load. During archive review, the map's timeline selects a
frame through `replayFor`; replay data is read-only and is rendered without fog.
The final timeline position returns to `reviewFor`, where participant perspective
and fog remain selectable.

### Map sidebar

`FleetAndOrdersPanel` is a view-local, switchable sidebar for Details, Fleets,
Relocations and Report. Details show the selected system or fleet; for a system out
of sight they show the last intelligence from fog-filtered `VisibleSystem` values, for
an own system its relocations and relocation balance (`ProductionFlow`). Fleets lists
fleets under way and the round's planned orders together, grouped by arrival round.
Report renders the player's turn events beside the map. The four sections are `Tabs`
at every width and on phones; above them `MainView` places the alliance and undo
actions. In Extended games an own system's details also carry the `IndustryPanel`. The
panel receives only `PlayerViewState` and the view-local map selection from `MainView`;
selecting a system or fleet never mutates the game. A planned joint attack or an edited
order selects Fleets, a resolved turn selects Report; the report stays in the sidebar.
The battle replay names the system's known production; acknowledging it opens no
further dialog.

Round start walkthrough: `RoundWalkthrough` (package `game.ui`) picks the located events
of the round report, i.e. battles (`TurnEvent.battleSite`), cosmic news with a system and
cancelled or reduced joint starts (`TurnEvent.JointDeparture`), plus storm and space-time
quake, which have no system (`RoundWalkthrough.walked`, `Step.NO_SYSTEM`); production,
reinforcements and other events without a place stay out. `RoundWalkthrough.marks`
derives the map marks of the open step from the player's own fleets (`MapMarks`: labels at
fleets and systems, the fleet to centre on): "−n" at a vanished fleet (at its target when the
whole fleet is gone) or a quaked spaceport, "+1" or the new arrival "Rn" at every own fleet in
flight, refugees and panic jumps highlighted, fleeing pirates named at the exploded system.
`MapRenderer` draws them (`map-event-mark`) and gives the focused fleet badge
`map-event-focus`, which the curtain spot follows like `sys-report-event-active`. `RoundWalkthrough.current` returns
the first one the account has not confirmed, with its position ("Event 2 of 5"). The
confirmations use the durable battle acknowledgements (`BattleAcknowledgements`, keyed
by account, game, round and original event index), so the progress survives reloads and
other devices. While a step is open `MainView` centres the map on its system, shows its
card on `RoundCurtain`, a layer in front of the whole map view (desktop and phone), and
locks planning: no target selection, sends, relocations or turn submission. The curtain
marks everything behind it `inert` and darkens it except a bright spot that a small
browser loop keeps on the event's system (`--spot-x`, `--spot-y`, `--spot-r`). A battle offers
its replay, every other event **Continue**. Spectators and the review are never held up;
a decided game opens its outcome only after the walkthrough.

Before the first round is resolved the same curtain holds the start card (`StartCard`):
the empire, its first system (the lowest own system id, lit like an event site) and the
number of opponents, of them computers. Its confirmation is stored as event 0 of round 0 in
the battle acknowledgements, which no report has, so no migration was needed; confirming
the first real event later removes it with the older rounds. In a tutorial the card adds
what a round consists of, and `TutorialGuide` beside **Submit turn** names the next step
from the selection, the target choice and the planned orders.

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

Viewing, chatting and giving orders are separate rights in `GameAccessPolicy`
(platform decision E2, 19.11.2026). `GameState.activeSeat(account)` is the seat through
which an account still plays: seated and joined. Leaving, resigning and losing the
seat to the computer keep the seat binding (`seatByUser`) but end the active seat.
`mayReadChat` needs a seat binding, so former players keep reading and observers do
not; `mayWriteChat` needs an active seat and a game that is not over (the waiting room
counts); `mayCommand` needs an active seat in a started, running game. Orders go
through `activeSeat`, never through the bare seat binding.
The platform asks the same chat rules through `MatchDirectory.chatAccess`
(games-platform-api 0.4.0): `GameChatService.mayWrite` gives `WRITE`, `mayRead`
`READ`, anything else `NONE`. `MainView` and `WaitingRoomView` implement `MatchView`
and name the game they show once `beforeEnter` let the account in, so the
platform can put the chat around them. Starfare shows and stores no chat itself
(since 0.2.13); 0.2.14 removed the code for the old table `game_chat_messages`,
and 0.2.15 dropped the table and its sequence (`V2_15`).

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
These static stylesheets (`starfare-navigation.css`, `starfare-wizard.css` and the phone
stylesheet) are loaded on entering a route of a `de.zettsystems.starfare` view and removed on
leaving it, so a platform host's own pages and other games never carry Starfare's rules.

Lobby, archive and dialogs share their components with the desktop
and adapt through that stylesheet. The map does not: `MainView` builds either the
`SplitLayout` with map and sidebar or `MobileMapLayout`, never both. The phone layout
reuses the same `MapCanvas`, `FleetAndOrdersPanel` and controls, arranges them as
full-screen map plus a sheet, and receives the target-selection state on every refresh.
`MapHeaderBar` has a compact variant without title and language switcher. The legend
lives only in the map help: the desktop's help panel above the map, on phones a
help button beside the zoom buttons (`MapLegend`). Game
logic and callbacks stay in `MainView`; only the arrangement differs.

## Configuration and setup

- Tunables: constants in `game.values.GameConfig`.
- Spring configs: `@ConfigurationProperties` (records or classes), never
  `@Value`. `@Configuration` classes live in the `<module>/config/`
  package (they fit neither `ui` nor `application/domain/values`).
- New game: `NewGameDialog` (opened from the lobby or `NewMatchView`) holds a `NewGamePanel`
  built from `SetupLayout` sections and `SetupCards` picture cards (radio groups with a
  `ComponentRenderer`); `SetupDetails` holds the Classic-only events and round limit.
  The panel yields a `GameSetup` (`game.values`); the dialog creates, joins, publishes
  unless invitation-only, and starts at once when every other seat is a computer.
  `GameSetup` keeps only what the dialog offers: seats, system count, highest neutral
  production, layout, round rules (tempo), ruleset, `ComputerStrength`, `RandomnessLevel`,
  enabled events, round limit and tutorial. `normalized()` clamps to 0–10 humans, at most
  10 participants, 8–120 systems (at least the participant count) and production 1–20;
  unknown fields of older JSON are ignored. Everything else is fixed for new games:
  home production 4 and 8 ships (`GameConfig.START_PRODUCTION/START_GARRISON`), neutral
  production bell-shaped from 1 to the chosen maximum, start balance always on, seat
  colours from the palette, observers, re-entry and battle presentation on, and
  `VictoryRules.forNewGame` (70%; Extended additionally alliances, alliance victory at 70%
  and computer alliances).
  `RandomnessLevel` maps to combat randomness (0/5/10/20%), `EventFrequency`
  (off/rare/normal/frequent ×2) and the attack order (strongest first only at None);
  rulesets that resolve coalition battles keep the default order.
  `ComputerStrength` picks each computer's `AiStrategy` (Easy baseline; Normal the calm
  `CAUTIOUS` profile in Classic and the baseline in Extended; Challenging the strongest
  benchmark profiles) and scales its home production and garrison (Classic 0.5/1.0/1.5,
  Extended 0.5/1.0/1.0); the values are named constants calibrated with the strategy bench
  (`docs/STRATEGY-EXPERIMENTS.md`). An explicit personal battle speed overrides the
  presentation default across rounds and sessions via the platform's settings.
  Only the winner's full-conquest continuation changes the victory threshold after the
  start, to 100%.
  `GameRegistry.createGame(GameSetup, hostPlayerId, name)` is the main
  entry point (the short overload builds a default, unnamed game).
- Galaxy size: `GalaxySize` offers 16/24/30/40 systems (sizes with fewer than two systems
  per player are locked). `GalaxyExtent.forSystems` keeps a fixed density of 240,000 px²
  per system at aspect ratio 1.6 (Small 2600×1670, Medium 3160×2020, Large 3510×2240,
  Huge 4040×2570); the extent is stored in `GameStateSnapshot.extent` (absent means the
  former 3200×2000). `GalaxyPlacement` and `GalaxyGenesis.spaceOut` place systems
  inside it, `PlayerViewState.extent` carries it to `MapCanvas`, `MapRenderer` and
  `map-geometry.js`, so "whole galaxy" and "own realm" fit any size. Travel uses one fixed
  speed, `GameState.roundsFor` = ceil(distance / 160 px) clamped to 2–40 rounds; nearest
  neighbours are usually three rounds apart, a crossing takes about 19/23/25/29 rounds.
  Extended's `RangeCalibration` measures the longest spanning-tree edge in map pixels and
  therefore adapts to every size without change.
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
- Language switcher (own start only): `standalone.ui.LanguageSwitcher` ComboBox
  in the lobby toolbar and the map header; stores the `Locale` in the
  `VaadinSession`, calls `ui.setLocale(...)` and reloads the page.
- `standalone.ui.LocaleServiceInitListener` (Vaadin `ServiceInitListener`)
  re-applies the session locale on every UI init, so the choice
  survives navigation and reload. Inside a host the platform decides the language.

## Friends, presence and invitations

Friends, presence, visibility and direct messages belong to the platform (plan
M13). The game asks only games-platform-api's `PlatformFriends` and
`PlatformPresence`; in a host the platform answers, in Starfare's own start
`standalone.platform` answers with no friends, nobody else online and everybody
visible. Starfare's former tables `friendships`, `direct_messages` and
`user_preferences` are no longer touched and are dropped by a later migration.

Invitations are stored as `GameState.invitedSeats` in the persisted
`game_sessions.state_json` snapshot, so a restart retains the seat reservation.
Inviting asks `PlatformPresence.canSee(host, invitee)`.

### Owner mechanics and invites

- `GameSession.hostPlayerId` is the owner; kick/abort/invite are
  owner-only.
- Auto-transfer on `leaveGame`: next human by seat ID. If there are
  neither humans nor observers left, the game plays itself out via
  `AutoplayRunner`.
- An invite reserves the seat (`GameState.invitedSeats`, persisted with the
  game session and reset in `resetForNewGame`). `canStartGame` blocks
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
- UI journeys run headless with Vaadin Browserless (`AbstractBrowserlessTest`) in the
  normal `test` task; Selenium/Cucumber (`e2eTest`) keeps only what needs a real
  browser. The core logic stays independent of the UI.
- Architecture check: `ModulithTest` verifies module boundaries on
  every `./gradlew test`.

## Command planning and personal display choices

`OrderPlanning` validates a complete replacement of one player's pending orders
under `GameRegistry.writeState`. Edits compare the displayed command with the
current index and use a turn token. A joint attack (`OrderPlanning.jointAttack`) rejects duplicate sources,
foreign systems, missing destinations and overcommitted garrisons before any
`GameState.previousOrders` stores one undo step per player, is part of the optional
snapshot fields, and is cleared by `nextTurn`. Undo revalidates the current garrisons.
`ArrivalPreview` combines fleets and queued sends by destination and arrival turn;
wait commands affect the existing fleet rather than adding a second arrival.

`GameState.originalSetup` retains the normalized starting setup. The optional
snapshot field is absent in older saves; those games cannot supply an exact
template. `GameTemplateService` restricts copying to hosts and participants,
generates a fresh private game, and optionally reserves the previous human seats.
Reservations do not join participants or start the game. Archived source games
use the same access check. For finished games the review offers the dialog
(`MainView.offerTemplate`, in `ReviewControls`), so both starts keep it.

`ArchiveView` lives in `standalone.ui` (plan S.3, 19.11.2026): only Starfare's own
start lists finished games; the game module has no `/starfare/archive` route and
`game.properties` no archive link. In a host, the platform will list finished
games and link to the review (plan S.4).

Display choices are kept by the platform (`PlatformSettings`, games-platform-api
0.4.0 and later) for the signed-in account under the game key `starfare`; the keys are the
names of `DisplaySetting`: speed, sound, event filters, route search,
sidebar position and whether the tutorial guide is hidden (`TUTORIAL_GUIDE`).
The new-game dialog keeps the choices of the account's last new game under the key
`create.setup` as one value `key=value;key=value` (`NewGameMemory`, at most 512
characters); `NewGamePanel.recall` checks every value against what the form offers now
and keeps the default where one no longer fits. `DisplayPreferences` reads them in one call while the map is
built and caches them in the session; each explicit changed choice is written at
once, and only its own key, so an open map never overwrites what another device
chose meanwhile (the fleet panel saves its two filters separately); keys of removed
settings that a platform still keeps are ignored; a blank one
resets the key (the platform keeps no blank values, so "no
event category" is stored as `-`). Reopening a map refreshes the cache. Without a
`PlatformSettings` bean the choices last for the session. Starfare's own start
keeps them in `user_display_settings` (Flyway V2_8, `SoloPlatformSettings`), which
a host takes over once; deleting an account still removes those rows
(`GameAccountDeletionListener`). These values never modify game setup or game state.

Map and sidebar rendering skip unchanged content. Route filters are stable Vaadin
components while their result containers are replaced independently. Custom click
targets use `KeyboardActions` with Enter/Space activation and visible focus styles.

## Extended coalition combat

RoundPipeline dispatches arrivals to CoalitionCombatService only for the fixed
Extended rules. Classic retains CombatService/CombatResolver and its
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
in map, system details and fleet cards, including after reload. Recorded participating
fleet IDs constrain concealment to actual combatants; protected blocked fleets
at the same location remain visible. Missing IDs in older event JSON default to
an empty immutable list. Existing durable
acknowledgements and final-outcome gating apply to the new battle subtype.
The independent frontend animation reads recorded results, reveals every side
concurrently and preserves the personal sound preference. SharedBrowser mutes
all browser acceptance tests.

## Creating Extended games

`RulesetCatalog.builtIn()` holds exactly one version per variant: `classic / 1.0.0` and
`extended / 2.0.0`; Classic stays first and therefore preselected in the new-game
dialog. `RulesetDefinition.creatableVersions` and `supportedVersions` both contain only
that version; `requireCreatable` applies the same choice to registry creation, and
`SetupCards.rules` shows one card per variant without a version number.
Templates and rematches use the current rules of their variant. `DefaultGameRegistry.createGame`
initializes industry and the calibrated range directly after galaxy generation, so lobby
views show the final Extended state before the start. `ExtendedReleaseIntegrationTest`
covers creation, joining, starting, regular round resolution and a rematch through the
public services against PostgreSQL; `RetiredRulesPlatformTest` checks that stored games
of the removed 1.x versions stay out of the platform's lists instead of failing them.


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
receives no mutable GameState. Classic and Extended engines dispatch each
AI seat independently; Extended still answers diplomacy proposals before planning.

`GameSetup.computerStrength` selects the profiles: `DefaultGameRegistry` draws each
computer's `AiStrategy` from `ComputerStrength.strategy` and stores it in
`Player.aiStrategy`, which existing JSON snapshots already carry. Missing fields
deserialize as the historical baseline. Player handovers retain the profile;
templates and rematches retain the strength and draw again. `NewGameDialogTest`
verifies ruleset switches, count changes, creation and the saved setup.

`Observation` exposes only PlayerViewBuilder's filtered state, diplomacy membership
and legal route durations. Alternative policies never receive GameState. The
baseline adapter invokes the existing AI on a private copy with only the current
seat marked as AI and no pending orders. Classic baseline only reads exact own
forces and distances to non-owned systems; Extended baseline already consumes
filtered views. Information-boundary tests perturb hidden forces and enemy orders.
Only the trusted simulator and result recorder inspect complete state.

Explicit --profiles selections in matrix and multiplayer mode are validated
against every chosen ruleset before any match. Historical default profile sets
remain unchanged. The shared `ai.domain.IndustryPolicy` supplies two allocations:
INDUSTRY_LIGHT and INDUSTRY_ADAPTIVE. They reuse INDUSTRY's military policy and
consume only own industry, available ships and route distance
to visible enemies. Unknown threats are not revealed. Integer allocation may be
zero on very small systems; no fractional growth or new economic rule is added.

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

## Industry editing and forecasts

`GameService.previewProduction` resolves the account's active seat and checks access,
round, ownership and submission under the registry read lock. It applies the requested
allocation to `GameState.copyOf`, then reuses `PlayerViewBuilder` for the local industry
and the empire. The original session, orders and persisted snapshot remain untouched.
`ProductionPlanPreview` carries the `IndustryView`, `EmpireStats` and the relocation
adjustments the change would cause.

`AllocationSplit` splits the integer capacity into the expansion share and the
shipbuilding rest. `ProductionEditor` shows the one always-visible expansion control and
saves every completed input through `GameService.commitProduction`; slider dragging only
calls the copy-based preview. `ProductionInputs` keeps the transient input, the focused
control and the last save feedback per system of one map view; a server update always
replaces the input. `IndustryPanel.result` renders ships per round, the expansion progress
after the next round and the rounds until the level completes.

`RelocationTrim` is the single rule for relocation shortening: floor(old × limit / total),
remaining ships by largest remainder, ties by order id, zero removes the route.
`FleetService.trimRelocations` applies it with `routingCapacity` (expected shipbuilding
plus incoming routes) as limit. `DefaultGameService` runs the production command and, only
when the expected shipbuilding dropped, the trim in the same write lock; the preview
performs the identical steps on its copy. `ProductionCommit` and `ProductionPlanPreview`
return the adjustments.

`TurnEvent.EconomySummary` is appended once per participant and resolved Extended round at
the production step, before combat: the capacity gained from finished expansion, taken
from the `IndustrialProduction` results that `EconomyService.produce` books. Zero
summaries are stored too. The event adds no report line and is filtered from report lists
while keeping the original event indices used by battle acknowledgements.
`EconomyTotals.of` sums one player's summaries over the replay frames' resolved rounds for
the game-end dialog ("Industry built"); Classic carries no economy.

## Range view, map geometry and battle presentation

`Routes.rangeFrom` classifies every target from a permitted station as direct, via stations
(with the number of stops of the fastest route) or unreachable, with the same
`withinRange`, station and travel-edge rules as route commands. `RangeView` carries no
distances. `GameService.rangeFor` is read-only; `MapRenderer` draws `RangeOverlay` only
during target selection in map coordinates without click handling, and marks targets by
outline, opacity, `data-reach` and the accessible texts "Directly reachable", "Via N
stations" and "No route". The send dialog uses the same texts. Classic returns no range
view.

Map interaction geometry is computed by `map-geometry.js` from the actual browser zoom and
rendered system diameter. Shared free lane intervals drive both visible lines and
interactive hit areas. Source, destination and intersecting systems have protected circles;
badges occupy free space, with the fleet list as fallback. Pure segment and placement
functions are covered by Node tests.

`BattleReplay.defenderSideId` projects the coalition containing the original owner (or
neutral member zero) into a presentation role, without exposing the subsequent ownership
transition. `CombatReadiness` derives deterministic, transient crew-condition buckets from
stored opening strength, never from survivors or the winner. `ProductionFlow` also
supplies the net retained production amount used by the send dialog, on top of its mode
maximum.
