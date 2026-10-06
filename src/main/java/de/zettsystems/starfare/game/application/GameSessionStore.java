package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.values.GameId;

import java.util.List;
import java.util.Optional;
import java.time.Duration;

/**
 * Persistence contract for {@link GameSession} instances, backed by PostgreSQL snapshots
 * and an in-memory session cache in production.
 */
public interface GameSessionStore {
    void save(GameSession session);

    Optional<GameSession> load(GameId id);

    List<GameId> listIds();

    default List<GameId> loadedIds() {
        return listIds();
    }

    void delete(GameId id);

    default void touch(GameId id) {
    }

    default int unloadInactiveSingleHumanGames(Duration inactivity) {
        return 0;
    }
}
