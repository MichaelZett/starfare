package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractRepositoryTest;
import de.zettsystems.starfare.game.domain.GameSessionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GameSessionRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private GameSessionRepository repository;

    @BeforeEach
    void clearSessions() {
        // Other test classes may have committed game_sessions through the
        // production save path; @Transactional rollback can't reach those.
        repository.deleteAll();
    }

    @Test
    void findAllOrdersByCreatedAtAscending() {
        Instant t0 = Instant.parse("2026-04-22T08:00:00Z");
        repository.save(new GameSessionEntity("game-c", "Third", "alice", t0.plusSeconds(20), "{}"));
        repository.save(new GameSessionEntity("game-a", "First", "alice", t0, "{}"));
        repository.save(new GameSessionEntity("game-b", "Second", "alice", t0.plusSeconds(10), "{}"));

        List<GameSessionEntity> ordered = repository.findAllByOrderByCreatedAtAsc();

        assertThat(ordered.stream().map(GameSessionEntity::getId).toList()).containsExactlyElementsOf(List.of("game-a", "game-b", "game-c"));
    }

    @Test
    void replaceSnapshotPersistsOnSave() {
        Instant now = Instant.parse("2026-04-22T08:00:00Z");
        GameSessionEntity stored = repository.saveAndFlush(
                new GameSessionEntity("game-x", "X", null, now, "{\"turn\":1}"));

        stored.replaceSnapshot("{\"turn\":2}");
        repository.saveAndFlush(stored);

        GameSessionEntity reloaded = repository.findById("game-x").orElseThrow();
        assertThat(reloaded.getStateJson()).isEqualTo("{\"turn\":2}");
    }
}
