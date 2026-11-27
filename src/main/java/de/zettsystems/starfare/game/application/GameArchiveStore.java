package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameArchiveEntity;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.domain.GameStateSnapshot;
import de.zettsystems.starfare.game.values.GameId;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    /** What a list of finished games shows of an archive, read without its snapshot. */
    record ListedGame(GameId id, String name, Instant finishedAt, boolean tutorial, boolean aborted,
                      List<GameArchiveEntity.Seat> seats) {
        public ListedGame {
            seats = List.copyOf(seats);
        }
    }

    /** Archives the finished game with what a list of finished games shows of it. */
    void save(GameSession session, GameStateSnapshot snapshot);
    Optional<ArchivedGame> load(GameId id);
    List<ArchivedGame> all();
    /**
     * One page of the listed games the account held a seat in, except the given ones, latest end first, then by id
     * in code point order; reads no snapshot.
     */
    List<ListedGame> listedWithSeat(String account, Set<GameId> except, int offset, int limit);
    /** Fills the listing of archives stored without one, once; returns how many it filled. */
    int listUnlisted();
    /** The games that ended before the cutoff, oldest first; reads no snapshot. */
    List<GameId> idsFinishedBefore(Instant cutoff);
    void delete(GameId id);
}
