package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.domain.GameSessionEntity;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Eine unlesbare Zeile in {@code game_sessions} darf nur diese Partie kosten,
 * nicht den Start der Anwendung — sonst legt ein einziger Snapshot aus einem
 * älteren Schema die ganze Instanz still.
 */
class JpaGameSessionStoreRestoreTest extends AbstractIntegrationTest {

    private static final String BROKEN_ID = "broken-session";

    @Autowired
    private JpaGameSessionStore store;

    @Autowired
    private GameSessionRepository repository;

    @AfterEach
    void dropBrokenRow() {
        repository.deleteById(BROKEN_ID);
    }

    private void persistBrokenRow(String stateJson) {
        repository.saveAndFlush(new GameSessionEntity(
                BROKEN_ID, "Broken", "alice", Instant.parse("2026-04-22T08:00:00Z"), stateJson));
    }

    @Test
    void unreadableSnapshotIsSkippedInsteadOfFailingStartup() {
        GameId healthy = registry.createGame(GameSetup.defaults(), "alice", "healthy");
        persistBrokenRow("{}");

        assertThatCode(store::loadFromDatabase).doesNotThrowAnyException();

        assertThat(store.load(GameId.of(BROKEN_ID))).as("defekte Partie bleibt unsichtbar").isEmpty();
        assertThat(store.load(healthy)).as("gesunde Partie ist da").isPresent();
        assertThat(store.listIds()).doesNotContain(GameId.of(BROKEN_ID)).contains(healthy);
    }

    @Test
    void skippedRowStaysInTheDatabaseForInspection() {
        persistBrokenRow("{}");

        store.loadFromDatabase();

        assertThat(repository.findById(BROKEN_ID))
                .as("nicht stillschweigend loeschen, damit die Partie reparierbar bleibt")
                .isPresent();
    }

    @Test
    void malformedJsonIsSkippedToo() {
        persistBrokenRow("not json at all");

        assertThatCode(store::loadFromDatabase).doesNotThrowAnyException();

        assertThat(store.load(GameId.of(BROKEN_ID))).isEmpty();
    }
}
