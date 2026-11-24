package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.config.GameArchiveCleanupProperties;
import de.zettsystems.starfare.game.values.GameId;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Removes finished games once their retention has passed: the stored game, its archive entry and the
 * battle acknowledgements go together, so the archive lists only what can still be reviewed. A game
 * continued after its victory runs again and stays. The removal announces nothing to the platform: the
 * platform keeps who played against whom and the result in its statistics.
 */
@Service
class DefaultGameArchiveCleanupService implements GameArchiveCleanupService {
    private final GameArchiveStore archives;
    private final GameSessionStore sessions;
    private final BattleAcknowledgementService acknowledgements;
    private final GameArchiveCleanupProperties properties;

    DefaultGameArchiveCleanupService(GameArchiveStore archives, GameSessionStore sessions,
                                     BattleAcknowledgementService acknowledgements,
                                     GameArchiveCleanupProperties properties) {
        this.archives = archives;
        this.sessions = sessions;
        this.acknowledgements = acknowledgements;
        this.properties = properties;
    }

    @Override
    public List<GameId> preview() {
        return archives.idsFinishedBefore(Instant.now().minus(properties.retention())).stream()
                .filter(this::stillFinished)
                .toList();
    }

    @Override
    public int removeExpired() {
        int removed = 0;
        for (GameId id : preview()) {
            // The stored game first: should the archive entry fail, the next run finds it again.
            sessions.delete(id);
            acknowledgements.forgetGame(id);
            archives.delete(id);
            removed++;
        }
        return removed;
    }

    /** Whether the game is over, or gone already; a game continued after its victory is not. */
    private boolean stillFinished(GameId id) {
        return sessions.load(id)
                .map(session -> session.readState(state -> state.gameOver() && state.finishedAt() != null))
                .orElse(true);
    }
}
