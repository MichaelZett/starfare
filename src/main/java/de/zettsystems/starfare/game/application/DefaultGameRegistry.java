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
    private final GameSessionStore store;

    public DefaultGameRegistry(GameSessionStore store) {
        this.store = store;
    }

    @Override
    public GameId createGame(GameSetup requestedSetup, @Nullable String hostUsername, String name) {
        GameId id = GameId.newId();
        GameSession session = new GameSession(id, name, hostUsername, Instant.now());
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
        T result = session.writeState(fn);
        store.save(session);
        return result;
    }

    @Override
    public Optional<Integer> claimSeat(GameId id, @Nullable String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(writeState(id, state -> tryClaimSeat(state, username)));
    }

    private static @Nullable Integer tryClaimSeat(GameState state, String username) {
        if (!state.active() || state.gameOver()) {
            return null;
        }
        Integer existing = state.seatByUser().get(username);
        if (existing != null) {
            return tryReclaimExistingSeat(state, existing);
        }
        if (state.started()) {
            return null;
        }
        Integer invitedSeat = state.invitedSeats().get(username);
        if (invitedSeat != null) {
            return claimInvitedSeat(state, username, invitedSeat);
        }
        return tryClaimFreeSeat(state, username);
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

    private static Integer claimInvitedSeat(GameState state, String username, int invitedSeat) {
        state.invitedSeats().remove(username);
        state.joinedHumanPlayerIds().add(invitedSeat);
        state.seatByUser().put(username, invitedSeat);
        return invitedSeat;
    }

    private static @Nullable Integer tryClaimFreeSeat(GameState state, String username) {
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
        state.seatByUser().put(username, seat);
        return seat;
    }

    @Override
    public Optional<Integer> seatOf(GameId id, @Nullable String username) {
        if (username == null) {
            return Optional.empty();
        }
        return find(id).flatMap(session -> session.readState(state ->
                Optional.ofNullable(state.seatByUser().get(username))));
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
            if (!state.active() || state.started()) {
                return false;
            }
            var humans = state.players().stream().filter(p -> !p.ai()).map(Player::id).toList();
            return !humans.isEmpty() && state.joinedHumanPlayerIds().containsAll(humans);
        });
    }

    @Override
    public boolean startGame(GameId id) {
        return writeState(id, state -> {
            if (!state.active() || state.started()) {
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

        var colorPool = new ArrayList<>(GameConfig.PLAYER_PALETTE);
        colorPool.remove(setup.humanColorHex());
        Collections.shuffle(colorPool, new Random(System.nanoTime()));

        int pid = 1;
        int ci = 0;
        for (int i = 1; i <= setup.humanPlayers(); i++) {
            String color = i == 1 ? setup.humanColorHex() : colorPool.get(ci++);
            int seatId = pid++;
            state.players().add(new Player(seatId, "P" + i, false, color));
            state.originalHumanPlayerIds().add(seatId);
        }
        for (int i = 1; i <= setup.aiPlayers(); i++) {
            state.players().add(new Player(pid++, "AI" + i, true, colorPool.get(ci++)));
        }
        for (Player p : state.players()) {
            state.intel().put(p.id(), new HashMap<>());
        }

        var r = new Random(System.nanoTime());
        List<String> names = SystemNameGenerator.sample(setup.systemCount(), r);
        for (int i = 1; i <= setup.systemCount(); i++) {
            double x = r.nextDouble(GameState.MAX_X);
            double y = r.nextDouble(GameState.MAX_Y);
            int prodRange = setup.neutralMaxProduction() - setup.neutralMinProduction() + 1;
            int prod = setup.neutralMinProduction() + r.nextInt(Math.max(1, prodRange));
            int garrison = Math.max(1, prod - 1);
            state.systems().add(new StarSystem(i, names.get(i - 1), x, y, null, garrison, prod, true));
        }

        var shuffled = new ArrayList<>(state.systems());
        Collections.shuffle(shuffled, r);
        int idx = 0;
        for (int playerSeat = 0; playerSeat < state.players().size(); playerSeat++) {
            Player p = state.players().get(playerSeat);
            StarSystem s = shuffled.get(idx++);
            int startProduction = setup.startProductionForSeat(playerSeat);
            int startGarrison = Math.max(setup.startGarrison(), startProduction);
            state.updateSystem(s.id(), current -> current.colonize(p.id(), startGarrison, startProduction));
        }
        spaceOut(state, GameConfig.SPACEOUT_ITERATIONS, GameConfig.SPACEOUT_MIN_DIST);
    }

    private static void spaceOut(GameState state, int iterations, double minDist) {
        double minX = 0;
        double maxX = GameState.MAX_X;
        double minY = 0;
        double maxY = GameState.MAX_Y;
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
