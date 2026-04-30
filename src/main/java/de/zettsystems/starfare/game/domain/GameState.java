package de.zettsystems.starfare.game.domain;

import de.zettsystems.starfare.fleet.values.FleetOrder;
import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StandingOrder;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.report.values.TurnReport;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Mutable in-memory game state for a single running match.
 */
@SuppressFBWarnings(value = "EI_EXPOSE_REP",
        justification = "GameState is the mutable aggregate root of a running match. Live collections "
                + "are exposed on purpose so application services can mutate them; concurrency is "
                + "serialized via GameSession's read/write lock (see CLAUDE.md \"State/repository contract\").")
public class GameState {
    public static final float MAX_X = 3200.0F;
    public static final float MAX_Y = 2000.0F;
    private int turn = 1;
    private final List<Player> players = new ArrayList<>();
    private final List<StarSystem> systems = new ArrayList<>();
    private final List<Fleet> fleets = new ArrayList<>();
    private final Map<Integer, TurnReport> reports = new HashMap<>();
    private final Map<Integer, Map<Integer, Intel>> intel = new HashMap<>();
    private int nextGlobalFleetId = 1;
    private final java.util.Map<Integer, Integer> nextLocalFleetNo = new java.util.HashMap<>();
    // neu
    private final java.util.Set<Integer> waitThisTurn = new java.util.HashSet<>();
    private final java.util.Set<Integer> submittedThisTurn = new java.util.HashSet<>();
    private boolean gameOver;
    private @Nullable Integer winnerId;
    private boolean active = true;
    private boolean started;
    private final java.util.Set<Integer> joinedHumanPlayerIds = new java.util.HashSet<>();
    private final java.util.Set<Integer> originalHumanPlayerIds = new java.util.HashSet<>();
    private final java.util.Set<String> observers = new java.util.HashSet<>();
    private final Map<String, Integer> seatByUser = new HashMap<>();
    /**
     * Username → reserved seat id. Ephemeral (not persisted via {@link #toSnapshot(GameState)});
     * server restart clears pending invites.
     */
    private final Map<String, Integer> invitedSeats = new HashMap<>();
    private final Map<Integer, List<FleetOrder>> pendingOrders = new HashMap<>();
    private final Map<Integer, List<StandingOrder>> standingOrders = new HashMap<>();
    private final Map<Integer, Integer> nextStandingOrderId = new HashMap<>();
    private boolean observersAllowed;
    private boolean reentryAllowed;

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

    public java.util.Set<Integer> waitThisTurn() {
        return waitThisTurn;
    }

    public java.util.Set<Integer> submittedThisTurn() {
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
        this.started = true;
    }

    public java.util.Set<Integer> joinedHumanPlayerIds() {
        return joinedHumanPlayerIds;
    }

    public java.util.Set<Integer> originalHumanPlayerIds() {
        return originalHumanPlayerIds;
    }

    public java.util.Set<String> observers() {
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

    /**
     * Applies the lobby-level policies (observer access, re-entry of dropped humans).
     */
    public void configureLobby(boolean observersAllowed, boolean reentryAllowed) {
        this.observersAllowed = observersAllowed;
        this.reentryAllowed = reentryAllowed;
    }

    /**
     * Ends the game, optionally recording a winner (null = abort/draw).
     */
    public void endGame(@Nullable Integer winnerId) {
        this.gameOver = true;
        this.winnerId = winnerId;
    }

    public void clearGameOver() {
        this.gameOver = false;
        this.winnerId = null;
    }

    public void resetForNewGame() {
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
        this.standingOrders.clear();
        this.nextStandingOrderId.clear();
        this.observersAllowed = false;
        this.reentryAllowed = false;
        this.players.clear();
        this.systems.clear();
        this.fleets.clear();
        this.reports.clear();
        this.intel.clear();
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
        this.standingOrders.clear();
        this.nextStandingOrderId.clear();
        this.observersAllowed = false;
        this.reentryAllowed = false;
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
        int localNo = nextLocalNoFor(ownerId);
        Fleet f = new Fleet(nextGlobalFleetId++, ownerId, localNo, from, to, ships, turn, turn + travelRounds);
        fleets.add(f);
        return f.globalId();
    }

    public double distance(int fromId, int toId) {
        StarSystem a = byId(fromId);
        StarSystem b = byId(toId);
        return Math.hypot(a.x() - b.x(), a.y() - b.y());
    }

    public int travelRounds(int fromId, int toId) {
        double d = distance(fromId, toId);
        var bbox = bounds();
        double diag = Math.hypot(bbox.width, bbox.height);
        double baseSpeed = Math.max(1.0, diag / 20.0);
        int r = (int) Math.ceil(d / baseSpeed);
        return Math.clamp(r, 2, 20);
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
                systems.set(i, updater.apply(current));
                return;
            }
        }
        throw new IllegalArgumentException("System not found: " + id);
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
        this.turn++;
    }

    private record Bounds(double x, double y, double width, double height) {
    }

    public record Intel(@Nullable Integer ownerId, int turn) {
    }

    public static GameStateSnapshot toSnapshot(GameState s) {
        Map<Integer, Map<Integer, Intel>> intelCopy = new HashMap<>();
        s.intel.forEach((pid, inner) -> intelCopy.put(pid, new HashMap<>(inner)));
        Map<Integer, List<FleetOrder>> ordersCopy = new HashMap<>();
        s.pendingOrders.forEach((pid, orders) -> ordersCopy.put(pid, new ArrayList<>(orders)));
        Map<Integer, List<StandingOrder>> standingCopy = new HashMap<>();
        s.standingOrders.forEach((pid, orders) -> standingCopy.put(pid, new ArrayList<>(orders)));
        return new GameStateSnapshot(
                s.turn, s.nextGlobalFleetId, new HashMap<>(s.nextLocalFleetNo),
                List.copyOf(s.players), List.copyOf(s.systems), List.copyOf(s.fleets),
                new HashMap<>(s.reports), intelCopy,
                new java.util.HashSet<>(s.waitThisTurn), new java.util.HashSet<>(s.submittedThisTurn),
                s.gameOver, s.winnerId, s.active, s.started,
                new java.util.HashSet<>(s.joinedHumanPlayerIds), new java.util.HashSet<>(s.originalHumanPlayerIds),
                new java.util.HashSet<>(s.observers), new HashMap<>(s.seatByUser),
                ordersCopy, standingCopy, new HashMap<>(s.nextStandingOrderId),
                s.observersAllowed, s.reentryAllowed);
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
        c.waitThisTurn.addAll(s.waitThisTurn());
        c.submittedThisTurn.addAll(s.submittedThisTurn());
        if (s.gameOver()) {
            c.endGame(s.winnerId());
        }
        c.active = s.active();
        c.started = s.started();
        c.joinedHumanPlayerIds.addAll(s.joinedHumanPlayerIds());
        c.originalHumanPlayerIds.addAll(s.originalHumanPlayerIds());
        c.observers.addAll(s.observers());
        c.seatByUser.putAll(s.seatByUser());
        s.pendingOrders().forEach((pid, orders) -> c.pendingOrders.put(pid, new ArrayList<>(orders)));
        if (s.standingOrders() != null) {
            s.standingOrders().forEach((pid, orders) -> c.standingOrders.put(pid, new ArrayList<>(orders)));
        }
        if (s.nextStandingOrderId() != null) {
            c.nextStandingOrderId.putAll(s.nextStandingOrderId());
        }
        c.observersAllowed = s.observersAllowed();
        c.reentryAllowed = s.reentryAllowed();
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

        if (s.gameOver()) {
            c.endGame(s.winnerId());
        }

        c.active = s.active();
        c.started = s.started();

        // Turn angleichen (ohne direkten Setter)
        while (c.turn() < s.turn()) {
            c.nextTurn();
        }

        return c;
    }
}
