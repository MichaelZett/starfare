package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StarSystem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class AutoplayToEndTest extends AbstractIntegrationTest {

    @Autowired
    private GameService game;

    @Autowired
    private AutoplayRunner autoplay;

    private GameId setupAiDominantGame() {
        GameId id = registry.createGame(GameSetup.defaults());
        registry.writeState(id, state -> {
            state.resetForNewGame();
            state.configureLobby(true, false);
            state.players().add(new Player(1, "AI1", true, "#111111"));
            state.players().add(new Player(2, "AI2", true, "#222222"));
            state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 10, 2, false));
            state.systems().add(new StarSystem(2, "S2", 1000, 0, 1, 10, 2, false));
            state.systems().add(new StarSystem(3, "S3", 500, 500, 2, 10, 2, false));
            state.start();
            return null;
        });
        return id;
    }

    @Test
    void autoplayReachesGameOver() {
        GameId id = setupAiDominantGame();

        autoplay.autoplayToEnd(id);

        assertThat(registry.readState(id, GameState::gameOver).booleanValue()).isTrue();
        assertThat((int) registry.readState(id, GameState::winnerId)).isOne();
    }

    @Test
    void leaveObserveTriggersAutoplayWhenLastObserverLeaves() {
        GameId id = setupAiDominantGame();
        assertThat(game.observeGame(id, "alice")).isTrue();

        boolean removed = game.leaveObserve(id, "alice");

        assertThat(removed).isTrue();
        assertThat(registry.readState(id, GameState::gameOver).booleanValue()).as("leaveObserve should have run autoplay synchronously (direct call, no proxy)").isTrue();
    }

    @Test
    void leaveObserveWithOtherObserverDoesNotTriggerAutoplay() {
        GameId id = setupAiDominantGame();
        game.observeGame(id, "alice");
        game.observeGame(id, "bob");

        game.leaveObserve(id, "alice");

        assertThat(registry.readState(id, GameState::gameOver).booleanValue()).isFalse();
    }

    @Test
    void leaveObserveRejoiningHumanDoesNotTriggerAutoplay() {
        GameId id = registry.createGame(GameSetup.defaults());
        registry.writeState(id, state -> {
            state.resetForNewGame();
            state.configureLobby(true, false);
            state.players().add(new Player(1, "P1", false, "#111111"));
            state.players().add(new Player(2, "AI1", true, "#222222"));
            state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 10, 2, false));
            state.systems().add(new StarSystem(2, "S2", 1000, 0, 2, 10, 2, false));
            state.originalHumanPlayerIds().add(1);
            state.joinedHumanPlayerIds().add(1);
            state.start();
            return null;
        });
        game.observeGame(id, "alice");
        int turnBefore = registry.readState(id, GameState::turn);

        game.leaveObserve(id, "alice");

        assertThat(registry.readState(id, GameState::gameOver).booleanValue()).isFalse();
        assertThat((int) registry.readState(id, GameState::turn)).isEqualTo(turnBefore);
    }

    @Test
    void autoplayStopsWhenObserverRejoinsMidway() {
        GameId id = registry.createGame(GameSetup.defaults());
        registry.writeState(id, state -> {
            state.resetForNewGame();
            state.configureLobby(true, false);
            state.players().add(new Player(1, "AI1", true, "#111111"));
            state.players().add(new Player(2, "AI2", true, "#222222"));
            state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 5, 1, false));
            state.systems().add(new StarSystem(2, "S2", 1000, 0, null, 5, 1, true));
            state.systems().add(new StarSystem(3, "S3", 500, 500, 2, 5, 1, false));
            state.observers().add("alice");
            state.start();
            return null;
        });

        autoplay.autoplayToEnd(id);

        assertThat(registry.readState(id, GameState::gameOver).booleanValue()).as("autoplay should bail out immediately because an observer is present").isFalse();
    }
}
