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

class GameServiceLeaveGameTest extends AbstractIntegrationTest {

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

    private GameId setupSingleHumanStartedGame() {
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
        return id;
    }

    private int currentTurn(GameId id) {
        return registry.readState(id, GameState::turn);
    }

    private boolean playerIsAi(GameId id, int pid) {
        return registry.readState(id, state ->
                state.players().stream().filter(p -> p.id() == pid).findFirst().orElseThrow().ai());
    }

    private boolean isJoined(GameId id, int pid) {
        return registry.readState(id, state -> state.joinedHumanPlayerIds().contains(pid));
    }

    private boolean submitted(GameId id, int pid) {
        return registry.readState(id, state -> state.submittedThisTurn().contains(pid));
    }

    @Test
    void leaveFlipsPlayerToAi() {
        GameId id = setupTwoHumanStartedGame();

        assertThat(game.leaveGame(id, 1)).isTrue();
        assertThat(playerIsAi(id, 1)).isTrue();
        assertThat(playerIsAi(id, 2)).isFalse();
    }

    @Test
    void leaveRemovesFromJoinedHumanPlayerIds() {
        GameId id = setupTwoHumanStartedGame();

        game.leaveGame(id, 1);

        assertThat(isJoined(id, 1)).isFalse();
        assertThat(isJoined(id, 2)).isTrue();
    }

    @Test
    void leaveAdvancesWhenRemainingHumanAlreadySubmitted() {
        GameId id = setupTwoHumanStartedGame();
        int turnBefore = currentTurn(id);

        game.submitTurn(id, 2);
        assertThat(currentTurn(id)).isEqualTo(turnBefore);

        game.leaveGame(id, 1);

        assertThat(currentTurn(id)).isEqualTo(turnBefore + 1);
    }

    @Test
    void leaveBySoleHumanTriggersAutoplay() {
        GameId id = setupSingleHumanStartedGame();
        int turnBefore = currentTurn(id);

        assertThat(game.leaveGame(id, 1)).isTrue();
        assertThat(playerIsAi(id, 1)).isTrue();
        assertThat(isJoined(id, 1)).isFalse();
        assertThat(currentTurn(id) > turnBefore).as("leaveGame by sole human with no observers must trigger autoplay (turn advances)").isTrue();
    }

    @Test
    void leaveByNonParticipantIsRejected() {
        GameId id = setupTwoHumanStartedGame();

        assertThat(game.leaveGame(id, 42)).isFalse();
        assertThat(playerIsAi(id, 1)).isFalse();
        assertThat(playerIsAi(id, 2)).isFalse();
    }

    @Test
    void leaveInFinishedGameIsRejected() {
        GameId id = setupTwoHumanStartedGame();
        registry.writeState(id, state -> {
            state.endGame(1);
            return null;
        });

        assertThat(game.leaveGame(id, 1)).isFalse();
        assertThat(playerIsAi(id, 1)).isFalse();
        assertThat(isJoined(id, 1)).isTrue();
    }

    @Test
    void leaveInUnstartedGameIsRejected() {
        GameId id = setupTwoHumanGame(false);

        assertThat(game.leaveGame(id, 1)).isFalse();
    }

    @Test
    void leaveClearsPreviousSubmissionOfLeavingPlayer() {
        GameId id = setupTwoHumanStartedGame();

        game.submitTurn(id, 1);
        assertThat(submitted(id, 1)).isTrue();

        game.leaveGame(id, 1);

        assertThat(submitted(id, 1)).isFalse();
    }

    @Test
    void leaveClearsPendingOrdersOfLeavingPlayer() {
        GameId id = setupTwoHumanStartedGame();
        game.sendFleet(id, 1, 1, 2, 3);
        assertThat(registry.<Boolean>readState(id,
                state -> state.pendingOrders().containsKey(1) && !state.pendingOrders().get(1).isEmpty())).isTrue();

        game.leaveGame(id, 1);

        assertThat(registry.<Boolean>readState(id,
                state -> !state.pendingOrders().containsKey(1) || state.pendingOrders().get(1).isEmpty())).isTrue();
    }

    @Test
    void submissionsClearedAfterLeaveTriggersAdvance() {
        GameId id = setupTwoHumanStartedGame();

        game.submitTurn(id, 2);
        game.leaveGame(id, 1);

        assertThat(submitted(id, 1)).isFalse();
        assertThat(submitted(id, 2)).isFalse();
    }
}
