package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StarSystem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class GameServiceHostTest extends AbstractIntegrationTest {

    @Autowired
    private GameService game;

    private GameId createGameWithHost(String host) {
        return game.newGame(GameSetup.defaults(), host, "Test");
    }

    private void seedTwoHumanSeats(GameId id, String user1, String user2) {
        registry.writeState(id, state -> {
            state.resetForNewGame();
            state.players().add(new Player(1, "P1", false, "#111111"));
            state.players().add(new Player(2, "P2", false, "#222222"));
            state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 10, 2, false));
            state.systems().add(new StarSystem(2, "S2", 1000, 0, 2, 10, 2, false));
            state.systems().add(new StarSystem(3, "S3", 500, 500, null, 5, 2, true));
            state.seatByUser().put(user1, 1);
            state.seatByUser().put(user2, 2);
            state.joinedHumanPlayerIds().add(1);
            state.joinedHumanPlayerIds().add(2);
            state.start();
            return null;
        });
    }

    @Test
    void hostUsernameStoredOnCreation() {
        GameId id = createGameWithHost("alice");

        assertThat(game.hostUsernameOf(id)).hasValue("alice");
    }

    @Test
    void abortByHostSucceeds() {
        GameId id = createGameWithHost("alice");

        assertThat(game.abortGame(id, "alice")).isTrue();
        assertThat(game.hasActiveGame(id)).isFalse();
    }

    @Test
    void abortByNonHostIsRejected() {
        GameId id = createGameWithHost("alice");

        assertThat(game.abortGame(id, "bob")).isFalse();
        assertThat(game.hasActiveGame(id)).isTrue();
    }

    @Test
    void canAbortIsFalseForBlankUsername() {
        GameId id = createGameWithHost("alice");

        assertThat(game.canAbort(id, null)).isFalse();
        assertThat(game.canAbort(id, "")).isFalse();
    }

    @Test
    void legacyGameWithoutHostAllowsAnyAbort() {
        GameId id = game.newGame(GameSetup.defaults());

        assertThat(game.canAbort(id, "anyone")).isTrue();
    }

    @Test
    void leaveByHostTransfersToNextHuman() {
        GameId id = createGameWithHost("alice");
        seedTwoHumanSeats(id, "alice", "bob");

        assertThat(game.leaveGame(id, 1)).isTrue();

        assertThat(game.hostUsernameOf(id)).hasValue("bob");
    }

    @Test
    void leaveByNonHostDoesNotChangeHost() {
        GameId id = createGameWithHost("alice");
        seedTwoHumanSeats(id, "alice", "bob");

        assertThat(game.leaveGame(id, 2)).isTrue();

        assertThat(game.hostUsernameOf(id)).hasValue("alice");
    }

    @Test
    void leaveByLastHumanClearsHost() {
        GameId id = createGameWithHost("alice");
        registry.writeState(id, state -> {
            state.resetForNewGame();
            state.players().add(new Player(1, "P1", false, "#111111"));
            state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 10, 2, false));
            state.systems().add(new StarSystem(2, "S2", 1000, 0, null, 5, 2, true));
            state.seatByUser().put("alice", 1);
            state.joinedHumanPlayerIds().add(1);
            state.start();
            return null;
        });

        assertThat(game.leaveGame(id, 1)).isTrue();

        assertThat(game.hostUsernameOf(id)).isEmpty();
    }
}
