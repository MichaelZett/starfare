package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StarSystem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Entladen untätiger Einzelspieler-Partien: Eine wieder geladene Partie muss erneut
 * entladbar sein, und das Entladen darf sich nicht mit einem laufenden Speichern
 * verklemmen.
 */
class JpaGameSessionStoreUnloadTest extends AbstractIntegrationTest {

    /** Negativ: Jede bisherige Zugriffszeit liegt vor dem Stichtag. */
    private static final Duration ANY_ACCESS_IS_IDLE = Duration.ofSeconds(-1);

    @Autowired
    private JpaGameSessionStore store;

    @Autowired
    private GameSessionRepository repository;

    @Autowired
    private GameArchiveStore archives;

    @Autowired
    private ObjectMapper objectMapper;

    private GameId runningSingleHumanGame() {
        GameId id = registry.createGame(GameSetup.defaults(), "alice", "solo-game");
        registry.writeState(id, state -> {
            state.resetForNewGame();
            state.configureLobby(true, true);
            state.players().add(new Player(1, "P1", false, "#111111"));
            state.players().add(new Player(2, "P2", true, "#222222"));
            state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 10, 2, false));
            state.systems().add(new StarSystem(2, "S2", 1000, 0, 2, 10, 2, false));
            state.originalHumanPlayerIds().add(1);
            state.joinedHumanPlayerIds().add(1);
            state.seatByUser().put("alice", 1);
            state.start();
            return null;
        });
        return id;
    }

    @Test
    void reloadedGameCanBeUnloadedAgain() {
        GameId id = runningSingleHumanGame();

        assertThat(store.unloadInactiveSingleHumanGames(ANY_ACCESS_IS_IDLE)).isEqualTo(1);
        assertThat(store.loadedIds()).doesNotContain(id);

        assertThat(store.load(id)).as("aus der DB wieder geladen").isPresent();
        assertThat(store.unloadInactiveSingleHumanGames(ANY_ACCESS_IS_IDLE)).isEqualTo(1);
        assertThat(store.loadedIds()).doesNotContain(id);
    }

    @Test
    void unloadingDoesNotDeadlockWithASaveUnderTheWriteLock() {
        GameId id = runningSingleHumanGame();
        // Eigene Store-Instanz mit eigener GameSession: Verklemmt sie sich doch, scheitert
        // der Test am Timeout, statt dass das Aufräumen über die Registry mit hängen bleibt.
        JpaGameSessionStore isolated = new JpaGameSessionStore(repository, archives, objectMapper);
        GameSession session = isolated.load(id).orElseThrow();
        ExecutorService threads = Executors.newFixedThreadPool(2);
        CountDownLatch unloadStarted = new CountDownLatch(1);
        try {
            // Hält den Schreib-Lock, lässt währenddessen entladen und speichert danach.
            CompletableFuture<Integer> unload = CompletableFuture.supplyAsync(() -> session.writeStateAndThen(_ -> {
                CompletableFuture<Integer> started = CompletableFuture.supplyAsync(
                        () -> {
                            unloadStarted.countDown();
                            return isolated.unloadInactiveSingleHumanGames(ANY_ACCESS_IS_IDLE);
                        }, threads);
                assertThat(awaitUnloadStarted(unloadStarted)).isTrue();
                return started;
            }, isolated::save), threads).thenCompose(started -> started);

            assertThat(unload.orTimeout(15, TimeUnit.SECONDS).join()).isBetween(0, 1);
        } finally {
            threads.shutdownNow();
        }
    }

    private static boolean awaitUnloadStarted(CountDownLatch started) {
        try {
            return started.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
