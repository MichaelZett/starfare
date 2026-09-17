package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameArchiveEntity;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.domain.GameStateSnapshot;
import de.zettsystems.starfare.game.values.GameId;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
class JpaGameArchiveStore implements GameArchiveStore {
    private final GameArchiveRepository repository;
    private final ObjectMapper objectMapper;
    JpaGameArchiveStore(GameArchiveRepository repository, ObjectMapper objectMapper) { this.repository = repository; this.objectMapper = objectMapper; }
    @Override public void save(GameSession session, GameStateSnapshot snapshot) {
        Instant finishedAt = snapshot.finishedAt();
        if (!snapshot.gameOver() || finishedAt == null) { return; }
        try {
            String json = objectMapper.writeValueAsString(snapshot);
            GameArchiveEntity entity = repository.findById(session.id().value()).orElseGet(() ->
                    new GameArchiveEntity(session.id().value(), session.name(), session.hostPlayerId(), finishedAt, json));
            entity.replaceSnapshot(json);
            repository.save(entity);
        } catch (JacksonException e) { throw new IllegalStateException("Failed to archive game " + session.id(), e); }
    }
    @Override public Optional<GameArchiveStore.ArchivedGame> load(GameId id) { return repository.findById(id.value()).flatMap(this::read); }
    @Override public List<GameArchiveStore.ArchivedGame> all() { return repository.findAll().stream().map(this::read).flatMap(Optional::stream).toList(); }
    @Override public List<GameArchiveStore.ArchivedGame> finishedBefore(Instant cutoff) { return repository.findAllByFinishedAtBeforeOrderByFinishedAtAsc(cutoff).stream().map(this::read).flatMap(Optional::stream).toList(); }
    private Optional<GameArchiveStore.ArchivedGame> read(GameArchiveEntity entity) {
        try {
            GameStateSnapshot snapshot = objectMapper.readValue(entity.getStateJson(), GameStateSnapshot.class);
            String entityId = Objects.requireNonNull(entity.getId(), "Persisted archive must have an id");
            return Optional.of(new GameArchiveStore.ArchivedGame(GameId.of(entityId), entity.getName(), entity.getHostPlayerId(), entity.getFinishedAt(), GameState.fromSnapshot(snapshot)));
        } catch (RuntimeException _) { return Optional.empty(); }
    }
}
