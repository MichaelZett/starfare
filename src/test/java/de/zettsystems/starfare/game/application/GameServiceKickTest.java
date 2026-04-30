package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StarSystem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class GameServiceKickTest extends AbstractIntegrationTest {

    @Autowired
    private GameService game;

    private GameId createGameWithHost(String host) {
        return game.newGame(GameSetup.defaults(), host, "Test");
    }

    private void seedTwoHumansStarted(GameId id, String user1, String user2) {
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
            state.originalHumanPlayerIds().add(1);
            state.originalHumanPlayerIds().add(2);
            state.start();
            return null;
        });
    }

    @Test
    void hostKicksHumanBeforeStart() {
        GameId id = createGameWithHost("alice");
        registry.writeState(id, state -> {
            state.resetForNewGame();
            state.players().add(new Player(1, "P1", false, "#111111"));
            state.players().add(new Player(2, "P2", false, "#222222"));
            state.seatByUser().put("alice", 1);
            state.seatByUser().put("bob", 2);
            state.joinedHumanPlayerIds().add(1);
            state.joinedHumanPlayerIds().add(2);
            return null;
        });

        assertThat(game.kickHuman(id, "alice", 2)).isTrue();

        registry.readState(id, state -> {
            Player seat2 = state.players().stream().filter(p -> p.id() == 2).findFirst().orElseThrow();
            assertThat(seat2.ai()).isTrue();
            assertThat(state.joinedHumanPlayerIds()).doesNotContain(2);
            assertThat(state.seatByUser()).doesNotContainKey("bob");
            return null;
        });
    }

    @Test
    void nonHostCannotKick() {
        GameId id = createGameWithHost("alice");
        seedTwoHumansStarted(id, "alice", "bob");

        assertThat(game.kickHuman(id, "bob", 1)).isFalse();
        registry.readState(id, state -> {
            Player seat1 = state.players().stream().filter(p -> p.id() == 1).findFirst().orElseThrow();
            assertThat(seat1.ai()).isFalse();
            return null;
        });
    }

    @Test
    void hostCannotKickSelf() {
        GameId id = createGameWithHost("alice");
        seedTwoHumansStarted(id, "alice", "bob");

        assertThat(game.kickHuman(id, "alice", 1)).isFalse();
    }

    @Test
    void kickHumanDuringRunningGameSeatBecomesAi() {
        GameId id = createGameWithHost("alice");
        seedTwoHumansStarted(id, "alice", "bob");

        assertThat(game.kickHuman(id, "alice", 2)).isTrue();

        registry.readState(id, state -> {
            Player seat2 = state.players().stream().filter(p -> p.id() == 2).findFirst().orElseThrow();
            assertThat(seat2.ai()).isTrue();
            assertThat(state.joinedHumanPlayerIds()).doesNotContain(2);
            return null;
        });
    }

    @Test
    void cannotKickAiSeat() {
        GameId id = createGameWithHost("alice");
        registry.writeState(id, state -> {
            state.resetForNewGame();
            state.players().add(new Player(1, "P1", false, "#111111"));
            state.players().add(new Player(2, "AI1", true, "#222222"));
            state.seatByUser().put("alice", 1);
            state.joinedHumanPlayerIds().add(1);
            return null;
        });

        assertThat(game.kickHuman(id, "alice", 2)).isFalse();
    }

    @Test
    void kickUnknownSeatRejected() {
        GameId id = createGameWithHost("alice");
        seedTwoHumansStarted(id, "alice", "bob");

        assertThat(game.kickHuman(id, "alice", 99)).isFalse();
    }

    @Test
    void kickNonJoinedHumanBeforeStartStillSucceeds() {
        GameId id = createGameWithHost("alice");
        registry.writeState(id, state -> {
            state.resetForNewGame();
            state.players().add(new Player(1, "P1", false, "#111111"));
            state.players().add(new Player(2, "P2", false, "#222222"));
            state.seatByUser().put("alice", 1);
            state.joinedHumanPlayerIds().add(1);
            return null;
        });

        assertThat(game.kickHuman(id, "alice", 2)).isTrue();
        registry.readState(id, state -> {
            assertThat(state.players().stream().filter(p -> p.id() == 2).findFirst().orElseThrow().ai()).isTrue();
            return null;
        });
    }
}
