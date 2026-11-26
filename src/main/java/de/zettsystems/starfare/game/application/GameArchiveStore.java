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
    /** The games the account held a seat in, latest end first, then by id; reads no snapshot. */
    List<GameId> idsWithSeat(String account);
    /** The readable archives of these games in the given order. */
    List<ArchivedGame> load(List<GameId> ids);
    /** The games that ended before the cutoff, oldest first; reads no snapshot. */
    List<GameId> idsFinishedBefore(Instant cutoff);
    void delete(GameId id);
}
