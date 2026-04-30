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

class AiOnlyTickTest extends AbstractIntegrationTest {

    @Autowired
    private GameService game;

    private GameId setupAiOnlyStartedGame() {
        GameId id = registry.createGame(GameSetup.defaults());
        registry.writeState(id, state -> {
            state.resetForNewGame();
            state.configureLobby(true, false);
            state.players().add(new Player(1, "AI1", true, "#111111"));
            state.players().add(new Player(2, "AI2", true, "#222222"));
            state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 10, 2, false));
            state.systems().add(new StarSystem(2, "S2", 1000, 0, 2, 10, 2, false));
            state.systems().add(new StarSystem(3, "S3", 500, 500, null, 5, 2, true));
            state.start();
            return null;
        });
        return id;
    }

    private int currentTurn(GameId id) {
        return registry.readState(id, GameState::turn);
    }

    @Test
    void observerAdvancesAiOnlyGame() {
        GameId id = setupAiOnlyStartedGame();
        game.observeGame(id, "alice");
        int before = currentTurn(id);

        boolean ok = game.advanceForObserver(id, "alice");

        assertThat(ok).isTrue();
        assertThat(currentTurn(id)).isEqualTo(before + 1);
    }

    @Test
    void nonObserverCannotAdvance() {
        GameId id = setupAiOnlyStartedGame();
        int before = currentTurn(id);

        boolean ok = game.advanceForObserver(id, "not-an-observer");

        assertThat(ok).isFalse();
        assertThat(currentTurn(id)).isEqualTo(before);
    }

    @Test
    void advanceRejectedWhenHumanSeatPresent() {
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
        int before = currentTurn(id);

        boolean ok = game.advanceForObserver(id, "alice");

        assertThat(ok).isFalse();
        assertThat(currentTurn(id)).isEqualTo(before);
    }

    @Test
    void advanceRejectedWhenGameOver() {
        GameId id = setupAiOnlyStartedGame();
        game.observeGame(id, "alice");
        registry.writeState(id, state -> {
            state.endGame(1);
            return null;
        });

        assertThat(game.advanceForObserver(id, "alice")).isFalse();
    }

    @Test
    void advanceRejectedForBlankUsername() {
        GameId id = setupAiOnlyStartedGame();

        assertThat(game.advanceForObserver(id, null)).isFalse();
        assertThat(game.advanceForObserver(id, "  ")).isFalse();
    }

    @Test
    void advanceTicksExactlyOnce() {
        GameId id = setupAiOnlyStartedGame();
        game.observeGame(id, "alice");
        int before = currentTurn(id);

        game.advanceForObserver(id, "alice");
        game.advanceForObserver(id, "alice");

        assertThat(currentTurn(id)).isEqualTo(before + 2);
    }
}
