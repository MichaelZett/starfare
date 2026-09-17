package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.domain.GameStateSnapshot;
import de.zettsystems.starfare.game.values.GameId;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface GameArchiveStore {
    record ArchivedGame(GameId id, String name, @Nullable String hostPlayerId, Instant finishedAt, GameState state) {
        public ArchivedGame {
            state = GameState.copyOf(state);
        }

        @Override
        public GameState state() {
            return GameState.copyOf(state);
        }
    }

    void save(GameSession session, GameStateSnapshot snapshot);
    Optional<ArchivedGame> load(GameId id);
    List<ArchivedGame> all();
    List<ArchivedGame> finishedBefore(Instant cutoff);
}
