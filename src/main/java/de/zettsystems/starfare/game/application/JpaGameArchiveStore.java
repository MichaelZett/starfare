package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameArchiveEntity;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.platform.ArchiveListing;
import de.zettsystems.starfare.game.domain.GameStateSnapshot;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.RulesetCatalog;
import de.zettsystems.starfare.game.values.RulesetRef;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Repository
class JpaGameArchiveStore implements GameArchiveStore {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(JpaGameArchiveStore.class);
    private final GameArchiveRepository repository;
    private final ObjectMapper objectMapper;
    private final RulesetCatalog catalog;
    private final EntityManager entityManager;
    private final TransactionTemplate transactions;

    JpaGameArchiveStore(GameArchiveRepository repository, ObjectMapper objectMapper, RulesetCatalog catalog,
                        EntityManager entityManager, PlatformTransactionManager transactionManager) {
        this.repository = repository; this.objectMapper = objectMapper; this.catalog = catalog;
        this.entityManager = entityManager; this.transactions = new TransactionTemplate(transactionManager);
    }

    /** Archives stored before the listing existed get it at the start, so that they show in the lists. */
    @PostConstruct
    void listStoredArchives() {
        int listed = listUnlisted();
        if (listed > 0) {
            LOG.info("Filled the listing of {} game archive(s)", listed);
        }
    }

    @Override
    @Transactional
    public void save(GameSession session, GameStateSnapshot snapshot) {
        RulesetRef ref = snapshot.ruleset();
        catalog.requireSupported(ref == null ? RulesetRef.SECTOR_FORCES : ref);
        Instant finishedAt = snapshot.finishedAt();
        if (!snapshot.gameOver() || finishedAt == null) { return; }
        try {
            String json = objectMapper.writeValueAsString(snapshot);
            GameArchiveEntity entity = repository.findById(session.id().value()).orElseGet(() ->
                    new GameArchiveEntity(session.id().value(), session.name(), session.hostPlayerId(), finishedAt, json));
            entity.replaceSnapshot(json);
            session.readState(state -> { ArchiveListing.list(entity, state); return null; });
            repository.save(entity);
        } catch (JacksonException e) { throw new IllegalStateException("Failed to archive game " + session.id(), e); }
    }
    @Override public Optional<GameArchiveStore.ArchivedGame> load(GameId id) { return repository.findById(id.value()).flatMap(this::read); }
    @Override public List<GameArchiveStore.ArchivedGame> all() { return repository.findAll().stream().map(this::read).flatMap(Optional::stream).toList(); }

    @Override
    @Transactional(readOnly = true)
    public List<ListedGame> listedWithSeat(String account, Set<GameId> except, int offset, int limit) {
        // An empty "not in" is not portable JPQL: leave the condition out instead.
        String exclusion = except.isEmpty() ? "" : " and a.id not in :except";
        TypedQuery<Object[]> page = entityManager.createQuery("select a.id, a.name, a.finishedAt, a.tutorial, a.aborted"
                        + " from GameArchiveEntity a join a.seats s where s.account = :account" + exclusion
                        + " order by a.finishedAt desc, collate(a.id as ucs_basic)", Object[].class)
                .setParameter("account", account).setFirstResult(offset).setMaxResults(limit);
        if (!except.isEmpty()) {
            page.setParameter("except", except.stream().map(GameId::value).toList());
        }
        List<Object[]> rows = page.getResultList();
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<String, List<GameArchiveEntity.Seat>> seats = new LinkedHashMap<>();
        rows.forEach(row -> seats.put((String) row[0], new ArrayList<>()));
        entityManager.createQuery("select a.id, s from GameArchiveEntity a join a.seats s where a.id in :ids", Object[].class)
                .setParameter("ids", List.copyOf(seats.keySet())).getResultList()
                .forEach(row -> seats.computeIfAbsent((String) row[0], _ -> new ArrayList<>()).add((GameArchiveEntity.Seat) row[1]));
        return rows.stream().map(row -> new ListedGame(GameId.of((String) row[0]), (String) row[1], (Instant) row[2],
                (Boolean) row[3], (Boolean) row[4], seats.getOrDefault((String) row[0], List.of()))).toList();
    }

    @Override
    public int listUnlisted() {
        int listed = 0;
        for (String id : repository.findIdsNotListed()) {
            // One transaction per archive: a broken one costs only itself.
            transactions.executeWithoutResult(_ -> repository.findById(id).ifPresent(this::listStored));
            listed++;
        }
        return listed;
    }

    private void listStored(GameArchiveEntity entity) {
        read(entity).ifPresentOrElse(game -> ArchiveListing.list(entity, game.state()),
                () -> ArchiveListing.unlistable(entity));
    }

    @Override public List<GameId> idsFinishedBefore(Instant cutoff) { return repository.findIdsFinishedBefore(cutoff).stream().map(GameId::of).toList(); }
    @Override public void delete(GameId id) { repository.deleteById(id.value()); }
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
