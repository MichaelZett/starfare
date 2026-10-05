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

    record GameAborted(GameId gameId) implements GameEvent {}

    record GameStarted(GameId gameId) implements GameEvent {}

    record GameContinued(GameId gameId) implements GameEvent {}

    record HostChanged(GameId gameId, @Nullable String newHostPlayerId) implements GameEvent {
    }

    record DiplomacyChanged(GameId gameId) implements GameEvent {}

    record ProductionAllocationChanged(GameId gameId) implements GameEvent {}

    record RoundRulesChanged(GameId gameId) implements GameEvent {}

    record ChatMessage(GameId gameId, String senderPlayerId, String text, Instant sentAt) implements GameEvent {}
}
