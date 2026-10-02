package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameArchiveEntity;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.domain.GameStateSnapshot;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.RulesetCatalog;
import de.zettsystems.starfare.game.values.RulesetRef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
class JpaGameArchiveStore implements GameArchiveStore {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(JpaGameArchiveStore.class);
    private final GameArchiveRepository repository;
    private final ObjectMapper objectMapper;
    private final RulesetCatalog catalog;
    JpaGameArchiveStore(GameArchiveRepository repository, ObjectMapper objectMapper) {
        this(repository, objectMapper, RulesetCatalog.builtIn());
    }
    @Autowired
    JpaGameArchiveStore(GameArchiveRepository repository, ObjectMapper objectMapper,
                        RulesetCatalog catalog) {
        this.repository = repository; this.objectMapper = objectMapper; this.catalog = catalog;
    }
    @Override public void save(GameSession session, GameStateSnapshot snapshot) {
        RulesetRef ref = snapshot.ruleset();
        catalog.requireSupported(ref == null ? RulesetRef.SECTOR_FORCES : ref);
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
            GameState state = GameState.fromSnapshot(snapshot);
            catalog.requireSupported(state.ruleset());
            String entityId = Objects.requireNonNull(entity.getId(), "Persisted archive must have an id");
            return Optional.of(new GameArchiveStore.ArchivedGame(GameId.of(entityId), entity.getName(), entity.getHostPlayerId(), entity.getFinishedAt(), state));
        } catch (RuntimeException exception) {
            LOG.error("Skipping game archive {}: snapshot or ruleset could not be restored", entity.getId(), exception);
            return Optional.empty();
        }
    }
}
