package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.values.GameId;

import java.util.List;
import java.util.Optional;

/**
 * Persistence contract for {@link GameSession} instances. Only the in-memory implementation exists
 * today; a DB-backed one may follow.
 */
public interface GameSessionStore {
    void save(GameSession session);

    Optional<GameSession> load(GameId id);

    List<GameId> listIds();

    void delete(GameId id);
}
