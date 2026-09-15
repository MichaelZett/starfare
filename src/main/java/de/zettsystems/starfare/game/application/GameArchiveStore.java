package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.domain.GameStateSnapshot;
import de.zettsystems.starfare.game.values.GameId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface GameArchiveStore {
    void save(GameSession session, GameStateSnapshot snapshot);
    Optional<ArchivedGame> load(GameId id);
    List<ArchivedGame> all();
    List<ArchivedGame> finishedBefore(Instant cutoff);
}
