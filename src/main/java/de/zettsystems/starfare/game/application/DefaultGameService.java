package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.fleet.application.FleetService;
import de.zettsystems.starfare.fleet.values.FleetOrder;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.GameSummary;
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
import java.util.Set;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.Instant;
import de.zettsystems.starfare.game.config.GameTimingProperties;

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
    private final GameTimingProperties timing;

    public DefaultGameService(GameRegistry registry, TurnEngine turnEngine, FleetService fleetService,
                              ReportService reportService, AutoplayRunner autoplayRunner, Broadcaster broadcaster,
                              PlayerViewBuilder playerViewBuilder, GameTimingProperties timing) {
        this.registry = registry;
        this.turnEngine = turnEngine;
        this.fleetService = fleetService;
        this.reportService = reportService;
        this.autoplayRunner = autoplayRunner;
        this.broadcaster = broadcaster;
        this.playerViewBuilder = playerViewBuilder;
        this.timing = timing;
    }

    @Override
    public GameId newGame(GameSetup setup, @Nullable String hostPlayerId, String name) {
        GameId id = registry.createGame(setup, hostPlayerId, name);
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
    public boolean abortGame(GameId gameId, @Nullable String actorPlayerId) {
        if (!canAbort(gameId, actorPlayerId)) {
            return false;
        }
        abortGame(gameId);
        return true;
    }

    @Override
    public Optional<String> hostPlayerIdOf(GameId gameId) {
        return registry.find(gameId).map(GameSession::hostPlayerId);
    }

    @Override
    public boolean canAbort(GameId gameId, @Nullable String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return false;
        }
        return hostPlayerIdOf(gameId)
                .map(host -> host.equals(playerId))
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
    public Optional<Integer> joinGame(GameId gameId, @Nullable String playerId) {
        Optional<Integer> seat = registry.claimSeat(gameId, playerId);
        seat.ifPresent(pid -> broadcaster.publish(new GameEvent.PlayerJoined(gameId, pid)));
        return seat;
    }

    @Override
    public Optional<Integer> seatFor(GameId gameId, @Nullable String playerId) {
        return registry.seatOf(gameId, playerId);
    }

    @Override
    public Optional<Integer> inviteUser(GameId gameId, @Nullable String inviteePlayerId) {
        if (inviteePlayerId == null || inviteePlayerId.isBlank()) {
            return Optional.empty();
        }
        Integer seat = registry.writeState(gameId, state -> {
            if (!state.active() || state.started() || state.gameOver()) {
                return null;
            }
            if (state.invitedSeats().containsKey(inviteePlayerId)) {
                return null;
            }
            if (state.seatByUser().containsKey(inviteePlayerId)) {
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
            state.invitedSeats().put(inviteePlayerId, candidate);
            return candidate;
        });
        return Optional.ofNullable(seat);
    }

    @Override
    public Optional<Integer> revokeInvite(GameId gameId, @Nullable String inviteePlayerId) {
        if (inviteePlayerId == null) {
            return Optional.empty();
        }
        Integer seat = registry.writeState(gameId, state -> state.invitedSeats().remove(inviteePlayerId));
        return Optional.ofNullable(seat);
    }

    @Override
    public Map<String, Integer> invitedSeatsOf(GameId gameId) {
        return registry.readState(gameId, state -> Map.copyOf(state.invitedSeats()));
    }

    @Override
    public Optional<Integer> seatReservedFor(GameId gameId, @Nullable String inviteePlayerId) {
        if (inviteePlayerId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(registry.readState(gameId, state -> state.invitedSeats().get(inviteePlayerId)));
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
    public boolean observeGame(GameId gameId, @Nullable String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return false;
        }
        boolean added = registry.writeState(gameId, state -> {
            if (!state.active() || state.gameOver()) {
                return false;
            }
            if (!state.observersAllowed()) {
                return false;
            }
            state.observers().add(playerId);
            return true;
        });
        if (added) {
            broadcaster.publish(new GameEvent.ObserverJoined(gameId, playerId));
        }
        return added;
    }

    @Override
    public boolean leaveObserve(GameId gameId, @Nullable String playerId) {
        if (playerId == null) {
            return false;
        }
        boolean removed = registry.writeState(gameId, state -> state.observers().remove(playerId));
        if (removed) {
            broadcaster.publish(new GameEvent.ObserverLeft(gameId, playerId));
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
    public boolean advanceForObserver(GameId gameId, @Nullable String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return false;
        }
        TurnResult result = registry.writeState(gameId, state -> {
            if (!state.active() || !state.started() || state.gameOver()) {
                return TurnResult.REJECTED;
            }
            if (!state.observers().contains(playerId)) {
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
        return !TurnResult.REJECTED.equals(result);
    }

    @Override
    public boolean isAiOnly(GameId gameId) {
        return registry.find(gameId)
                .map(s -> s.readState(state -> state.active() && state.joinedHumanPlayerIds().isEmpty()))
                .orElse(false);
    }

    @Override
    public boolean isObserver(GameId gameId, @Nullable String playerId) {
        if (playerId == null) {
            return false;
        }
        return registry.find(gameId)
                .map(s -> s.readState(state -> state.observers().contains(playerId)))
                .orElse(false);
    }

    @Override
    public boolean observersAllowed(GameId gameId) {
        return registry.find(gameId).map(s -> s.readState(GameState::observersAllowed)).orElse(false);
    }

    @Override
    public GameSummary summaryOf(GameId gameId) {
        String name = gameNameOf(gameId);
        String hostPlayerId = hostPlayerIdOf(gameId).orElse(null);
        return registry.readState(gameId, state -> new GameSummary(
                gameId, name, hostPlayerId, state.turn(), state.started(), state.gameOver(),
                state.observersAllowed(), state.reentryAllowed(),
                List.copyOf(state.players()), Set.copyOf(state.joinedHumanPlayerIds()),
                Map.copyOf(state.seatByUser()), Map.copyOf(state.invitedSeats())));
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
    public boolean expireInactiveSeats(GameId gameId) {
        // Billiger Vorfilter: writeState persistiert bei jedem Aufruf einen Snapshot,
        // der Scheduler laeuft aber alle 30 s ueber saemtliche Partien.
        boolean expired = registry.readState(gameId, this::hasExpiredSeats);
        if (!expired) {
            return false;
        }
        ExpiryResult result = registry.writeState(gameId, state -> {
            if (!hasExpiredSeats(state)) {
                return ExpiryResult.none();
            }
            Set<Integer> inactive = state.joinedHumanPlayerIds().stream()
                    .filter(id -> !state.submittedThisTurn().contains(id))
                    .collect(Collectors.toUnmodifiableSet());
            List<String> abandonedBy = inactive.stream()
                    .map(id -> state.seatByUser().entrySet().stream()
                            .filter(entry -> Objects.equals(entry.getValue(), id))
                            .map(Map.Entry::getKey).findFirst().orElse(null))
                    .filter(Objects::nonNull)
                    .toList();
            inactive.forEach(id -> {
                state.updatePlayer(id, Player::asAi);
                state.joinedHumanPlayerIds().remove(id);
                state.pendingOrders().remove(id);
                // Der Sitz gehoert ab jetzt dauerhaft der KI, auch bei erlaubtem Wiedereinstieg.
                state.seatByUser().entrySet().removeIf(entry -> Objects.equals(entry.getValue(), id));
            });
            return new ExpiryResult(inactive, abandonedBy, maybeAdvance(state));
        });
        if (result.seats().isEmpty()) {
            return false;
        }
        result.seats().forEach(seatId -> broadcaster.publish(new GameEvent.SeatAbandoned(gameId, seatId)));
        publishTurnResult(gameId, result.turnResult());
        result.abandonedBy().forEach(playerId -> transferHostIfNeeded(gameId, playerId));
        if (shouldAutoplay(gameId)) {
            autoplayRunner.autoplayToEnd(gameId);
        }
        return true;
    }

    private boolean hasExpiredSeats(GameState state) {
        return state.active() && state.started() && !state.gameOver()
                && !state.turnStartedAt().plus(timing.inactivityTimeout()).isAfter(Instant.now())
                && !state.joinedHumanPlayerIds().stream().allMatch(state.submittedThisTurn()::contains);
    }

    private record ExpiryResult(Set<Integer> seats, List<String> abandonedBy, TurnResult turnResult) {
        static ExpiryResult none() {
            return new ExpiryResult(Set.of(), List.of(), TurnResult.REJECTED);
        }
    }

    @Override
    public boolean kickHuman(GameId gameId, @Nullable String actorPlayerId, int seatId) {
        if (!canAbort(gameId, actorPlayerId)) {
            return false;
        }
        String kickedPlayerId = playerIdBySeat(gameId, seatId).orElse(null);
        KickResult kick = registry.writeState(gameId, state -> resolveKick(state, seatId, actorPlayerId, kickedPlayerId));
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
            playerIdBySeat(gameId, playerId).ifPresent(leavingPlayerId ->
                    transferHostIfNeeded(gameId, leavingPlayerId));
            if (shouldAutoplay(gameId)) {
                autoplayRunner.autoplayToEnd(gameId);
            }
        }
        return result.accepted();
    }

    private Optional<String> playerIdBySeat(GameId gameId, int playerId) {
        return registry.readState(gameId, state -> state.seatByUser().entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue() == playerId)
                .map(Map.Entry::getKey)
                .findFirst());
    }

    private void transferHostIfNeeded(GameId gameId, String leavingPlayerId) {
        GameSession session = registry.find(gameId).orElse(null);
        if (session == null) {
            return;
        }
        String host = session.hostPlayerId();
        if (host == null || !host.equals(leavingPlayerId)) {
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

    private KickResult resolveKick(GameState state, int seatId, @Nullable String actorPlayerId, @Nullable String kickedPlayerId) {
        if (!state.active() || state.gameOver()) {
            return new KickResult(false, TurnResult.REJECTED);
        }
        boolean existsAsHuman = state.players().stream().anyMatch(p -> p.id() == seatId && !p.ai());
        if (!existsAsHuman) {
            return new KickResult(false, TurnResult.REJECTED);
        }
        if (kickedPlayerId != null && kickedPlayerId.equals(actorPlayerId)) {
            return new KickResult(false, TurnResult.REJECTED);
        }
        if (!state.started()) {
            return kickBeforeStart(state, seatId, kickedPlayerId);
        }
        return kickDuringGame(state, seatId, kickedPlayerId);
    }

    private KickResult kickBeforeStart(GameState state, int seatId, @Nullable String kickedPlayerId) {
        state.updatePlayer(seatId, Player::asAi);
        state.joinedHumanPlayerIds().remove(seatId);
        if (kickedPlayerId != null) {
            state.seatByUser().remove(kickedPlayerId);
        }
        return new KickResult(true, TurnResult.NONE);
    }

    private KickResult kickDuringGame(GameState state, int seatId, @Nullable String kickedPlayerId) {
        if (!state.joinedHumanPlayerIds().contains(seatId)) {
            return new KickResult(false, TurnResult.REJECTED);
        }
        state.updatePlayer(seatId, Player::asAi);
        state.joinedHumanPlayerIds().remove(seatId);
        state.submittedThisTurn().remove(seatId);
        state.pendingOrders().remove(seatId);
        if (kickedPlayerId != null) {
            state.seatByUser().remove(kickedPlayerId);
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
