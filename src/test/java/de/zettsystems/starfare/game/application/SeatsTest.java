package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StarSystem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class SeatsTest extends AbstractIntegrationTest {

    @Autowired
    private GameService game;

    private GameId setupRunningGame(boolean reentryAllowed) {
        GameId id = registry.createGame(GameSetup.defaults());
        registry.writeState(id, state -> {
            state.resetForNewGame();
            state.configureLobby(true, reentryAllowed);
            state.players().add(new Player(1, "P1", false, "#111111"));
            state.players().add(new Player(2, "P2", false, "#222222"));
            state.players().add(new Player(3, "AI1", true, "#333333"));
            state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 10, 2, false));
            state.systems().add(new StarSystem(2, "S2", 1000, 0, 2, 10, 2, false));
            state.systems().add(new StarSystem(3, "S3", 500, 500, 3, 10, 2, false));
            state.originalHumanPlayerIds().add(1);
            state.originalHumanPlayerIds().add(2);
            state.joinedHumanPlayerIds().add(1);
            state.joinedHumanPlayerIds().add(2);
            state.start();
            return null;
        });
        return id;
    }

    private boolean playerIsAi(GameId id, int pid) {
        return registry.readState(id, state ->
                state.players().stream().filter(p -> p.id() == pid).findFirst().orElseThrow().ai());
    }

    private boolean isJoined(GameId id, int pid) {
        return registry.readState(id, state -> state.joinedHumanPlayerIds().contains(pid));
    }

    @Test
    void rejoinAfterLeaveIsAcceptedWhenReentryAllowed() {
        GameId id = setupRunningGame(true);

        assertThat(game.leaveGame(id, 1)).isTrue();
        assertThat(playerIsAi(id, 1)).isTrue();
        assertThat(isJoined(id, 1)).isFalse();

        assertThat(game.joinGame(id, 1)).isTrue();
        assertThat(playerIsAi(id, 1)).isFalse();
        assertThat(isJoined(id, 1)).isTrue();
    }

    @Test
    void rejoinAfterLeaveRejectedWhenReentryDisallowed() {
        GameId id = setupRunningGame(false);

        assertThat(game.leaveGame(id, 1)).isTrue();

        assertThat(game.joinGame(id, 1)).isFalse();
        assertThat(playerIsAi(id, 1)).isTrue();
        assertThat(isJoined(id, 1)).isFalse();
    }

    @Test
    void rejoinByForeignIdRejectedEvenWithReentryAllowed() {
        GameId id = setupRunningGame(true);
        game.leaveGame(id, 1);

        assertThat(game.joinGame(id, 42)).isFalse();
    }

    @Test
    void doubleJoinOfAlreadyJoinedPlayerIsRejected() {
        GameId id = setupRunningGame(true);

        assertThat(game.joinGame(id, 1)).isFalse();
        assertThat((int) registry.readState(id, state -> state.joinedHumanPlayerIds().size())).isEqualTo(2);
    }

    @Test
    void originalAiSeatCannotBeJoined() {
        GameId id = setupRunningGame(true);

        assertThat(game.joinGame(id, 3)).isFalse();
        assertThat(playerIsAi(id, 3)).isTrue();
    }

    @Test
    void rejoinInFinishedGameIsRejected() {
        GameId id = setupRunningGame(true);
        game.leaveGame(id, 1);
        registry.writeState(id, state -> {
            state.endGame(2);
            return null;
        });

        assertThat(game.joinGame(id, 1)).isFalse();
    }

    @Test
    void originalHumansTrackedAfterNewGame() {
        GameSetup setup = new GameSetup(
                10, 2, 1,
                java.util.List.of(4, 4, 4),
                2, 4, 6, "#0072B2", true, true);
        GameId id = registry.createGame(setup);

        assertThat((int) registry.readState(id, state -> state.originalHumanPlayerIds().size())).isEqualTo(2);
    }
}
