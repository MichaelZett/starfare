package de.zettsystems.starfare.game.domain;

import de.zettsystems.starfare.diplomacy.values.DiplomacyState;
import de.zettsystems.starfare.economy.domain.Industry;
import de.zettsystems.starfare.fleet.values.FleetOrder;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.navigation.domain.RangeCalibration;
import de.zettsystems.starfare.navigation.domain.Routes;
import de.zettsystems.starfare.navigation.values.FlightJourney;
import de.zettsystems.starfare.navigation.values.NavigationSettings;
import de.zettsystems.starfare.report.values.TurnReport;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * Mutable in-memory game state for a single running match.
 */
@SuppressFBWarnings(value = "EI_EXPOSE_REP",
        justification = "GameState is the mutable aggregate root of a running match. Live collections "
                + "are exposed on purpose so application services can mutate them; concurrency is "
                + "serialized via GameSession's read/write lock.")
public class GameState {
    private @Nullable VictoryRules victoryRules;
    private List<Integer> groupWinners = List.of();
    private @Nullable Integer winningAlliance;
    public VictoryRules victoryRules() {
        return victoryRules == null ? VictoryRules.defaults(ruleset, victorySystemPercent) : victoryRules;
    }
    public void configureVictoryRules(VictoryRules rules) {
        if (started) { throw new IllegalStateException("Victory rules are fixed at creation"); }
        victoryRules = rules.forRuleset(ruleset);
        victorySystemPercent = victoryRules.individualSystemPercent();
    }
    public GameOutcome outcome() {
        List<Integer> individualWinners = winnerId == null ? List.of() : List.of(winnerId);
        return new GameOutcome(winnerId, finishedAt, winningAlliance == null ? individualWinners : groupWinners, winningAlliance);
    }
    public void winAsAlliance(int group, Set<Integer> winners) {
        if (!victoryRules().allianceVictoryAllowed() || winners.size() < 2
                || diplomacy.groupFor(winners.iterator().next()).filter(g -> g.id() == group && g.members().equals(winners)).isEmpty()) {
            throw new IllegalArgumentException("Victory must name an effective alliance");
        }
        if (gameOver) { return; }
        endGame(null);
        groupWinners = winners.stream().sorted().toList();
        winningAlliance = group;
    }

    private DiplomacyState diplomacy = DiplomacyState.EMPTY;
    public DiplomacyState diplomacy() { return diplomacy; }
    public boolean allied(int left, @Nullable Integer right) { return victoryRules().alliancesAllowed() && Routes.limited(this) && right != null && diplomacy.allied(left, right); }
    public void agreeTreaties(DiplomacyState agreed) {
        if (!Routes.limited(this) || !victoryRules().alliancesAllowed()) { throw new IllegalStateException("Diplomacy is disabled"); }
        diplomacy = agreed;
    }
    private void validateDiplomacy() {
        if ((!Routes.limited(this) || !victoryRules().alliancesAllowed()) && !diplomacy.equals(DiplomacyState.EMPTY)) {
            throw new IllegalArgumentException("Diplomacy belongs only to Spaceward");
        }
        var participants = players.stream().map(Player::id).collect(java.util.stream.Collectors.toSet());
        if (diplomacy.groups().stream().anyMatch(group -> !participants.containsAll(group.members()))
                || diplomacy.proposals().stream().anyMatch(proposal -> !participants.containsAll(proposal.voters()))) {
            throw new IllegalArgumentException("Treaty references an unknown empire");
        }
    }
    private @Nullable NavigationSettings navigation;
    public Optional<NavigationSettings> navigationSettings() { return Optional.ofNullable(navigation); }
    public void configureNavigation(NavigationSettings settings) {
        if (started || navigation != null || !ruleset.isSpaceward()) { throw new IllegalStateException("Navigation is fixed at creation"); }
        navigation = settings;
    }
    public void initializeNavigation() {
        if (ruleset.isSpaceward() && navigation == null) {
            navigation = RangeCalibration.forGalaxy(systems);
        }
    }
    public void replaceFleet(Fleet replacement) {
        for (int index = 0; index < fleets.size(); index++) {
            if (fleets.get(index).globalId() == replacement.globalId()) { fleets.set(index, replacement); return; }
        }
        throw new IllegalArgumentException("Unknown fleet " + replacement.globalId());
    }

    private int turn = 1;
    private RulesetRef ruleset = RulesetRef.SECTOR_FORCES;
    private final Map<Integer, Industry> industries = new HashMap<>();
    /** Seats whose player gave up for good: the computer keeps them and the player cannot reclaim them. */
    private final Set<Integer> resignedSeats = new HashSet<>();
    public Set<Integer> resignedSeats() { return resignedSeats; }
    /** Seat → the place it held when it timed out; reported with that departure when the game ends. */
    private final Map<Integer, Integer> timedOutPlaces = new HashMap<>();
    public Map<Integer, Integer> timedOutPlaces() { return timedOutPlaces; }
    /** Random-event state of SectorForces games: home systems, unstable suns, destroyed systems and pirates. */
    private CosmicState cosmic = CosmicState.EMPTY;
    public CosmicState cosmic() { return cosmic; }
    public void updateCosmic(UnaryOperator<CosmicState> change) { cosmic = change.apply(cosmic); }
    /** Whether a supernova destroyed the system; nothing can fly there any more. */
    public boolean destroyed(int systemId) { return cosmic.destroyed(systemId); }
    /** Systems that still exist; the victory threshold no longer counts destroyed ones. */
    public int livingSystemCount() { return systems.size() - cosmic.destroyedSystems().size(); }

    /**
     * A fleet that events create after this round's arrivals, such as refugees of a supernova. It leaves
     * with the coming round and arrives at {@code arrivalTurn}.
     *
     * @return the fleet's number for its owner
     */
    public int launchEventFleet(int ownerId, int from, int to, int ships, int arrivalTurn) {
        if (arrivalTurn <= turn + 1) { throw new IllegalArgumentException("This round's arrivals are already resolved"); }
        int localNo = nextLocalNoFor(ownerId);
        fleets.add(new Fleet(nextGlobalFleetId++, ownerId, localNo, from, to, ships, turn + 1, arrivalTurn));
        return localNo;
    }
    public long totalShips() {
        return systems.stream().mapToLong(StarSystem::garrison).sum() + fleets.stream().mapToLong(Fleet::ships).sum();
    }
    /** Ships the system builds next round; relocations and planning read this forecast. */
    public int expectedShipProduction(int systemId) {
        return getSystem(systemId).productionPerTurn();
    }

    public RulesetRef ruleset() { return ruleset; }
    public Map<Integer, Industry> industries() { return Map.copyOf(industries); }

    public void initializeIndustry() {
        if (!ruleset.isSpaceward() || !industries.isEmpty()) { return; }
        systems.forEach(system -> industries.put(system.id(), Industry.establish(system.productionPerTurn())));
    }

    public void planIndustrialExpansion(int systemId, Industry planned) {
        Industry current = industries.get(systemId);
        if (current == null || planned.capacity() != current.capacity()
                || planned.expansionProgress() != current.expansionProgress()) {
            throw new IllegalArgumentException("Allocation must preserve industrial capacity and progress");
        }
        updateSystem(systemId, system -> system.planShipbuilding(planned.shipbuilding()));
        industries.put(systemId, planned);
    }

    /** Builds with this round's split; expanded capacity becomes available from the next round. */
    public void completeIndustrialProduction(int systemId, Industry expanded, int routed) {
        Industry current = industries.get(systemId);
        if (current == null || !expanded.equals(current.expandForOneTurn())) {
            throw new IllegalArgumentException("Industrial growth must follow the current allocation");
        }
        int produced = current.shipbuilding();
        updateSystem(systemId, system -> system.completeShipbuilding(produced, routed, expanded.shipbuilding()));
        industries.put(systemId, expanded);
    }

    private void validateNavigation() {
        if (!Routes.limited(this) && (navigation != null || fleets.stream().anyMatch(f -> f.journey() != null))) {
            throw new IllegalArgumentException("Navigation state belongs only to Spaceward");
        }
        for (Fleet fleet : fleets) {
            var journey = fleet.journey();
            if (journey == null) { continue; }
            if (journey.stations().stream().anyMatch(id -> systems.stream().noneMatch(system -> system.id() == id))
                    || fleet.fromSystemId() != journey.stations().get(journey.legIndex() - 1)
                    || fleet.toSystemId() != journey.stations().get(journey.legIndex())
                    || fleet.ships() <= 0 || journey.finalArrivalTurn() < fleet.arrivalTurn()) {
                throw new IllegalArgumentException("Fleet and itinerary disagree");
            }
            for (int leg = 1; leg < journey.stations().size(); leg++) {
                if (!Routes.withinRange(this, journey.stations().get(leg - 1), journey.stations().get(leg))) {
                    throw new IllegalArgumentException("Stored itinerary exceeds the fixed flight range");
                }
            }
        }
    }

    private void validateIndustry() {
        if (!ruleset.isSpaceward()) {
            if (!industries.isEmpty()) { throw new IllegalArgumentException("Industry belongs only to Spaceward"); }
            return;
        }
        if (industries.size() != systems.size() || systems.stream().anyMatch(system -> {
            Industry industry = industries.get(system.id());
            return industry == null || industry.shipbuilding() != system.productionPerTurn()
                    || (system.neutral() && industry.expansionAllocation() != 0);
        })) { throw new IllegalArgumentException("System industry and ship production disagree"); }
    }
    private final List<Player> players = new ArrayList<>();
    private final List<StarSystem> systems = new ArrayList<>();
    private final List<Fleet> fleets = new ArrayList<>();
    private final Map<Integer, TurnReport> reports = new HashMap<>();
    private final Map<Integer, Map<Integer, Intel>> intel = new HashMap<>();
    private final Map<Integer, List<SystemOwnership>> ownershipHistory = new HashMap<>();
    private final Map<Integer, ReplayFrame> replayFrames = new HashMap<>();
    private int nextGlobalFleetId = 1;
    private final Map<Integer, Integer> nextLocalFleetNo = new HashMap<>();
    // neu
    private final Set<Integer> waitThisTurn = new HashSet<>();
    private final Set<Integer> submittedThisTurn = new HashSet<>();
    private boolean gameOver;
    private GameVisibility visibility = GameVisibility.PRIVATE;
    private @Nullable Instant finishedAt;

    public GameVisibility visibility() { return visibility; }
    public @Nullable Instant finishedAt() { return finishedAt; }
    public void publishInLobby() { visibility = GameVisibility.PUBLIC; }
    public void makePrivate() { visibility = GameVisibility.PRIVATE; }

    private @Nullable Integer winnerId;
    private boolean started;
    private final Set<Integer> joinedHumanPlayerIds = new HashSet<>();
    private final Set<Integer> originalHumanPlayerIds = new HashSet<>();
    private final Set<String> observers = new HashSet<>();
    private final Map<String, Integer> seatByUser = new HashMap<>();
    /** Player id → reserved seat id. Persisted as part of the game session snapshot. */
    private final Map<String, Integer> invitedSeats = new HashMap<>();
    private final Map<Integer, List<FleetOrder>> pendingOrders = new HashMap<>();
    private final Map<Integer, List<FleetOrder>> previousOrders = new HashMap<>();
    private @Nullable GameSetup originalSetup;
    private GalaxyExtent extent = GalaxyExtent.LEGACY;

    public Optional<GameSetup> originalSetup() { return Optional.ofNullable(originalSetup); }

    /** Size of this galaxy's map; fixed when the game is created, older games keep the earlier fixed map. */
    public GalaxyExtent extent() { return extent; }

    /** SectorForces options of this game; games without them, or other variants, have everything off. */
    public SectorOptions sectorOptions() {
        return originalSetup == null ? SectorOptions.none() : originalSetup.sectorOptions();
    }

    /** Whether the game was created as a tutorial; games from before the choice existed are none. */
    public boolean tutorial() {
        return originalSetup != null && originalSetup.tutorial();
    }

    /** Whether the round limit of a SectorForces game is reached; such a game cannot be continued. */
    public boolean roundLimitReached() {
        SectorOptions options = sectorOptions();
        return ruleset.usesSectorOptions() && options.hasRoundLimit() && turn >= options.roundLimit();
    }

    public void rememberSetup(GameSetup setup) {
        if (started || originalSetup != null) {
            throw new IllegalStateException("Game rules are fixed at creation");
        }
        originalSetup = setup;
        ruleset = setup.ruleset();
        extent = GalaxyExtent.forSystems(setup.systemCount());
        configureVictoryRules(setup.victoryRules());
    }

    /** One reversible command change per player, valid only within the current round. */
    public void rememberOrders(int player) {
        previousOrders.put(player, List.copyOf(pendingOrders.getOrDefault(player, List.of())));
    }

    public boolean undoOrders(int player) {
        List<FleetOrder> previous = previousOrders.get(player);
        if (previous == null || !de.zettsystems.starfare.fleet.domain.OrderPlanning.valid(this, player, previous)) {
            return false;
        }
        pendingOrders.put(player, new ArrayList<>(previous));
        previousOrders.remove(player);
        return true;
    }
    private final Map<Integer, List<StandingOrder>> standingOrders = new HashMap<>();
    private final Map<Integer, Integer> nextStandingOrderId = new HashMap<>();
    private boolean observersAllowed;
    private boolean reentryAllowed;
    private boolean battlePresentationEnabled = GameConfig.DEFAULT_BATTLE_PRESENTATION_ENABLED;
    private int combatRandomnessPercent = GameConfig.DEFAULT_COMBAT_RANDOMNESS_PERCENT;
    private int victorySystemPercent = GameConfig.VICTORY_SYSTEM_PERCENT;
    private Instant turnStartedAt = Instant.now();
    private RoundRules roundRules = RoundRules.defaults();
    /** Seit wann nur noch ein Mensch fehlt; {@code null}, solange die Nachzügler-Uhr nicht läuft. */
    private @Nullable Instant stragglerSince;
    /** Spieler-ID → verpasste Rundenfristen in Folge. */
    private final Map<Integer, Integer> missedRounds = new HashMap<>();

    public int turn() {
        return turn;
    }

    public List<Player> players() {
        return players;
    }

    public List<StarSystem> systems() {
        return systems;
    }

    public List<Fleet> fleets() {
        return fleets;
    }

    public Map<Integer, TurnReport> reports() {
        return reports;
    }

    public Map<Integer, Map<Integer, Intel>> intel() {
        return intel;
    }

    /** Ownership periods, oldest first, retained for system details and game review. */
    public Map<Integer, List<SystemOwnership>> ownershipHistory() {
        return ownershipHistory;
    }

    /** Turn snapshots used solely by the read-only replay timeline. */
    public Map<Integer, ReplayFrame> replayFrames() {
        return replayFrames;
    }

    public Set<Integer> waitThisTurn() {
        return waitThisTurn;
    }

    public Set<Integer> submittedThisTurn() {
        return submittedThisTurn;
    }

    public boolean gameOver() {
        return gameOver;
    }

    public @Nullable Integer winnerId() {
        return winnerId;
    }

    public boolean started() {
        return started;
    }

    /**
     * Marks this game as started so turns may be advanced.
     */
    public void start() {
        initializeIndustry();
        initializeNavigation();
        this.started = true;
        this.turnStartedAt = Instant.now();
        this.stragglerSince = null;
        captureReplayFrame();
    }

    public Set<Integer> joinedHumanPlayerIds() {
        return joinedHumanPlayerIds;
    }

    public Set<Integer> originalHumanPlayerIds() {
        return originalHumanPlayerIds;
    }

    public Set<String> observers() {
        return observers;
    }

    /** Releases identity bindings without removing other players or their game. */
    public boolean releaseAccount(String account) {
        Integer seat = seatByUser.remove(account);
        boolean changed = observers.remove(account);
        changed |= invitedSeats.remove(account) != null;
        if (seat != null) {
            joinedHumanPlayerIds.remove(seat);
            originalHumanPlayerIds.remove(seat);
            submittedThisTurn.remove(seat);
            pendingOrders.remove(seat);
            standingOrders.remove(seat);
            updatePlayer(seat, Player::asAi);
            changed = true;
        }
        return changed;
    }

    /**
     * The seat through which the account still plays: seated and joined. Whoever left, resigned or lost the
     * seat to the computer keeps the seat binding (for reading the chat and the review) but has no active seat.
     */
    public @Nullable Integer activeSeat(String account) {
        Integer seat = seatByUser.get(account);
        return seat != null && joinedHumanPlayerIds.contains(seat) ? seat : null;
    }
    public Map<String, Integer> seatByUser() {
        return seatByUser;
    }

    public Map<String, Integer> invitedSeats() {
        return invitedSeats;
    }

    public Map<Integer, List<FleetOrder>> pendingOrders() {
        return pendingOrders;
    }

    public Map<Integer, List<StandingOrder>> standingOrders() {
        return standingOrders;
    }

    public int nextStandingOrderIdFor(int playerId) {
        int n = nextStandingOrderId.getOrDefault(playerId, 0) + 1;
        nextStandingOrderId.put(playerId, n);
        return n;
    }

    public boolean observersAllowed() {
        return observersAllowed;
    }

    public boolean reentryAllowed() {
        return reentryAllowed;
    }

    public boolean battlePresentationEnabled() {
        return battlePresentationEnabled;
    }

    public int combatRandomnessPercent() {
        return combatRandomnessPercent;
    }

    public int victorySystemPercent() { return victorySystemPercent; }

    public void configureVictorySystemPercent(int percent) {
        victorySystemPercent = Math.clamp(percent, GameConfig.MIN_VICTORY_SYSTEM_PERCENT,
                GameConfig.MAX_VICTORY_SYSTEM_PERCENT);
        if (victoryRules != null) { victoryRules = victoryRules.withIndividualPercent(victorySystemPercent); }
    }

    public Instant turnStartedAt() {
        return turnStartedAt;
    }

    public RoundRules roundRules() {
        return roundRules;
    }

    public void configureRoundRules(RoundRules rules) {
        this.roundRules = rules;
    }

    public @Nullable Instant stragglerSince() {
        return stragglerSince;
    }

    /**
     * Whether the seat timed out: it missed {@link GameConfig#MAX_MISSED_ROUNDS}
     * rounds in this game, counted over the whole game, not only in a row. A timed-out seat stays with the
     * computer for good, unlike a seat that was left and may be taken back.
     */
    public boolean timedOut(int seat) {
        return missedRounds.getOrDefault(seat, 0) >= GameConfig.MAX_MISSED_ROUNDS;
    }

    /**
     * Keeps the place the seat holds now for its timed-out departure: one more than the number of
     * players holding more systems, computer opponents included. A place kept earlier stays.
     */
    public void keepTimedOutPlace(int seat) {
        Map<Integer, Long> held = systems.stream().map(StarSystem::ownerId).filter(Objects::nonNull)
                .collect(Collectors.groupingBy(owner -> owner, Collectors.counting()));
        long own = held.getOrDefault(seat, 0L);
        long better = players.stream().filter(player -> held.getOrDefault(player.id(), 0L) > own).count();
        timedOutPlaces.putIfAbsent(seat, 1 + (int) better);
    }

    /** Seats nobody may take back: resigned (RESIGNED) or timed out (TIMED_OUT), kept apart from a seat that was left. */
    public Set<Integer> closedSeats() {
        Set<Integer> closed = new HashSet<>(resignedSeats);
        missedRounds.keySet().stream().filter(this::timedOut).forEach(closed::add);
        return Set.copyOf(closed);
    }

    /** Missed rounds per seat over the whole game. */
    public Map<Integer, Integer> missedRounds() {
        return missedRounds;
    }

    /** Menschen, die in dieser Runde noch nicht abgegeben haben. */
    public Set<Integer> pendingHumanPlayerIds() {
        Set<Integer> pending = new HashSet<>(joinedHumanPlayerIds);
        pending.removeAll(submittedThisTurn);
        return pending;
    }

    /**
     * Startet die Nachzügler-Uhr, sobald nur noch ein Mensch fehlt, und hält sie sonst an.
     * Nach jeder Abgabe und jedem Wechsel der Mitspieler aufrufen.
     */
    public void updateStragglerClock(Instant now) {
        if (!timed() || pendingHumanPlayerIds().size() != 1) {
            stragglerSince = null;
        } else if (stragglerSince == null) {
            stragglerSince = now;
        }
    }

    /**
     * Wann die laufende Runde spätestens endet: Rundenlimit ab Rundenbeginn oder, falls früher,
     * Nachzügler-Limit ab dem Moment, in dem nur noch einer fehlt. Mit weniger als zwei
     * Menschen läuft keine Frist.
     */
    public @Nullable Instant roundDeadline() {
        if (!timed()) {
            return null;
        }
        Instant roundEnd = turnStartedAt.plus(roundRules.roundLimit());
        Instant since = stragglerSince;
        // Steigt jemand wieder ein, fehlen erneut mehrere: dann gilt nur das Rundenlimit.
        if (since == null || pendingHumanPlayerIds().size() != 1) {
            return roundEnd;
        }
        Instant stragglerEnd = since.plus(roundRules.stragglerLimit());
        return stragglerEnd.isBefore(roundEnd) ? stragglerEnd : roundEnd;
    }

    private boolean timed() {
        return started && !gameOver && joinedHumanPlayerIds.size() >= 2;
    }

    /**
     * Applies the lobby-level policies (observer access, re-entry of dropped humans).
     */
    public void configureLobby(boolean observersAllowed, boolean reentryAllowed) {
        configureLobby(observersAllowed, reentryAllowed, GameConfig.DEFAULT_BATTLE_PRESENTATION_ENABLED);
    }

    /** Applies lobby policies and the default presentation mode for each new round report. */
    public void configureLobby(boolean observersAllowed, boolean reentryAllowed, boolean battlePresentationEnabled) {
        configureLobby(observersAllowed, reentryAllowed, battlePresentationEnabled,
                GameConfig.DEFAULT_COMBAT_RANDOMNESS_PERCENT);
    }

    /** Applies lobby policies, report presentation and the fixed combat randomness for this game. */
    public void configureLobby(boolean observersAllowed, boolean reentryAllowed, boolean battlePresentationEnabled,
                                int combatRandomnessPercent) {
        this.observersAllowed = observersAllowed;
        this.reentryAllowed = reentryAllowed;
        this.battlePresentationEnabled = battlePresentationEnabled;
        this.combatRandomnessPercent = Math.clamp(combatRandomnessPercent,
                GameConfig.MIN_COMBAT_RANDOMNESS_PERCENT, GameConfig.MAX_COMBAT_RANDOMNESS_PERCENT);
    }

    /**
     * Ends the game, optionally recording a winner (null = abort/draw).
     */
    public void endGame(@Nullable Integer winnerId) {
        endGame(winnerId, Instant.now());
    }

    public void endGame(@Nullable Integer winnerId, Instant finishedAt) {
        if (this.gameOver && (winningAlliance != null || ruleset.isSpaceward())) { return; }
        if (!this.gameOver) { this.finishedAt = finishedAt; }
        this.gameOver = true;
        this.winnerId = winnerId;
        this.groupWinners = List.of();
        this.winningAlliance = null;
    }

    public void clearGameOver() {
        this.gameOver = false;
        this.finishedAt = null;
        this.winnerId = null;
        this.groupWinners = List.of();
        this.winningAlliance = null;
    }

    /**
     * Whether every victory path already requires all systems, e.g. after a continuation; resuming
     * again could not change the goal, so the winner is not offered it.
     */
    public boolean requiresFullConquest() {
        VictoryRules rules = victoryRules();
        return victorySystemPercent >= GameConfig.MAX_VICTORY_SYSTEM_PERCENT
                && (!rules.allianceVictoryAllowed() || rules.allianceSystemPercent() >= GameConfig.MAX_VICTORY_SYSTEM_PERCENT);
    }

    /** Starts a fresh round deadline when the winner resumes play towards full conquest. */
    public void resumeForFullConquest(Instant now) {
        victoryRules = ruleset.isSpaceward() ? victoryRules().forFullConquest()
                : VictoryRules.defaults(ruleset, GameConfig.MAX_VICTORY_SYSTEM_PERCENT);
        configureVictorySystemPercent(GameConfig.MAX_VICTORY_SYSTEM_PERCENT);
        clearGameOver();
        turnStartedAt = now;
        // Every joined human must submit again, so no straggler clock can be running.
        stragglerSince = null;
        submittedThisTurn.clear();
    }

    public void resetForNewGame() {
        makePrivate();
        this.turn = 1;
        this.nextGlobalFleetId = 1;
        this.nextLocalFleetNo.clear();
        this.waitThisTurn.clear();
        this.submittedThisTurn.clear();
        this.joinedHumanPlayerIds.clear();
        this.originalHumanPlayerIds.clear();
        this.observers.clear();
        this.seatByUser.clear();
        this.invitedSeats.clear();
        this.pendingOrders.clear();
        this.previousOrders.clear();
        this.originalSetup = null;
        this.extent = GalaxyExtent.LEGACY;
        this.victoryRules = null;
        this.groupWinners = List.of();
        this.winningAlliance = null;
        this.ruleset = RulesetRef.SECTOR_FORCES;
        this.standingOrders.clear();
        this.nextStandingOrderId.clear();
        this.observersAllowed = false;
        this.reentryAllowed = false;
        this.battlePresentationEnabled = GameConfig.DEFAULT_BATTLE_PRESENTATION_ENABLED;
        this.combatRandomnessPercent = GameConfig.DEFAULT_COMBAT_RANDOMNESS_PERCENT;
        this.victorySystemPercent = GameConfig.VICTORY_SYSTEM_PERCENT;
        this.roundRules = RoundRules.defaults();
        this.stragglerSince = null;
        this.missedRounds.clear();
        this.players.clear();
        this.systems.clear();
        this.industries.clear();
        resignedSeats.clear();
        timedOutPlaces.clear();
        cosmic = CosmicState.EMPTY;
        navigation = null;
        diplomacy = DiplomacyState.EMPTY;
        this.fleets.clear();
        this.reports.clear();
        this.intel.clear();
        this.ownershipHistory.clear();
        this.replayFrames.clear();
        clearGameOver();
        this.started = false;
    }

    public int nextLocalNoFor(int playerId) {
        int n = nextLocalFleetNo.getOrDefault(playerId, 0) + 1;
        nextLocalFleetNo.put(playerId, n);
        return n;
    }

    public int addFleet(int ownerId, int from, int to, int ships) {
        int travelRounds = travelRounds(from, to);        // unverändert
        FlightJourney journey = null;
        int firstTarget = to;
        if (Routes.limited(this)) {
            var route = Routes.plan(this, ownerId, from, to).orElseThrow();
            firstTarget = route.systems().get(1);
            travelRounds = travelRounds(from, firstTarget);
            journey = new FlightJourney(route.systems(), 1,
                    FlightJourney.Phase.FLYING, 0, turn + route.rounds());
        }
        int localNo = nextLocalNoFor(ownerId);
        Fleet f = new Fleet(nextGlobalFleetId++, ownerId, localNo, from, firstTarget, ships, turn, turn + travelRounds, journey);
        fleets.add(f);
        return f.globalId();
    }

    public double distance(int fromId, int toId) {
        StarSystem a = byId(fromId);
        StarSystem b = byId(toId);
        return Math.hypot(a.x() - b.x(), a.y() - b.y());
    }

    public int travelRounds(int fromId, int toId) {
        return roundsFor(distance(fromId, toId));
    }

    /**
     * IDs aller Systeme, die von mindestens einer der Quellen in hoechstens
     * {@code maxRounds} Reiserunden erreichbar sind (Quellen eingeschlossen).
     * Berechnet Bounding-Box und System-Index nur einmal statt je Paar.
     */
    public Set<Integer> systemsWithinRounds(Collection<Integer> sourceSystemIds, int maxRounds) {
        if (sourceSystemIds.isEmpty()) {
            return Set.of();
        }
        Map<Integer, StarSystem> index = new HashMap<>();
        for (StarSystem s : systems) {
            index.put(s.id(), s);
        }
        List<StarSystem> sources = sourceSystemIds.stream().map(index::get).filter(Objects::nonNull).toList();
        Set<Integer> reachable = new HashSet<>();
        for (StarSystem target : systems) {
            for (StarSystem source : sources) {
                double d = Math.hypot(source.x() - target.x(), source.y() - target.y());
                if (roundsFor(d) <= maxRounds) {
                    reachable.add(target.id());
                    break;
                }
            }
        }
        return reachable;
    }

    /** Every map flies at the same speed; a larger galaxy is larger, not faster. */
    private static int roundsFor(double distance) {
        return Math.clamp((int) Math.ceil(distance / GameConfig.TRAVEL_SPEED),
                GameConfig.MIN_TRAVEL_ROUNDS, GameConfig.MAX_TRAVEL_ROUNDS);
    }

    public StarSystem byId(int id) {
        return systems.stream().filter(s -> s.id() == id).findFirst().orElseThrow();
    }

    public StarSystem getSystem(int id) {
        return byId(id);
    }

    /** Looks up a system by an id from outside, such as an order: empty if this map has no such system. */
    public Optional<StarSystem> findSystem(int id) {
        return systems.stream().filter(s -> s.id() == id).findFirst();
    }

    public void updateSystem(int id, UnaryOperator<StarSystem> updater) {
        for (int i = 0; i < systems.size(); i++) {
            StarSystem current = systems.get(i);
            if (current.id() != id) { continue; }
            StarSystem updated = updater.apply(current);
            boolean ownerChanged = !Objects.equals(current.ownerId(), updated.ownerId());
            if (ownerChanged) { updated = captureIndustry(id, updated); }
            systems.set(i, updated);
            recordOwnershipChange(current, updated);
            return;
        }
        throw new IllegalArgumentException("System not found: " + id);
    }

    /** Conquest keeps capacity and progress; the new owner starts with the whole capacity on shipbuilding. */
    private StarSystem captureIndustry(int id, StarSystem updated) {
        Industry captured = industries.computeIfPresent(id, (_, industry) -> industry.captured());
        return captured == null ? updated : updated.planShipbuilding(captured.shipbuilding());
    }

    private void recordOwnershipChange(StarSystem before, StarSystem after) {
        if (Objects.equals(before.ownerId(), after.ownerId())) {
            return;
        }
        ownershipHistory.computeIfAbsent(after.id(), _ -> new ArrayList<>())
                .add(new SystemOwnership(after.ownerId(), turn));
    }

    public void updatePlayer(int id, UnaryOperator<Player> updater) {
        for (int i = 0; i < players.size(); i++) {
            Player current = players.get(i);
            if (current.id() == id) {
                players.set(i, updater.apply(current));
                return;
            }
        }
        throw new IllegalArgumentException("Player not found: " + id);
    }

    public void nextTurn() {
        previousOrders.clear();
        this.turn++;
        this.turnStartedAt = Instant.now();
        this.stragglerSince = null;
    }

    /** Captures the resolved state of the current turn without keeping mutable collections. */
    public void captureReplayFrame() {
        replayFrames.put(turn, new ReplayFrame(turn, systems, fleets, reports, industries, outcome()));
    }

    public record Intel(@Nullable Integer ownerId, int turn, @Nullable Integer garrison) {
        public Intel(@Nullable Integer ownerId, int turn) {
            this(ownerId, turn, null);
        }
    }

    public static GameStateSnapshot toSnapshot(GameState s) {
        s.validateIndustry();
        s.validateNavigation();
        s.validateDiplomacy();
        Map<Integer, Map<Integer, Intel>> intelCopy = new HashMap<>();
        s.intel.forEach((pid, inner) -> intelCopy.put(pid, new HashMap<>(inner)));
        Map<Integer, List<FleetOrder>> ordersCopy = new HashMap<>();
        s.pendingOrders.forEach((pid, orders) -> ordersCopy.put(pid, new ArrayList<>(orders)));
        Map<Integer, List<StandingOrder>> standingCopy = new HashMap<>();
        s.standingOrders.forEach((pid, orders) -> standingCopy.put(pid, new ArrayList<>(orders)));
        Map<Integer, List<SystemOwnership>> historyCopy = new HashMap<>();
        s.ownershipHistory.forEach((systemId, periods) -> historyCopy.put(systemId, new ArrayList<>(periods)));
        Map<Integer, ReplayFrame> replayCopy = new HashMap<>(s.replayFrames);
        return new GameStateSnapshot(
                s.turn, s.nextGlobalFleetId, new HashMap<>(s.nextLocalFleetNo),
                List.copyOf(s.players), List.copyOf(s.systems), List.copyOf(s.fleets),
                new HashMap<>(s.reports), intelCopy,
                new HashSet<>(s.waitThisTurn), new HashSet<>(s.submittedThisTurn),
                s.gameOver, s.winnerId, s.started,
                new HashSet<>(s.joinedHumanPlayerIds), new HashSet<>(s.originalHumanPlayerIds),
                new HashSet<>(s.observers), new HashMap<>(s.seatByUser), new HashMap<>(s.invitedSeats()),
                ordersCopy, standingCopy, new HashMap<>(s.nextStandingOrderId),
                s.observersAllowed, s.reentryAllowed, s.turnStartedAt, s.visibility, s.finishedAt, historyCopy, replayCopy,
                s.battlePresentationEnabled, s.combatRandomnessPercent, s.roundRules, s.stragglerSince,
                  new HashMap<>(s.missedRounds), s.victorySystemPercent, s.originalSetup, new HashMap<>(s.previousOrders), s.ruleset, new HashMap<>(s.industries), s.navigation, s.diplomacy, s.victoryRules(), s.outcome(),
                Set.copyOf(s.resignedSeats), s.cosmic, Map.copyOf(s.timedOutPlaces), s.extent);
    }

    public static GameState fromSnapshot(GameStateSnapshot s) {
        GameState c = new GameState();
        c.turn = s.turn();
        c.nextGlobalFleetId = s.nextGlobalFleetId();
        c.nextLocalFleetNo.putAll(s.nextLocalFleetNo());
        c.players.addAll(s.players());
        c.systems.addAll(s.systems());
        c.fleets.addAll(s.fleets());
        c.reports.putAll(s.reports());
        s.intel().forEach((pid, inner) -> c.intel.put(pid, new HashMap<>(inner)));
        restoreOwnership(c, s);
        restoreEconomy(c, s);
        var resigned = s.resignedSeats();
        if (resigned != null) { c.resignedSeats.addAll(resigned); }
        var timedOutPlaces = s.timedOutPlaces();
        if (timedOutPlaces != null) { c.timedOutPlaces.putAll(timedOutPlaces); }
        c.cosmic = CosmicState.orEmpty(s.cosmic());
        restoreTravel(c, s);
        restoreOrders(c, s);
        restoreSettings(c, s);
        validateOutcome(c, s);
        restoreTiming(c, s);
        return c;
    }

    private static void restoreOwnership(GameState c, GameStateSnapshot s) {
        Map<Integer, List<SystemOwnership>> history = s.ownershipHistory();
        if (history != null) {
            history.forEach((systemId, periods) -> c.ownershipHistory.put(systemId, new ArrayList<>(periods)));
        } else {
            c.initializeOwnershipHistoryFromSystems();
        }
        Map<Integer, ReplayFrame> frames = s.replayFrames();
        if (frames != null) {
            c.replayFrames.putAll(frames);
        }
        c.waitThisTurn.addAll(s.waitThisTurn());
        c.submittedThisTurn.addAll(s.submittedThisTurn());
        if (s.gameOver()) {
            c.endGame(s.winnerId());
        }
        c.finishedAt = s.finishedAt();
        c.visibility = s.visibility() == null ? GameVisibility.PUBLIC : s.visibility();
        c.started = s.started();
        c.joinedHumanPlayerIds.addAll(s.joinedHumanPlayerIds());
        c.originalHumanPlayerIds.addAll(s.originalHumanPlayerIds());
        c.observers.addAll(s.observers());
        c.seatByUser.putAll(s.seatByUser());
        Map<String, Integer> invited = s.invitedSeats();
        if (invited != null) {
            c.invitedSeats.putAll(invited);
        }
    }

    private static void restoreEconomy(GameState c, GameStateSnapshot s) {
        RulesetRef storedRuleset = s.ruleset();
        c.ruleset = storedRuleset == null ? RulesetRef.SECTOR_FORCES : storedRuleset;
        VictoryRules storedVictoryRules = s.victoryRules();
        c.victoryRules = storedVictoryRules == null ? null : storedVictoryRules.forRuleset(c.ruleset);
        var storedOutcome = s.outcome();
        if (storedOutcome != null && storedOutcome.allianceVictory()) {
            if (!s.gameOver()) { throw new IllegalArgumentException("Running game cannot have alliance winners"); }
            c.groupWinners = storedOutcome.winnerIds(); c.winningAlliance = storedOutcome.allianceId();
        }
        var industry = s.industries();
        if (industry != null) { c.industries.putAll(industry); }
        s.pendingOrders().forEach((pid, orders) -> c.pendingOrders.put(pid, new ArrayList<>(orders)));
        c.validateIndustry();
    }

    private static void restoreTravel(GameState c, GameStateSnapshot s) {
        var treaties = s.diplomacy();
        c.diplomacy = treaties == null ? DiplomacyState.EMPTY : treaties;
        c.validateDiplomacy();
        c.navigation = s.navigation();
        if (c.navigation == null && c.fleets.stream().anyMatch(fleet -> fleet.journey() != null)) {
            throw new IllegalArgumentException("Navigation settings missing for stored itineraries");
        }
        c.initializeNavigation();
        c.validateNavigation();
        c.originalSetup = s.originalSetup();
        c.extent = s.extent() == null ? GalaxyExtent.LEGACY : s.extent();
        if (c.originalSetup != null && !c.originalSetup.ruleset().equals(c.ruleset)) {
            throw new IllegalArgumentException("Snapshot and original setup disagree on game rules");
        }
    }

    private static void restoreOrders(GameState c, GameStateSnapshot s) {
        var previous = s.previousOrders();
        if (previous != null) { previous.forEach((pid, orders) -> c.previousOrders.put(pid, List.copyOf(orders))); }
        Map<Integer, List<StandingOrder>> standing = s.standingOrders();
        if (standing != null) {
            standing.forEach((pid, orders) -> c.standingOrders.put(pid, new ArrayList<>(orders)));
        }
        if (s.nextStandingOrderId() != null) {
            c.nextStandingOrderId.putAll(s.nextStandingOrderId());
        }
    }

    private static void restoreSettings(GameState c, GameStateSnapshot s) {
        c.observersAllowed = s.observersAllowed();
        c.reentryAllowed = s.reentryAllowed();
        Boolean presentationEnabled = s.battlePresentationEnabled();
        c.battlePresentationEnabled = presentationEnabled != null ? presentationEnabled
                : GameConfig.DEFAULT_BATTLE_PRESENTATION_ENABLED;
        Integer randomness = s.combatRandomnessPercent();
        c.combatRandomnessPercent = randomness != null
                ? Math.clamp(randomness, GameConfig.MIN_COMBAT_RANDOMNESS_PERCENT, GameConfig.MAX_COMBAT_RANDOMNESS_PERCENT)
                : GameConfig.DEFAULT_COMBAT_RANDOMNESS_PERCENT;
        Integer victoryPercent = s.victorySystemPercent();
        c.victorySystemPercent = victoryPercent != null
                ? Math.clamp(victoryPercent, GameConfig.MIN_VICTORY_SYSTEM_PERCENT, GameConfig.MAX_VICTORY_SYSTEM_PERCENT)
                : GameConfig.VICTORY_SYSTEM_PERCENT;
    }

    private static void validateOutcome(GameState c, GameStateSnapshot s) {
        var storedOutcome = s.outcome();
        if (c.victoryRules != null && c.victoryRules.individualSystemPercent() != c.victorySystemPercent) {
            throw new IllegalArgumentException("Stored victory thresholds disagree");
        }
        if (storedOutcome != null && (!Objects.equals(storedOutcome.winnerId(), c.winnerId)
                || (storedOutcome.allianceVictory() && (!c.victoryRules().allianceVictoryAllowed()
                    || !c.players.stream().map(Player::id).toList().containsAll(storedOutcome.winnerIds()))))) {
            throw new IllegalArgumentException("Stored winners disagree with game rules");
        }
    }

    private static void restoreTiming(GameState c, GameStateSnapshot s) {
        // Aeltere Snapshots kennen das Feld nicht; dann laeuft die Zug-Uhr ab Wiederherstellung.
        Instant startedAt = s.turnStartedAt();
        c.turnStartedAt = startedAt != null ? startedAt : Instant.now();
        RoundRules rules = s.roundRules();
        c.roundRules = rules != null ? rules : RoundRules.defaults();
        c.stragglerSince = s.stragglerSince();
        Map<Integer, Integer> missed = s.missedRounds();
        if (missed != null) {
            c.missedRounds.putAll(missed);
        }
    }

    /**
     * Creates a shallow copy of collections; elements are not deep-copied.
     */
    public static GameState copyOf(GameState s) {
        GameState c = new GameState();

        // flache, aber eigene Collections
        c.players().addAll(s.players());
        c.systems().addAll(s.systems());
        c.fleets().addAll(s.fleets());
        c.reports().putAll(s.reports());

        // INTEL tief kopieren (ownerId+turn)
        s.intel().forEach((pid, inner) -> {
            var copy = new HashMap<>(inner);
            c.intel().put(pid, copy);
        });
        s.ownershipHistory().forEach((systemId, periods) ->
                c.ownershipHistory.put(systemId, new ArrayList<>(periods)));
        c.replayFrames.putAll(s.replayFrames());

        // Wartemarkierungen übernehmen
        c.waitThisTurn().addAll(s.waitThisTurn());
        c.submittedThisTurn().addAll(s.submittedThisTurn());
        c.joinedHumanPlayerIds().addAll(s.joinedHumanPlayerIds());
        c.originalHumanPlayerIds().addAll(s.originalHumanPlayerIds());
        c.observers().addAll(s.observers());
        c.seatByUser().putAll(s.seatByUser());
        c.invitedSeats().putAll(s.invitedSeats());
        s.pendingOrders().forEach((pid, orders) -> c.pendingOrders().put(pid, new ArrayList<>(orders)));
        s.standingOrders().forEach((pid, orders) -> c.standingOrders().put(pid, new ArrayList<>(orders)));
        c.nextStandingOrderId.putAll(s.nextStandingOrderId);
        c.observersAllowed = s.observersAllowed();
        c.reentryAllowed = s.reentryAllowed();
        c.battlePresentationEnabled = s.battlePresentationEnabled();
        c.combatRandomnessPercent = s.combatRandomnessPercent;
        c.victorySystemPercent = s.victorySystemPercent;
        c.victoryRules = s.victoryRules;
        c.roundRules = s.roundRules();
        c.missedRounds.putAll(s.missedRounds());

        if (s.gameOver()) {
            c.endGame(s.winnerId());
        }

        c.finishedAt = s.finishedAt();
        c.groupWinners = s.groupWinners;
        c.winningAlliance = s.winningAlliance;
        c.visibility = s.visibility() == null ? GameVisibility.PUBLIC : s.visibility();
        c.started = s.started();

        // Rehydrate counters and clocks without executing a new round.
        c.turn = s.turn;
        c.nextGlobalFleetId = s.nextGlobalFleetId;
        c.nextLocalFleetNo.putAll(s.nextLocalFleetNo);
        c.turnStartedAt = s.turnStartedAt;
        c.stragglerSince = s.stragglerSince;

        c.ruleset = s.ruleset;
        c.industries.putAll(s.industries);
        c.resignedSeats.addAll(s.resignedSeats);
        c.timedOutPlaces.putAll(s.timedOutPlaces);
        c.cosmic = s.cosmic;
        c.navigation = s.navigation;
        c.diplomacy = s.diplomacy;
        c.originalSetup = s.originalSetup;
        c.extent = s.extent;
        c.previousOrders.putAll(s.previousOrders);
        return c;
    }

    private void initializeOwnershipHistoryFromSystems() {
        for (StarSystem system : systems) {
            if (system.ownerId() != null) {
                ownershipHistory.put(system.id(), new ArrayList<>(List.of(new SystemOwnership(system.ownerId(), turn))));
            }
        }
    }
}
