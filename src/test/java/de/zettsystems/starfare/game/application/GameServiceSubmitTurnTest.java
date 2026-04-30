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

class GameServiceSubmitTurnTest extends AbstractIntegrationTest {

    @Autowired
    private GameService game;

    private GameId setupTwoHumanStartedGame() {
        return setupTwoHumanGame(true);
    }

    private GameId setupTwoHumanGame(boolean started) {
        GameId id = registry.createGame(GameSetup.defaults());
        registry.writeState(id, state -> {
            state.resetForNewGame();
            state.players().add(new Player(1, "P1", false, "#111111"));
            state.players().add(new Player(2, "P2", false, "#222222"));
            state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 10, 2, false));
            state.systems().add(new StarSystem(2, "S2", 1000, 0, 2, 10, 2, false));
            state.systems().add(new StarSystem(3, "S3", 500, 500, null, 5, 2, true));
            state.joinedHumanPlayerIds().add(1);
            state.joinedHumanPlayerIds().add(2);
            if (started) {
                state.start();
            }
            return null;
        });
        return id;
    }

    private int currentTurn(GameId id) {
        return registry.readState(id, GameState::turn);
    }

    private boolean submitted(GameId id, int pid) {
        return registry.readState(id, state -> state.submittedThisTurn().contains(pid));
    }

    private int submittedSize(GameId id) {
        return registry.readState(id, state -> state.submittedThisTurn().size());
    }

    private boolean submittedEmpty(GameId id) {
        return registry.readState(id, state -> state.submittedThisTurn().isEmpty());
    }

    @Test
    void firstSubmitOfTwoDoesNotAdvanceTurn() {
        GameId id = setupTwoHumanStartedGame();
        int turnBefore = currentTurn(id);

        assertThat(game.submitTurn(id, 1)).isTrue();
        assertThat(currentTurn(id)).isEqualTo(turnBefore);
        assertThat(submitted(id, 1)).isTrue();
        assertThat(game.isWaitingForOtherPlayers(id, 1)).isTrue();
    }

    @Test
    void secondSubmitAdvancesTurnAndClearsSubmissions() {
        GameId id = setupTwoHumanStartedGame();
        int turnBefore = currentTurn(id);

        game.submitTurn(id, 1);
        game.submitTurn(id, 2);

        assertThat(currentTurn(id)).isEqualTo(turnBefore + 1);
        assertThat(submittedEmpty(id)).isTrue();
    }

    @Test
    void submitFromNonParticipantIsRejected() {
        GameId id = setupTwoHumanStartedGame();
        int turnBefore = currentTurn(id);

        assertThat(game.submitTurn(id, 42)).isFalse();
        assertThat(currentTurn(id)).isEqualTo(turnBefore);
        assertThat(submittedEmpty(id)).isTrue();
    }

    @Test
    void repeatedSubmitBySamePlayerIsIdempotent() {
        GameId id = setupTwoHumanStartedGame();
        int turnBefore = currentTurn(id);

        assertThat(game.submitTurn(id, 1)).isTrue();
        assertThat(game.submitTurn(id, 1)).isTrue();
        assertThat(game.submitTurn(id, 1)).isTrue();

        assertThat(currentTurn(id)).isEqualTo(turnBefore);
        assertThat(submittedSize(id)).isOne();
    }

    @Test
    void submitInFinishedGameIsRejected() {
        GameId id = setupTwoHumanStartedGame();
        registry.writeState(id, state -> {
            state.endGame(1);
            return null;
        });
        int turnBefore = currentTurn(id);

        assertThat(game.submitTurn(id, 1)).isFalse();
        assertThat(currentTurn(id)).isEqualTo(turnBefore);
    }

    @Test
    void submitInUnstartedGameIsRejected() {
        GameId id = setupTwoHumanGame(false);

        assertThat(game.submitTurn(id, 1)).isFalse();
    }

    @Test
    void singleHumanSubmitAdvancesImmediately() {
        GameId id = registry.createGame(GameSetup.defaults());
        registry.writeState(id, state -> {
            state.resetForNewGame();
            state.players().add(new Player(1, "P1", false, "#111111"));
            state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 10, 2, false));
            state.systems().add(new StarSystem(2, "S2", 1000, 0, null, 5, 2, true));
            state.joinedHumanPlayerIds().add(1);
            state.start();
            return null;
        });
        int turnBefore = currentTurn(id);

        assertThat(game.submitTurn(id, 1)).isTrue();
        assertThat(currentTurn(id)).isEqualTo(turnBefore + 1);
    }

    @Test
    void isWaitingForOtherPlayersFalseWhenAllSubmitted() {
        GameId id = setupTwoHumanStartedGame();
        game.submitTurn(id, 1);

        assertThat(game.isWaitingForOtherPlayers(id, 1)).isTrue();

        game.submitTurn(id, 2);

        assertThat(game.isWaitingForOtherPlayers(id, 1)).isFalse();
        assertThat(game.isWaitingForOtherPlayers(id, 2)).isFalse();
    }
}
