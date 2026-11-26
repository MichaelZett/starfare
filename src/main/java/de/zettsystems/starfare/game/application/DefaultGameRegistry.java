package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GalaxyGenesis;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.*;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Spring-injected GameSessionStore is kept by reference for the bean's lifetime by design.")
public class DefaultGameRegistry implements GameRegistry {
    private final GameSessionStore store;
    private final GameAccessPolicy access;
    private final RulesetCatalog catalog;
    private final RulesetAccess rulesetAccess;

    public DefaultGameRegistry(GameSessionStore store, GameAccessPolicy access) {
        this(store, access, RulesetCatalog.builtIn());
    }

    public DefaultGameRegistry(GameSessionStore store, GameAccessPolicy access, RulesetCatalog catalog) {
        this(store, access, catalog, RulesetAccess.unrestricted());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public DefaultGameRegistry(GameSessionStore store, GameAccessPolicy access, RulesetCatalog catalog,
                               RulesetAccess rulesetAccess) {
        this.store = store;
        this.access = access;
        this.catalog = catalog;
        this.rulesetAccess = rulesetAccess;
    }

    @Override
    public RulesetCatalog rulesets() { return catalog; }

    @Override
    public GameId createGame(GameSetup requestedSetup, @Nullable String hostPlayerId, String name) {
        GameSetup setup = requestedSetup.normalized();
        catalog.requireCreatable(setup.ruleset());
        if (!rulesetAccess.mayUse(setup.ruleset(), hostPlayerId)) {
            throw new IllegalArgumentException("Ruleset is reserved for testers: " + setup.ruleset().variant());
        }
        GameId id = GameId.newId();
        GameSession session = new GameSession(id, name, hostPlayerId, Instant.now());
        session.writeState(state -> {
            initializeState(state, setup);
            state.initializeIndustry();
            state.initializeNavigation();
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
    public long version() {
        return store.version();
    }

    @Override
    public List<GameId> loadedIds() {
        return store.loadedIds();
    }

    @Override
    public Optional<GameSession> find(GameId id) {
        return store.load(id);
    }

    @Override
    public Optional<de.zettsystems.starfare.game.values.GameSummary> parkedSummary(GameId id) {
        return store.parkedSummary(id);
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
        return session.writeStateAndThen(state -> {
            catalog.requireSupported(state.ruleset());
            return fn.apply(state);
        }, store::save);
    }

    @Override
    public void touch(GameId id) {
        store.touch(id);
    }

    @Override
    public Optional<Integer> claimSeat(GameId id, @Nullable String playerId) {
        return claimSeat(id, playerId, "", "");
    }

    @Override
    public Optional<Integer> claimSeat(GameId id, @Nullable String playerId, String playerName, String empireName) {
        if (playerId == null || playerId.isBlank()) {
            return Optional.empty();
        }
        if (find(id).isEmpty()) { return Optional.empty(); }
        // Die Variante ändert sich nach dem Anlegen nicht; die Kontoabfrage läuft deshalb vor dem Schreiblock.
        boolean newcomersAdmitted = rulesetAccess.mayUse(readState(id, GameState::ruleset), playerId);
        return Optional.ofNullable(writeState(id, state -> {
            if (!access.visible(state, require(id).hostPlayerId(), playerId)) { return null; }
            boolean knownAccount = state.seatByUser().containsKey(playerId);
            if (!knownAccount && !newcomersAdmitted) { return null; }
            Integer seat = tryClaimSeat(state, playerId);
            if (seat != null && !knownAccount) {
                state.updatePlayer(seat, player -> player.identifiedAs(playerName, empireName));
            }
            return seat;
        }));
    }

    private static @Nullable Integer tryClaimSeat(GameState state, String playerId) {
        if (state.gameOver()) {
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
            if (!state.reentryAllowed() || !state.originalHumanPlayerIds().contains(existing)
                    || state.closedSeats().contains(existing)) {
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
        Set<Integer> reserved = new HashSet<>(state.invitedSeats().values());
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
            if (state.gameOver()) {
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
            if (!state.originalHumanPlayerIds().contains(playerId) || state.closedSeats().contains(playerId)) {
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
            if (state.started() || state.gameOver()) {
                return false;
            }
            var humans = state.players().stream().filter(p -> !p.ai()).map(Player::id).toList();
            return !humans.isEmpty() && state.joinedHumanPlayerIds().containsAll(humans);
        });
    }

    @Override
    public boolean startGame(GameId id) {
        return writeState(id, state -> {
            if (state.started() || state.gameOver()) {
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

    @Override
    public int unloadInactiveSingleHumanGames(Duration inactivity) {
        return store.unloadInactiveSingleHumanGames(inactivity);
    }

    private static void initializeState(GameState state, GameSetup setup) {
        GalaxyGenesis.populate(state, setup, new Random(System.nanoTime()));
    }
}
