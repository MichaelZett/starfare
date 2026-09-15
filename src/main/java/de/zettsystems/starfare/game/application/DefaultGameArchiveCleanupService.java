package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.config.GameArchiveCleanupProperties;
import de.zettsystems.starfare.game.values.GameId;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;

@Service
class DefaultGameArchiveCleanupService implements GameArchiveCleanupService {
    private final GameArchiveStore archives; private final GameSessionStore sessions; private final GameArchiveCleanupProperties properties;
    DefaultGameArchiveCleanupService(GameArchiveStore archives, GameSessionStore sessions, GameArchiveCleanupProperties properties) { this.archives = archives; this.sessions = sessions; this.properties = properties; }
    @Override public List<GameId> preview() { return archives.finishedBefore(Instant.now().minus(properties.retention())).stream().map(ArchivedGame::id).filter(id -> sessions.load(id).isPresent()).toList(); }
    @Override public int removeExpired() { List<GameId> candidates = preview(); for (GameId id : candidates) { sessions.load(id).ifPresent(session -> { if (session.readState(state -> state.gameOver() && state.finishedAt() != null)) sessions.delete(id); }); } return candidates.size(); }
}
