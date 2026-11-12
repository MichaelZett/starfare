package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSummary;

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

    /** Drops a derived cache after an externally committed snapshot update; uncached stores need no action. */
    default void invalidate(GameId id) { }


    default void touch(GameId id) {
    }

    default int unloadInactiveSingleHumanGames(Duration inactivity) {
        return 0;
    }

    /**
     * Summary taken when the game was unloaded; empty once it is loaded again. Lets the lobby list
     * an unloaded game without restoring its snapshot.
     */
    default Optional<GameSummary> parkedSummary(GameId id) {
        return Optional.empty();
    }
}
