package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.*;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;

@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Spring-injected GameSessionStore is kept by reference for the bean's lifetime by design.")
public class DefaultGameRegistry implements GameRegistry {
    private static final List<String> AI_NAMES = List.of("Orion", "Lyra", "Vega", "Nova", "Atlas",
            "Mira", "Sagan", "Astra", "Kepler", "Selene");
    private final GameSessionStore store;
    private final GameAccessPolicy access;

    public DefaultGameRegistry(GameSessionStore store, GameAccessPolicy access) {
        this.store = store;
        this.access = access;
    }

    @Override
    public GameId createGame(GameSetup requestedSetup, @Nullable String hostPlayerId, String name) {
        GameId id = GameId.newId();
        GameSession session = new GameSession(id, name, hostPlayerId, Instant.now());
        session.writeState(state -> {
            initializeState(state, requestedSetup.normalized());
            return null;
        });
        store.save(session);
        return id;
    }

    @Override
    public GameId createGame(GameSetup setup) {
        return createGame(setup, null, GameConfig.DEFAULT_GAME_NAME);
    }

    @Override
    public List<GameId> listIds() {
        return store.listIds();
    }

    @Override
    public Optional<GameSession> find(GameId id) {
        return store.load(id);
    }

    @Override
    public GameSession require(GameId id) {
        return find(id).orElseThrow(() -> new IllegalArgumentException("Unknown game: " + id));
    }

    @Override
    public <T> T readState(GameId id, Function<GameState, T> fn) {
        return require(id).readState(fn);
    }

    @Override
    public <T> T writeState(GameId id, Function<GameState, T> fn) {
        GameSession session = require(id);
        return session.writeState(state -> {
            T result = fn.apply(state);
            store.save(session);
            return result;
        });
    }

    @Override
    public Optional<Integer> claimSeat(GameId id, @Nullable String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return Optional.empty();
        }
        if (find(id).isEmpty()) { return Optional.empty(); }
        return Optional.ofNullable(writeState(id, state -> {
            if (!access.visible(state, require(id).hostPlayerId(), playerId)) { return null; }
            return tryClaimSeat(state, playerId);
        }));
    }

    private static @Nullable Integer tryClaimSeat(GameState state, String playerId) {
        if (!state.active() || state.gameOver()) {
            return null;
        }
        Integer existing = state.seatByUser().get(playerId);
        if (existing != null) {
            return tryReclaimExistingSeat(state, existing);
        }
        if (state.started()) {
            return null;
        }
        Integer invitedSeat = state.invitedSeats().get(playerId);
        if (invitedSeat != null) {
            return claimInvitedSeat(state, playerId, invitedSeat);
        }
        return tryClaimFreeSeat(state, playerId);
    }

    private static @Nullable Integer tryReclaimExistingSeat(GameState state, int existing) {
        if (state.joinedHumanPlayerIds().contains(existing)) {
            return existing;
        }
        if (state.started()) {
            if (!state.reentryAllowed() || !state.originalHumanPlayerIds().contains(existing)) {
                return null;
            }
            state.updatePlayer(existing, Player::asHuman);
            state.joinedHumanPlayerIds().add(existing);
            return existing;
        }
        state.joinedHumanPlayerIds().add(existing);
        return existing;
    }

    private static Integer claimInvitedSeat(GameState state, String playerId, int invitedSeat) {
        state.invitedSeats().remove(playerId);
        state.joinedHumanPlayerIds().add(invitedSeat);
        state.seatByUser().put(playerId, invitedSeat);
        return invitedSeat;
    }

    private static @Nullable Integer tryClaimFreeSeat(GameState state, String playerId) {
        java.util.Set<Integer> reserved = new java.util.HashSet<>(state.invitedSeats().values());
        Integer seat = state.players().stream()
                .filter(p -> !p.ai())
                .map(Player::id)
                .filter(pid -> !state.joinedHumanPlayerIds().contains(pid))
                .filter(pid -> !reserved.contains(pid))
                .findFirst().orElse(null);
        if (seat == null) {
            return null;
        }
        state.joinedHumanPlayerIds().add(seat);
        state.seatByUser().put(playerId, seat);
        return seat;
    }

    @Override
    public Optional<Integer> seatOf(GameId id, @Nullable String playerId) {
        if (playerId == null) {
            return Optional.empty();
        }
        return find(id).flatMap(session -> session.readState(state ->
                Optional.ofNullable(state.seatByUser().get(playerId))));
    }

    @Override
    public boolean joinHumanPlayer(GameId id, int playerId) {
        return writeState(id, state -> {
            if (!state.active() || state.gameOver()) {
                return false;
            }
            if (state.joinedHumanPlayerIds().contains(playerId)) {
                return false;
            }
            if (!state.started()) {
                boolean exists = state.players().stream().anyMatch(p -> p.id() == playerId && !p.ai());
                if (!exists) {
                    return false;
                }
                state.joinedHumanPlayerIds().add(playerId);
                return true;
            }
            if (!state.reentryAllowed()) {
                return false;
            }
            if (!state.originalHumanPlayerIds().contains(playerId)) {
                return false;
            }
            state.updatePlayer(playerId, Player::asHuman);
            state.joinedHumanPlayerIds().add(playerId);
            return true;
        });
    }

    @Override
    public boolean canStartGame(GameId id) {
        return readState(id, state -> {
            if (!state.active() || state.started() || state.gameOver()) {
                return false;
            }
            var humans = state.players().stream().filter(p -> !p.ai()).map(Player::id).toList();
            return !humans.isEmpty() && state.joinedHumanPlayerIds().containsAll(humans);
        });
    }

    @Override
    public boolean startGame(GameId id) {
        return writeState(id, state -> {
            if (!state.active() || state.started() || state.gameOver()) {
                return false;
            }
            var humans = state.players().stream().filter(p -> !p.ai()).map(Player::id).toList();
            if (humans.isEmpty() || !state.joinedHumanPlayerIds().containsAll(humans)) {
                return false;
            }
            state.start();
            return true;
        });
    }

    @Override
    public void abortGame(GameId id) {
        store.delete(id);
    }

    private void initializeState(GameState state, GameSetup setup) {
        state.resetForNewGame();
        state.configureLobby(setup.observersAllowed(), setup.reentryAllowed());
        state.players().clear();
        state.systems().clear();
        state.fleets().clear();
        state.reports().clear();
        state.intel().clear();

        int pid = 1;
        for (int i = 1; i <= setup.humanPlayers(); i++) {
            int seatId = pid++;
            state.players().add(new Player(seatId, "P" + i, false, setup.colorForSeat(i - 1)));
            state.originalHumanPlayerIds().add(seatId);
        }
        for (int i = 1; i <= setup.aiPlayers(); i++) {
            state.players().add(new Player(pid++, aiName(i), true, setup.colorForSeat(setup.humanPlayers() + i - 1)));
        }
        for (Player p : state.players()) {
            state.intel().put(p.id(), new HashMap<>());
        }

        var r = new Random(System.nanoTime());
        List<String> names = SystemNameGenerator.sample(setup.systemCount(), r);
        List<double[]> positions = positionsFor(setup, r);
        for (int i = 1; i <= setup.systemCount(); i++) {
            double[] pos = positions.get(i - 1);
            int prod = neutralProduction(setup, r);
            int garrison = Math.max(1, prod - 1);
            state.systems().add(new StarSystem(i, names.get(i - 1), pos[0], pos[1], null, garrison, prod, true));
        }

        List<StarSystem> homes = homeSystems(state, r);
        for (int playerSeat = 0; playerSeat < state.players().size(); playerSeat++) {
            Player p = state.players().get(playerSeat);
            StarSystem s = homes.get(playerSeat);
            int startProduction = setup.startProductionForSeat(playerSeat);
            int startGarrison = Math.max(setup.startGarrison(), startProduction);
            state.updateSystem(s.id(), current -> current.colonize(p.id(), startGarrison, startProduction));
        }
        spaceOut(state, GameConfig.SPACEOUT_ITERATIONS, GameConfig.SPACEOUT_MIN_DIST);
    }

    private static String aiName(int index) {
        String name = AI_NAMES.get((index - 1) % AI_NAMES.size());
        int series = (index - 1) / AI_NAMES.size();
        return series == 0 ? name : name + " " + (series + 1);
    }

    private static int neutralProduction(GameSetup setup, Random r) {
        int min = setup.neutralMinProduction();
        int max = setup.neutralMaxProduction();
        if (max <= min) {
            return min;
        }
        if (setup.productionDistribution() == ProductionDistribution.UNIFORM) {
            return min + r.nextInt(max - min + 1);
        }
        // Mitte der Spanne als Erwartungswert, 2 Sigma bis zum Rand. Ausreisser werden
        // neu gezogen statt geklemmt, sonst haeufen sie sich genau auf min und max.
        double mean = (min + max) / 2.0;
        double sigma = (max - min) / 4.0;
        for (int attempt = 0; attempt < 10; attempt++) {
            long value = Math.round(mean + r.nextGaussian() * sigma);
            if (value >= min && value <= max) {
                return (int) value;
            }
        }
        return (int) Math.round(mean);
    }

    private static List<double[]> positionsFor(GameSetup setup, Random r) {
        int count = setup.systemCount();
        var out = new ArrayList<double[]>(count);
        double marginX = Math.min(GameConfig.SYSTEM_MARGIN, GameConfig.MAX_X / 4);
        double marginY = Math.min(GameConfig.SYSTEM_MARGIN, GameConfig.MAX_Y / 4);
        if (setup.galaxyLayout() == GalaxyLayout.RANDOM) {
            for (int i = 0; i < count; i++) {
                out.add(new double[]{
                        marginX + r.nextDouble(GameConfig.MAX_X - 2 * marginX),
                        marginY + r.nextDouble(GameConfig.MAX_Y - 2 * marginY)});
            }
            return out;
        }
        // Ein System je Rasterzelle, innerhalb der Zelle versetzt: verhindert die
        // Ballungen und Leerraeume, die rein zufaellige Punkte zwangslaeufig bilden.
        int cols = (int) Math.ceil(Math.sqrt(count * (double) GameConfig.MAX_X / GameConfig.MAX_Y));
        int rows = (int) Math.ceil((double) count / cols);
        double cellWidth = (GameConfig.MAX_X - 2 * marginX) / cols;
        double cellHeight = (GameConfig.MAX_Y - 2 * marginY) / rows;
        var cells = new ArrayList<int[]>(cols * rows);
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                cells.add(new int[]{col, row});
            }
        }
        Collections.shuffle(cells, r);
        for (int i = 0; i < count; i++) {
            int[] cell = cells.get(i);
            out.add(new double[]{
                    marginX + cell[0] * cellWidth + (0.25 + r.nextDouble() * 0.5) * cellWidth,
                    marginY + cell[1] * cellHeight + (0.25 + r.nextDouble() * 0.5) * cellHeight});
        }
        return out;
    }

    private static List<StarSystem> homeSystems(GameState state, Random r) {
        var candidates = new ArrayList<>(state.systems());
        Collections.shuffle(candidates, r);
        int needed = state.players().size();
        if (needed >= candidates.size()) {
            return candidates.subList(0, Math.min(needed, candidates.size()));
        }
        // Greedy moeglichst weit auseinander: sonst entscheidet der Zufall der
        // Startnachbarschaft die Partie, bevor der erste Zug laeuft.
        var chosen = new ArrayList<StarSystem>(needed);
        chosen.add(candidates.removeFirst());
        while (chosen.size() < needed) {
            StarSystem best = candidates.getFirst();
            double bestDist = -1;
            for (StarSystem candidate : candidates) {
                double nearest = chosen.stream()
                        .mapToDouble(c -> Math.hypot(c.x() - candidate.x(), c.y() - candidate.y()))
                        .min().orElse(0);
                if (nearest > bestDist) {
                    bestDist = nearest;
                    best = candidate;
                }
            }
            candidates.remove(best);
            chosen.add(best);
        }
        return chosen;
    }

    private static void spaceOut(GameState state, int iterations, double minDist) {
        double minX = Math.min(GameConfig.SYSTEM_MARGIN, GameConfig.MAX_X / 4);
        double maxX = GameConfig.MAX_X - minX;
        double minY = Math.min(GameConfig.SYSTEM_MARGIN, GameConfig.MAX_Y / 4);
        double maxY = GameConfig.MAX_Y - minY;
        for (int it = 0; it < iterations; it++) {
            for (int i = 0; i < state.systems().size(); i++) {
                for (int j = i + 1; j < state.systems().size(); j++) {
                    StarSystem a = state.systems().get(i);
                    StarSystem b = state.systems().get(j);
                    double dx = b.x() - a.x();
                    double dy = b.y() - a.y();
                    double dist = Math.hypot(dx, dy);
                    if (dist < 1e-6) {
                        dx = 1;
                        dy = 0;
                        dist = 1;
                    }
                    if (dist < minDist) {
                        double push = (minDist - dist) / 2.0;
                        double ux = dx / dist;
                        double uy = dy / dist;
                        double ax = clamp(a.x() - ux * push, minX, maxX);
                        double ay = clamp(a.y() - uy * push, minY, maxY);
                        double bx = clamp(b.x() + ux * push, minX, maxX);
                        double by = clamp(b.y() + uy * push, minY, maxY);
                        state.systems().set(i, a.relocateTo(ax, ay));
                        state.systems().set(j, b.relocateTo(bx, by));
                    }
                }
            }
        }
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.clamp(v, lo, hi);
    }
}
