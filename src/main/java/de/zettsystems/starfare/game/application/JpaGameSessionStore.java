package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.domain.GameSessionEntity;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.domain.GameStateSnapshot;
import de.zettsystems.starfare.game.values.GameId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Spring-injected GameSessionRepository and ObjectMapper are kept by reference for the bean's lifetime by design.")
public class JpaGameSessionStore implements GameSessionStore {

    private final GameSessionRepository repository;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<GameId, GameSession> cache = new ConcurrentHashMap<>();

    public JpaGameSessionStore(GameSessionRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void loadFromDatabase() {
        repository.findAll().forEach(entity -> {
            try {
                GameStateSnapshot snapshot = objectMapper.readValue(entity.getStateJson(), GameStateSnapshot.class);
                GameState state = GameState.fromSnapshot(snapshot);
                GameId id = GameId.of(entity.getId());
                cache.put(id, new GameSession(id, entity.getName(), entity.getHostUsername(), entity.getCreatedAt(), state));
            } catch (JacksonException e) {
                throw new IllegalStateException("Failed to restore game session " + entity.getId() + " from DB", e);
            }
        });
    }

    @Override
    public void save(GameSession session) {
        cache.put(session.id(), session);
        GameStateSnapshot snapshot = session.readState(GameState::toSnapshot);
        try {
            String json = objectMapper.writeValueAsString(snapshot);
            GameSessionEntity entity = repository.findById(session.id().value())
                    .orElse(new GameSessionEntity(session.id().value(), session.name(),
                            session.hostUsername(), session.createdAt(), json));
            entity.replaceSnapshot(json);
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
