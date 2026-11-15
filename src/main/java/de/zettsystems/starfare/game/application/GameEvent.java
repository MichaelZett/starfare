package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.values.GameId;
import org.jspecify.annotations.Nullable;
import java.time.Instant;

/**
 * Domain-level notifications published through {@link Broadcaster} so subscribed UIs
 * can refresh when someone else mutates a game.
 */
public sealed interface GameEvent {
    GameId gameId();

    record PlayerJoined(GameId gameId, int playerId) implements GameEvent {}

    record PlayerSubmitted(GameId gameId, int playerId) implements GameEvent {}

    record TurnAdvanced(GameId gameId, int turn) implements GameEvent {}

    record GameFinished(GameId gameId, @Nullable Integer winnerId, java.util.List<Integer> winnerIds,
                        @Nullable Integer allianceId) implements GameEvent {
        public GameFinished(GameId gameId, @Nullable Integer winnerId) {
            this(gameId, winnerId, winnerId == null ? java.util.List.of() : java.util.List.of(winnerId), null);
        }
        public GameFinished { winnerIds = java.util.List.copyOf(winnerIds); }
    }

    record SeatAbandoned(GameId gameId, int playerId) implements GameEvent {}

    record ObserverJoined(GameId gameId, String playerId) implements GameEvent {}

    record ObserverLeft(GameId gameId, String playerId) implements GameEvent {}

    record VisibilityChanged(GameId gameId) implements GameEvent {}

    record GameCreated(GameId gameId) implements GameEvent {}

    /**
     * The game was aborted and deleted. {@code running} tells whether it had started and not yet
     * ended; {@code accounts} are the accounts that held a seat, in seat order.
     */
    record GameAborted(GameId gameId, boolean running, java.util.List<String> accounts) implements GameEvent {
        public GameAborted(GameId gameId) {
            this(gameId, false, java.util.List.of());
        }
        public GameAborted { accounts = java.util.List.copyOf(accounts); }
    }

    record GameStarted(GameId gameId) implements GameEvent {}

    record GameContinued(GameId gameId) implements GameEvent {}

    record HostChanged(GameId gameId, @Nullable String newHostPlayerId) implements GameEvent {
    }

    record DiplomacyChanged(GameId gameId) implements GameEvent {}

    record ProductionAllocationChanged(GameId gameId) implements GameEvent {}

    record RoundRulesChanged(GameId gameId) implements GameEvent {}

    record ChatMessage(GameId gameId, String senderPlayerId, String text, Instant sentAt) implements GameEvent {}
}
