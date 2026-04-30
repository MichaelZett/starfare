package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.values.GameId;
import org.jspecify.annotations.Nullable;

/**
 * Domain-level notifications published through {@link Broadcaster} so subscribed UIs
 * can refresh when someone else mutates a game.
 */
public sealed interface GameEvent {
    GameId gameId();

    record PlayerJoined(GameId gameId, int playerId) implements GameEvent {}

    record PlayerSubmitted(GameId gameId, int playerId) implements GameEvent {}

    record TurnAdvanced(GameId gameId, int turn) implements GameEvent {}

    record GameFinished(GameId gameId, @Nullable Integer winnerId) implements GameEvent {
    }

    record SeatAbandoned(GameId gameId, int playerId) implements GameEvent {}

    record ObserverJoined(GameId gameId, String username) implements GameEvent {}

    record ObserverLeft(GameId gameId, String username) implements GameEvent {}

    record GameCreated(GameId gameId) implements GameEvent {}

    record GameAborted(GameId gameId) implements GameEvent {}

    record GameStarted(GameId gameId) implements GameEvent {}

    record HostChanged(GameId gameId, @Nullable String newHostUsername) implements GameEvent {
    }
}
