package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.domain.GameSessionEntity;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.domain.GameStateSnapshot;
import de.zettsystems.starfare.game.values.GameId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Spring-injected GameSessionRepository and ObjectMapper are kept by reference for the bean's lifetime by design.")
public class JpaGameSessionStore implements GameSessionStore {

    private static final Logger LOG = LoggerFactory.getLogger(JpaGameSessionStore.class);

    private final GameSessionRepository repository;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<GameId, GameSession> cache = new ConcurrentHashMap<>();

    public JpaGameSessionStore(GameSessionRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Stellt beim Start alle Partien wieder her. Eine Zeile, die sich nicht lesen
     * laesst — etwa ein Snapshot aus einem aelteren Schema — kostet nur diese
     * Partie, nicht den Start der Anwendung. Die Zeile bleibt unangetastet in der
     * DB, damit sie nachtraeglich untersucht oder repariert werden kann; sie ist
     * lediglich nicht im Cache und damit fuer die Anwendung unsichtbar.
     */
    @PostConstruct
    void loadFromDatabase() {
        int restored = 0;
        int skipped = 0;
        for (GameSessionEntity entity : repository.findAll()) {
            if (restore(entity)) {
                restored++;
            } else {
                skipped++;
            }
        }
        if (skipped > 0) {
            LOG.warn("Restored {} game session(s), skipped {} unreadable one(s); "
                    + "the affected rows are still in game_sessions", restored, skipped);
        } else {
            LOG.info("Restored {} game session(s) from DB", restored);
        }
    }

    private boolean restore(GameSessionEntity entity) {
        try {
            GameStateSnapshot snapshot = objectMapper.readValue(entity.getStateJson(), GameStateSnapshot.class);
            GameState state = GameState.fromSnapshot(snapshot);
            String idValue = Objects.requireNonNull(entity.getId(), "Persisted game session must have an id");
            GameId id = GameId.of(idValue);
            cache.put(id, new GameSession(id, entity.getName(), entity.getHostPlayerId(), entity.getCreatedAt(), state));
            return true;
        } catch (RuntimeException e) {
            // Auch fromSnapshot kann an unvollstaendigen Altdaten scheitern, nicht nur Jackson.
            LOG.error("Skipping game session {}: snapshot could not be restored", entity.getId(), e);
            return false;
        }
    }

    @Override
    public void save(GameSession session) {
        cache.put(session.id(), session);
        GameStateSnapshot snapshot = session.readState(GameState::toSnapshot);
        try {
            String json = objectMapper.writeValueAsString(snapshot);
            GameSessionEntity entity = repository.findById(session.id().value())
                    .orElse(new GameSessionEntity(session.id().value(), session.name(),
                            session.hostPlayerId(), session.createdAt(), json));
            entity.replaceSnapshot(json);
            entity.transferHostTo(session.hostPlayerId());
            repository.save(entity);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to persist game session " + session.id(), e);
        }
    }

    @Override
    public Optional<GameSession> load(GameId id) {
        return Optional.ofNullable(cache.get(id));
    }

    @Override
    public List<GameId> listIds() {
        List<GameSession> all = new ArrayList<>(cache.values());
        all.sort(Comparator.comparing(GameSession::createdAt));
        return all.stream().map(GameSession::id).toList();
    }

    @Override
    public void delete(GameId id) {
        cache.remove(id);
        repository.deleteById(id.value());
    }
}
