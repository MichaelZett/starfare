package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.fleet.application.FleetService;
import de.zettsystems.starfare.fleet.values.FleetOrder;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.PlayerViewState;
import de.zettsystems.starfare.report.application.ReportService;
import de.zettsystems.starfare.turn.application.TurnEngine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Spring-injected collaborators are kept by reference for the bean's lifetime by design.")
public class DefaultGameService implements GameService {
    private final GameRegistry registry;
    private final TurnEngine turnEngine;
    private final FleetService fleetService;
    private final ReportService reportService;
    private final AutoplayRunner autoplayRunner;
    private final Broadcaster broadcaster;
    private final PlayerViewBuilder playerViewBuilder;

    public DefaultGameService(GameRegistry registry, TurnEngine turnEngine, FleetService fleetService,
                              ReportService reportService, AutoplayRunner autoplayRunner, Broadcaster broadcaster,
                              PlayerViewBuilder playerViewBuilder) {
        this.registry = registry;
        this.turnEngine = turnEngine;
        this.fleetService = fleetService;
        this.reportService = reportService;
        this.autoplayRunner = autoplayRunner;
        this.broadcaster = broadcaster;
        this.playerViewBuilder = playerViewBuilder;
    }

    @Override
    public GameId newGame(GameSetup setup, @Nullable String hostUsername, String name) {
        GameId id = registry.createGame(setup, hostUsername, name);
        broadcaster.publish(new GameEvent.GameCreated(id));
        return id;
    }

    @Override
    public GameId newGame(GameSetup setup) {
        GameId id = registry.createGame(setup);
        broadcaster.publish(new GameEvent.GameCreated(id));
        return id;
    }

    @Override
    public List<GameId> listGames() {
        return registry.listIds();
    }

    @Override
    public String gameNameOf(GameId gameId) {
        return registry.find(gameId).map(GameSession::name).orElse("");
    }

    @Override
    public void abortGame(GameId gameId) {
        registry.abortGame(gameId);
        broadcaster.publish(new GameEvent.GameAborted(gameId));
    }

    @Override
    public boolean abortGame(GameId gameId, @Nullable String actorUsername) {
        if (!canAbort(gameId, actorUsername)) {
            return false;
        }
        abortGame(gameId);
        return true;
    }

    @Override
    public Optional<String> hostUsernameOf(GameId gameId) {
        return registry.find(gameId).map(GameSession::hostUsername);
    }

    @Override
    public boolean canAbort(GameId gameId, @Nullable String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        return hostUsernameOf(gameId)
                .map(host -> host.equalsIgnoreCase(username))
                .orElse(true);
    }

    @Override
    public boolean hasActiveGame(GameId gameId) {
        return registry.find(gameId).map(s -> s.readState(GameState::active)).orElse(false);
    }

    @Override
    public boolean hasStartedGame(GameId gameId) {
        return registry.find(gameId)
                .map(s -> s.readState(state -> state.active() && state.started()))
                .orElse(false);
    }

    @Override
    public boolean joinGame(GameId gameId, int playerId) {
        boolean joined = registry.joinHumanPlayer(gameId, playerId);
        if (joined) {
            broadcaster.publish(new GameEvent.PlayerJoined(gameId, playerId));
        }
        return joined;
    }

    @Override
    public Optional<Integer> joinGame(GameId gameId, @Nullable String username) {
        Optional<Integer> seat = registry.claimSeat(gameId, username);
        seat.ifPresent(pid -> broadcaster.publish(new GameEvent.PlayerJoined(gameId, pid)));
        return seat;
    }

    @Override
    public Optional<Integer> seatFor(GameId gameId, @Nullable String username) {
        return registry.seatOf(gameId, username);
    }

    @Override
    public Optional<Integer> inviteUser(GameId gameId, @Nullable String invitee) {
        if (invitee == null || invitee.isBlank()) {
            return Optional.empty();
        }
        Integer seat = registry.writeState(gameId, state -> {
            if (!state.active() || state.started() || state.gameOver()) {
                return null;
            }
            if (state.invitedSeats().containsKey(invitee)) {
                return null;
            }
            if (state.seatByUser().containsKey(invitee)) {
                return null;
            }
            java.util.Set<Integer> reserved = new java.util.HashSet<>(state.invitedSeats().values());
            Integer candidate = state.players().stream()
                    .filter(p -> !p.ai())
                    .map(Player::id)
                    .filter(pid -> !state.joinedHumanPlayerIds().contains(pid))
                    .filter(pid -> !reserved.contains(pid))
                    .findFirst().orElse(null);
            if (candidate == null) {
                return null;
            }
            state.invitedSeats().put(invitee, candidate);
            return candidate;
        });
        return Optional.ofNullable(seat);
    }

    @Override
    public Optional<Integer> revokeInvite(GameId gameId, @Nullable String invitee) {
        if (invitee == null) {
            return Optional.empty();
        }
        Integer seat = registry.writeState(gameId, state -> state.invitedSeats().remove(invitee));
        return Optional.ofNullable(seat);
    }

    @Override
    public Map<String, Integer> invitedSeatsOf(GameId gameId) {
        return registry.readState(gameId, state -> Map.copyOf(state.invitedSeats()));
    }

    @Override
    public Optional<Integer> seatReservedFor(GameId gameId, @Nullable String invitee) {
        if (invitee == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(registry.readState(gameId, state -> state.invitedSeats().get(invitee)));
    }

    @Override
    public boolean canStartGame(GameId gameId) {
        return registry.canStartGame(gameId);
    }

    @Override
    public boolean startGame(GameId gameId) {
        boolean started = registry.startGame(gameId);
        if (started) {
            broadcaster.publish(new GameEvent.GameStarted(gameId));
        }
        return started;
    }

    @Override
    public PlayerViewState viewFor(GameId gameId, int playerId) {
        return registry.readState(gameId, state -> playerViewBuilder.forPlayer(state, playerId));
    }

    @Override
    public PlayerViewState viewForObserver(GameId gameId) {
        return registry.readState(gameId, playerViewBuilder::forObserver);
    }

    @Override
    public boolean observeGame(GameId gameId, @Nullable String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        boolean added = registry.writeState(gameId, state -> {
            if (!state.active() || state.gameOver()) {
                return false;
            }
            if (!state.observersAllowed()) {
                return false;
            }
            state.observers().add(username);
            return true;
        });
        if (added) {
            broadcaster.publish(new GameEvent.ObserverJoined(gameId, username));
        }
        return added;
    }

    @Override
    public boolean leaveObserve(GameId gameId, @Nullable String username) {
        if (username == null) {
            return false;
        }
        boolean removed = registry.writeState(gameId, state -> state.observers().remove(username));
        if (removed) {
            broadcaster.publish(new GameEvent.ObserverLeft(gameId, username));
            if (shouldAutoplay(gameId)) {
                autoplayRunner.autoplayToEnd(gameId);
            }
        }
        return removed;
    }

    private boolean shouldAutoplay(GameId gameId) {
        return registry.readState(gameId, state ->
                state.active()
                        && state.started()
                        && !state.gameOver()
                        && state.observers().isEmpty()
                        && state.joinedHumanPlayerIds().isEmpty());
    }

    @Override
    public boolean advanceForObserver(GameId gameId, @Nullable String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        TurnResult result = registry.writeState(gameId, state -> {
            if (!state.active() || !state.started() || state.gameOver()) {
                return TurnResult.REJECTED;
            }
            if (!state.observers().contains(username)) {
                return TurnResult.REJECTED;
            }
            if (!state.joinedHumanPlayerIds().isEmpty()) {
                return TurnResult.REJECTED;
            }
            turnEngine.advanceTurn(state);
            state.submittedThisTurn().clear();
            return captureTurnResult(state);
        });
        publishTurnResult(gameId, result);
        return result != TurnResult.REJECTED;
    }

    @Override
    public boolean isAiOnly(GameId gameId) {
        return registry.find(gameId)
                .map(s -> s.readState(state -> state.active() && state.joinedHumanPlayerIds().isEmpty()))
                .orElse(false);
    }

    @Override
    public boolean isObserver(GameId gameId, @Nullable String username) {
        if (username == null) {
            return false;
        }
        return registry.find(gameId)
                .map(s -> s.readState(state -> state.observers().contains(username)))
                .orElse(false);
    }

    @Override
    public boolean observersAllowed(GameId gameId) {
        return registry.find(gameId).map(s -> s.readState(GameState::observersAllowed)).orElse(false);
    }

    @Override
    public GameState snapshot(GameId gameId) {
        return registry.readState(gameId, GameState::copyOf);
    }

    @Override
    public int travelTurns(GameId gameId, int fromId, int toId) {
        return registry.readState(gameId, state -> state.travelRounds(fromId, toId));
    }

    @Override
    public boolean sendFleet(GameId gameId, int playerId, int fromId, int toId, int ships) {
        if (!hasStartedGame(gameId)) {
            return false;
        }
        return registry.writeState(gameId, state -> fleetService.queueSend(state, playerId, fromId, toId, ships));
    }

    @Override
    public boolean addStandingOrder(GameId gameId, int playerId, int fromId, int toId) {
        if (!hasStartedGame(gameId)) {
            return false;
        }
        return registry.writeState(gameId,
                state -> fleetService.addStandingOrder(state, playerId, fromId, toId) > 0);
    }

    @Override
    public boolean removeStandingOrder(GameId gameId, int playerId, int orderId) {
        return registry.writeState(gameId, state -> fleetService.removeStandingOrder(state, playerId, orderId));
    }

    @Override
    public boolean removeStandingOrderFrom(GameId gameId, int playerId, int fromSystemId) {
        return registry.writeState(gameId, state -> fleetService.removeStandingOrderFrom(state, playerId, fromSystemId));
    }

    @Override
    public boolean submitTurn(GameId gameId, int playerId) {
        SubmitResult result = registry.writeState(gameId, state -> {
            if (!state.active() || !state.started() || state.gameOver()) {
                return new SubmitResult(false, TurnResult.REJECTED);
            }
            if (!state.joinedHumanPlayerIds().contains(playerId)) {
                return new SubmitResult(false, TurnResult.REJECTED);
            }
            state.submittedThisTurn().add(playerId);
            TurnResult turn = maybeAdvance(state);
            return new SubmitResult(true, turn);
        });
        if (result.accepted()) {
            broadcaster.publish(new GameEvent.PlayerSubmitted(gameId, playerId));
            publishTurnResult(gameId, result.turnResult());
        }
        return result.accepted();
    }

    @Override
    public boolean kickHuman(GameId gameId, @Nullable String actorUsername, int seatId) {
        if (!canAbort(gameId, actorUsername)) {
            return false;
        }
        String kickedUser = usernameBySeat(gameId, seatId).orElse(null);
        KickResult kick = registry.writeState(gameId, state -> resolveKick(state, seatId, actorUsername, kickedUser));
        if (kick.accepted()) {
            broadcaster.publish(new GameEvent.SeatAbandoned(gameId, seatId));
            publishTurnResult(gameId, kick.turnResult());
            if (shouldAutoplay(gameId)) {
                autoplayRunner.autoplayToEnd(gameId);
            }
        }
        return kick.accepted();
    }

    @Override
    public boolean leaveGame(GameId gameId, int playerId) {
        SubmitResult result = registry.writeState(gameId, state -> {
            if (!state.active() || !state.started() || state.gameOver()) {
                return new SubmitResult(false, TurnResult.REJECTED);
            }
            if (!state.joinedHumanPlayerIds().contains(playerId)) {
                return new SubmitResult(false, TurnResult.REJECTED);
            }
            state.updatePlayer(playerId, Player::asAi);
            state.joinedHumanPlayerIds().remove(playerId);
            state.submittedThisTurn().remove(playerId);
            state.pendingOrders().remove(playerId);
            TurnResult turn = maybeAdvance(state);
            return new SubmitResult(true, turn);
        });
        if (result.accepted()) {
            broadcaster.publish(new GameEvent.SeatAbandoned(gameId, playerId));
            publishTurnResult(gameId, result.turnResult());
            usernameBySeat(gameId, playerId).ifPresent(leavingUsername ->
                    transferHostIfNeeded(gameId, leavingUsername));
            if (shouldAutoplay(gameId)) {
                autoplayRunner.autoplayToEnd(gameId);
            }
        }
        return result.accepted();
    }

    private Optional<String> usernameBySeat(GameId gameId, int playerId) {
        return registry.readState(gameId, state -> state.seatByUser().entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue() == playerId)
                .map(Map.Entry::getKey)
                .findFirst());
    }

    private void transferHostIfNeeded(GameId gameId, String leavingUsername) {
        GameSession session = registry.find(gameId).orElse(null);
        if (session == null) {
            return;
        }
        String host = session.hostUsername();
        if (host == null || !host.equalsIgnoreCase(leavingUsername)) {
            return;
        }

        String newHost = registry.readState(gameId, state -> state.joinedHumanPlayerIds().stream()
                .sorted()
                .map(pid -> state.seatByUser().entrySet().stream()
                        .filter(e -> e.getValue() != null && e.getValue().equals(pid))
                        .map(Map.Entry::getKey).findFirst().orElse(null))
                .filter(Objects::nonNull)
                .findFirst().orElse(null));
        session.transferHostTo(newHost);
        broadcaster.publish(new GameEvent.HostChanged(gameId, newHost));
    }

    private KickResult resolveKick(GameState state, int seatId, @Nullable String actorUsername, @Nullable String kickedUser) {
        if (!state.active() || state.gameOver()) {
            return new KickResult(false, TurnResult.REJECTED);
        }
        boolean existsAsHuman = state.players().stream().anyMatch(p -> p.id() == seatId && !p.ai());
        if (!existsAsHuman) {
            return new KickResult(false, TurnResult.REJECTED);
        }
        if (kickedUser != null && kickedUser.equalsIgnoreCase(actorUsername)) {
            return new KickResult(false, TurnResult.REJECTED);
        }
        if (!state.started()) {
            return kickBeforeStart(state, seatId, kickedUser);
        }
        return kickDuringGame(state, seatId, kickedUser);
    }

    private KickResult kickBeforeStart(GameState state, int seatId, @Nullable String kickedUser) {
        state.updatePlayer(seatId, Player::asAi);
        state.joinedHumanPlayerIds().remove(seatId);
        if (kickedUser != null) {
            state.seatByUser().remove(kickedUser);
        }
        return new KickResult(true, TurnResult.NONE);
    }

    private KickResult kickDuringGame(GameState state, int seatId, @Nullable String kickedUser) {
        if (!state.joinedHumanPlayerIds().contains(seatId)) {
            return new KickResult(false, TurnResult.REJECTED);
        }
        state.updatePlayer(seatId, Player::asAi);
        state.joinedHumanPlayerIds().remove(seatId);
        state.submittedThisTurn().remove(seatId);
        state.pendingOrders().remove(seatId);
        if (kickedUser != null) {
            state.seatByUser().remove(kickedUser);
        }
        return new KickResult(true, maybeAdvance(state));
    }

    private TurnResult maybeAdvance(GameState state) {
        if (state.joinedHumanPlayerIds().isEmpty()) {
            return TurnResult.NONE;
        }
        if (state.submittedThisTurn().containsAll(state.joinedHumanPlayerIds())) {
            turnEngine.advanceTurn(state);
            state.submittedThisTurn().clear();
            return captureTurnResult(state);
        }
        return TurnResult.NONE;
    }

    private TurnResult captureTurnResult(GameState state) {
        if (state.gameOver()) {
            return new TurnResult.Finished(state.turn(), state.winnerId());
        }
        return new TurnResult.Advanced(state.turn());
    }

    private void publishTurnResult(GameId gameId, TurnResult result) {
        switch (result) {
            case TurnResult.Advanced(int turn) -> broadcaster.publish(new GameEvent.TurnAdvanced(gameId, turn));
            case TurnResult.Finished(int turn, Integer winnerId) -> {
                broadcaster.publish(new GameEvent.TurnAdvanced(gameId, turn));
                broadcaster.publish(new GameEvent.GameFinished(gameId, winnerId));
            }
            case TurnResult.None _ -> {
                // no event — nothing to publish when no turn was processed
            }
            case TurnResult.Rejected _ -> {
                // no event — rejections are surfaced to callers, not broadcast
            }
        }
    }

    @Override
    public boolean hasSignificantEventsFor(GameId gameId, int playerId) {
        return registry.readState(gameId, state -> reportService.hasSignificantEventsFor(state, playerId));
    }

    @Override
    public boolean isWaitingForOtherPlayers(GameId gameId, int playerId) {
        return registry.readState(gameId, state ->
                state.submittedThisTurn().contains(playerId)
                        && !state.submittedThisTurn().containsAll(state.joinedHumanPlayerIds()));
    }

    @Override
    public boolean setFleetWait(GameId gameId, int playerId, int fleetId) {
        if (!hasStartedGame(gameId)) {
            return false;
        }
        return registry.writeState(gameId, state -> fleetService.queueWait(state, playerId, fleetId));
    }

    @Override
    public boolean cancelOrder(GameId gameId, int playerId, int orderIndex) {
        return registry.writeState(gameId, state -> {
            List<FleetOrder> orders = state.pendingOrders().get(playerId);
            if (orders == null || orderIndex < 0 || orderIndex >= orders.size()) {
                return false;
            }
            orders.remove(orderIndex);
            return true;
        });
    }

    @Override
    public boolean disbandFleet(GameId gameId, int playerId, int fleetId) {
        if (!hasStartedGame(gameId)) {
            return false;
        }
        return registry.writeState(gameId, state -> fleetService.queueDisband(state, playerId, fleetId));
    }

    private sealed interface TurnResult {
        TurnResult NONE = new None();
        TurnResult REJECTED = new Rejected();

        record Advanced(int turn) implements TurnResult {
        }

        record Finished(int turn, @Nullable Integer winnerId) implements TurnResult {
        }

        record None() implements TurnResult {
        }

        record Rejected() implements TurnResult {
        }
    }

    private record SubmitResult(boolean accepted, TurnResult turnResult) {
    }

    private record KickResult(boolean accepted, TurnResult turnResult) {
    }
}
