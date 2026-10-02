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
import org.jspecify.annotations.Nullable;

/**
 * Mutable in-memory game state for a single running match.
 */
@SuppressFBWarnings(value = "EI_EXPOSE_REP",
        justification = "GameState is the mutable aggregate root of a running match. Live collections "
                + "are exposed on purpose so application services can mutate them; concurrency is "
                + "serialized via GameSession's read/write lock (see CLAUDE.md \"State/repository contract\").")
public class GameState {
    private DiplomacyState diplomacy = DiplomacyState.EMPTY;
    public DiplomacyState diplomacy() { return diplomacy; }
    public boolean allied(int left, @Nullable Integer right) { return Routes.limited(this) && right != null && diplomacy.allied(left, right); }
    public void agreeTreaties(DiplomacyState agreed) {
        if (!Routes.limited(this)) { throw new IllegalStateException("Diplomacy belongs only to Spaceward"); }
        diplomacy = agreed;
    }
    private void validateDiplomacy() {
        if (!Routes.limited(this) && !diplomacy.equals(DiplomacyState.EMPTY)) {
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
        if (started || navigation != null || !RulesetRef.SPACEWARD.equals(ruleset)) { throw new IllegalStateException("Navigation is fixed at creation"); }
        navigation = settings;
    }
    public void initializeNavigation() {
        if (RulesetRef.SPACEWARD.equals(ruleset) && navigation == null) {
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

    public RulesetRef ruleset() { return ruleset; }
    public Map<Integer, Industry> industries() { return Map.copyOf(industries); }

    public void initializeIndustry() {
        if (!RulesetRef.SPACEWARD.equals(ruleset) || !industries.isEmpty()) { return; }
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

    public void completeIndustrialProduction(int systemId, Industry expanded, int routed) {
        Industry current = industries.get(systemId);
        if (current == null || !expanded.equals(current.expandForOneTurn())) {
            throw new IllegalArgumentException("Industrial growth must follow the current allocation");
        }
        updateSystem(systemId, system -> system.completeShipbuilding(current.shipbuilding(), routed, expanded.shipbuilding()));
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
        if (!RulesetRef.SPACEWARD.equals(ruleset)) {
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
    private boolean active = true;
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

    public Optional<GameSetup> originalSetup() { return Optional.ofNullable(originalSetup); }

    public void rememberSetup(GameSetup setup) {
        if (started || originalSetup != null) {
            throw new IllegalStateException("Game rules are fixed at creation");
        }
        originalSetup = setup;
        ruleset = setup.ruleset();
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

    public boolean active() {
        return active;
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
        return active && started && !gameOver && joinedHumanPlayerIds.size() >= 2;
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
        if (!this.gameOver) { this.finishedAt = finishedAt; }
        this.gameOver = true;
        this.winnerId = winnerId;
    }

    public void clearGameOver() {
        this.gameOver = false;
        this.finishedAt = null;
        this.winnerId = null;
    }

    /** Starts a fresh round deadline when the winner resumes play towards full conquest. */
    public void resumeForFullConquest(Instant now) {
        configureVictorySystemPercent(GameConfig.MAX_VICTORY_SYSTEM_PERCENT);
        clearGameOver();
        turnStartedAt = now;
        stragglerSince = null;
        submittedThisTurn.clear();
        updateStragglerClock(now);
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
        navigation = null;
        diplomacy = DiplomacyState.EMPTY;
        this.fleets.clear();
        this.reports.clear();
        this.intel.clear();
        this.ownershipHistory.clear();
        this.replayFrames.clear();
        clearGameOver();
        this.active = true;
        this.started = false;
    }

    public void resetForAbort() {
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
        this.standingOrders.clear();
        this.nextStandingOrderId.clear();
        this.replayFrames.clear();
        this.observersAllowed = false;
        this.reentryAllowed = false;
        this.battlePresentationEnabled = GameConfig.DEFAULT_BATTLE_PRESENTATION_ENABLED;
        this.combatRandomnessPercent = GameConfig.DEFAULT_COMBAT_RANDOMNESS_PERCENT;
        this.victorySystemPercent = GameConfig.VICTORY_SYSTEM_PERCENT;
        this.roundRules = RoundRules.defaults();
        this.stragglerSince = null;
        this.missedRounds.clear();
        clearGameOver();
        this.active = false;
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
        return roundsFor(distance(fromId, toId), baseSpeed());
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
        double baseSpeed = baseSpeed();
        Map<Integer, StarSystem> index = new HashMap<>();
        for (StarSystem s : systems) {
            index.put(s.id(), s);
        }
        List<StarSystem> sources = sourceSystemIds.stream().map(index::get).filter(Objects::nonNull).toList();
        Set<Integer> reachable = new HashSet<>();
        for (StarSystem target : systems) {
            for (StarSystem source : sources) {
                double d = Math.hypot(source.x() - target.x(), source.y() - target.y());
                if (roundsFor(d, baseSpeed) <= maxRounds) {
                    reachable.add(target.id());
                    break;
                }
            }
        }
        return reachable;
    }

    private double baseSpeed() {
        var bbox = bounds();
        double diag = Math.hypot(bbox.width, bbox.height);
        return Math.max(1.0, diag / 20.0);
    }

    private static int roundsFor(double distance, double baseSpeed) {
        return Math.clamp((int) Math.ceil(distance / baseSpeed), 2, 20);
    }

    private Bounds bounds() {
        double minX = systems.stream().mapToDouble(StarSystem::x).min().orElse(0);
        double maxX = systems.stream().mapToDouble(StarSystem::x).max().orElse(0);
        double minY = systems.stream().mapToDouble(StarSystem::y).min().orElse(0);
        double maxY = systems.stream().mapToDouble(StarSystem::y).max().orElse(0);
        return new Bounds(minX, minY, maxX - minX, maxY - minY);
    }

    public StarSystem byId(int id) {
        return systems.stream().filter(s -> s.id() == id).findFirst().orElseThrow();
    }

    public StarSystem getSystem(int id) {
        return byId(id);
    }

    public void updateSystem(int id, UnaryOperator<StarSystem> updater) {
        for (int i = 0; i < systems.size(); i++) {
            StarSystem current = systems.get(i);
            if (current.id() == id) {
                StarSystem updated = updater.apply(current);
                if (!Objects.equals(current.ownerId(), updated.ownerId())) {
                    Industry industry = industries.get(id);
                    if (industry != null) {
                        industries.put(id, industry.captured());
                        updated = updated.planShipbuilding(industry.capacity());
                    }
                }
                systems.set(i, updated);
                recordOwnershipChange(current, updated);
                return;
            }
        }
        throw new IllegalArgumentException("System not found: " + id);
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
        replayFrames.put(turn, new ReplayFrame(turn, systems, fleets, reports, industries));
    }

    private record Bounds(double x, double y, double width, double height) {
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
                s.gameOver, s.winnerId, s.active, s.started,
                new HashSet<>(s.joinedHumanPlayerIds), new HashSet<>(s.originalHumanPlayerIds),
                new HashSet<>(s.observers), new HashMap<>(s.seatByUser), new HashMap<>(s.invitedSeats()),
                ordersCopy, standingCopy, new HashMap<>(s.nextStandingOrderId),
                s.observersAllowed, s.reentryAllowed, s.turnStartedAt, s.visibility, s.finishedAt, historyCopy, replayCopy,
                s.battlePresentationEnabled, s.combatRandomnessPercent, s.roundRules, s.stragglerSince,
                  new HashMap<>(s.missedRounds), s.victorySystemPercent, s.originalSetup, new HashMap<>(s.previousOrders), s.ruleset, new HashMap<>(s.industries), s.navigation, s.diplomacy);
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
        c.active = s.active();
        c.started = s.started();
        c.joinedHumanPlayerIds.addAll(s.joinedHumanPlayerIds());
        c.originalHumanPlayerIds.addAll(s.originalHumanPlayerIds());
        c.observers.addAll(s.observers());
        c.seatByUser.putAll(s.seatByUser());
        Map<String, Integer> invited = s.invitedSeats();
        if (invited != null) {
            c.invitedSeats.putAll(invited);
        }
        RulesetRef storedRuleset = s.ruleset();
        c.ruleset = storedRuleset == null ? RulesetRef.SECTOR_FORCES : storedRuleset;
        var industry = s.industries();
        if (industry != null) { c.industries.putAll(industry); }
        c.validateIndustry();
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
        if (c.originalSetup != null && !c.originalSetup.ruleset().equals(c.ruleset)) {
            throw new IllegalArgumentException("Snapshot and original setup disagree on game rules");
        }
        var previous = s.previousOrders();
        if (previous != null) { previous.forEach((pid, orders) -> c.previousOrders.put(pid, List.copyOf(orders))); }
        s.pendingOrders().forEach((pid, orders) -> c.pendingOrders.put(pid, new ArrayList<>(orders)));
        Map<Integer, List<StandingOrder>> standing = s.standingOrders();
        if (standing != null) {
            standing.forEach((pid, orders) -> c.standingOrders.put(pid, new ArrayList<>(orders)));
        }
        if (s.nextStandingOrderId() != null) {
            c.nextStandingOrderId.putAll(s.nextStandingOrderId());
        }
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
        return c;
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
        c.roundRules = s.roundRules();
        c.missedRounds.putAll(s.missedRounds());

        if (s.gameOver()) {
            c.endGame(s.winnerId());
        }

        c.finishedAt = s.finishedAt();
        c.visibility = s.visibility() == null ? GameVisibility.PUBLIC : s.visibility();
        c.active = s.active();
        c.started = s.started();

        // Turn angleichen (ohne direkten Setter)
        while (c.turn() < s.turn()) {
            c.nextTurn();
        }

        c.ruleset = s.ruleset;
        c.industries.putAll(s.industries);
        c.navigation = s.navigation;
        c.diplomacy = s.diplomacy;
        c.originalSetup = s.originalSetup;
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
