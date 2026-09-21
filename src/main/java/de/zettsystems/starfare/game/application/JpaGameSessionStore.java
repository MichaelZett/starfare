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
import java.time.Duration;
import java.time.Instant;

@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Spring-injected GameSessionRepository and ObjectMapper are kept by reference for the bean's lifetime by design.")
public class JpaGameSessionStore implements GameSessionStore {

    private static final Logger LOG = LoggerFactory.getLogger(JpaGameSessionStore.class);

    private final GameSessionRepository repository;
    private final GameArchiveStore archives;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<GameId, GameSession> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<GameId, Instant> lastAccess = new ConcurrentHashMap<>();
    private final Set<GameId> readableIds = ConcurrentHashMap.newKeySet();
    private final Object cacheMonitor = new Object();

    public JpaGameSessionStore(GameSessionRepository repository, GameArchiveStore archives, ObjectMapper objectMapper) {
        this.repository = repository;
        this.archives = archives;
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
            cache.putIfAbsent(id, new GameSession(id, entity.getName(), entity.getHostPlayerId(), entity.getCreatedAt(), state));
            readableIds.add(id);
            return true;
        } catch (RuntimeException e) {
            // Auch fromSnapshot kann an unvollstaendigen Altdaten scheitern, nicht nur Jackson.
            LOG.error("Skipping game session {}: snapshot could not be restored", entity.getId(), e);
            return false;
        }
    }

    @Override
    public void save(GameSession session) {
        synchronized (cacheMonitor) {
            cache.put(session.id(), session);
            readableIds.add(session.id());
            lastAccess.put(session.id(), Instant.now());
        }
        GameStateSnapshot snapshot = session.readState(GameState::toSnapshot);
        try {
            String json = objectMapper.writeValueAsString(snapshot);
            GameSessionEntity entity = repository.findById(session.id().value())
                    .orElse(new GameSessionEntity(session.id().value(), session.name(),
                            session.hostPlayerId(), session.createdAt(), json));
            entity.replaceSnapshot(json);
            entity.transferHostTo(session.hostPlayerId());
            repository.save(entity);
            archives.save(session, snapshot);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to persist game session " + session.id(), e);
        }
    }

    @Override
    public Optional<GameSession> load(GameId id) {
        synchronized (cacheMonitor) {
            GameSession cached = cache.get(id);
            if (cached != null) {
                return Optional.of(cached);
            }
            return repository.findById(id.value()).filter(this::restore).map(_ -> cache.get(id));
        }
    }

    @Override
    public List<GameId> listIds() {
        return repository.findAllByOrderByCreatedAtAsc().stream()
                .map(entity -> GameId.of(Objects.requireNonNull(entity.getId())))
                .filter(readableIds::contains).toList();
    }

    @Override
    public List<GameId> loadedIds() {
        return List.copyOf(cache.keySet());
    }

    @Override
    public void delete(GameId id) {
        repository.deleteById(id.value());
        synchronized (cacheMonitor) {
            cache.remove(id);
            lastAccess.remove(id);
            readableIds.remove(id);
        }
    }

    @Override
    public void touch(GameId id) {
        if (cache.containsKey(id)) {
            lastAccess.put(id, Instant.now());
        }
    }

    @Override
    public int unloadInactiveSingleHumanGames(Duration inactivity) {
        Instant cutoff = Instant.now().minus(inactivity);
        int unloaded = 0;
        synchronized (cacheMonitor) {
        for (var entry : cache.entrySet()) {
            GameId id = entry.getKey();
            GameSession session = entry.getValue();
            Instant accessed = lastAccess.get(id);
            boolean idle = accessed != null && accessed.isBefore(cutoff);
            boolean singleHuman = session.readState(state -> state.active() && state.started() && !state.gameOver()
                    && state.originalHumanPlayerIds().size() == 1);
            if (idle && singleHuman && cache.remove(id, session)) {
                lastAccess.remove(id);
                unloaded++;
            }
        }
        }
        return unloaded;
    }
}
