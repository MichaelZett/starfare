package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.game.values.RoundRules;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

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
    void hostPlayerIdStoredOnCreation() {
        GameId id = createGameWithHost("alice");

        assertThat(game.hostPlayerIdOf(id)).hasValue("alice");
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
    void canAbortIsFalseForBlankPlayerId() {
        GameId id = createGameWithHost("alice");

        assertThat(game.canAbort(id, null)).isFalse();
        assertThat(game.canAbort(id, "")).isFalse();
    }

    @Test
    void legacyGameWithoutHostAllowsAnyAbort() {
        GameId id = game.newGame(GameSetup.defaults());

        registry.writeState(id, state -> { state.publishInLobby(); return null; });
        assertThat(game.canAbort(id, "anyone")).isTrue();
    }

    @Test
    void leaveByHostTransfersToNextHuman() {
        GameId id = createGameWithHost("alice");
        seedTwoHumanSeats(id, "alice", "bob");

        assertThat(game.leaveGame(id, 1)).isTrue();

        assertThat(game.hostPlayerIdOf(id)).hasValue("bob");
    }

    @Test
    void leaveByNonHostDoesNotChangeHost() {
        GameId id = createGameWithHost("alice");
        seedTwoHumanSeats(id, "alice", "bob");

        assertThat(game.leaveGame(id, 2)).isTrue();

        assertThat(game.hostPlayerIdOf(id)).hasValue("alice");
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

        assertThat(game.hostPlayerIdOf(id)).isEmpty();
    }

    @Test
    void hostCanChangeRunningRoundDeadlinesButOtherPlayersCannot() {
        GameId id = createGameWithHost("alice");
        seedTwoHumanSeats(id, "alice", "bob");
        RoundRules changed = new RoundRules(Duration.ofMinutes(15), Duration.ofMinutes(2), RoundRules.DEFAULT_ATTACK_ORDER);

        assertThat(game.updateRoundRules(id, "bob", changed)).isFalse();
        assertThat(game.updateRoundRules(id, "alice", changed)).isTrue();

        assertThat(game.roundRulesFor(id, "alice")).contains(changed);
        assertThat(game.roundRulesFor(id, "outsider")).isEmpty();
    }

    @Test
    void refusesInvalidOrFinishedRoundRuleChanges() {
        GameId id = createGameWithHost("alice");
        seedTwoHumanSeats(id, "alice", "bob");
        RoundRules rules = RoundRules.defaults();

        assertThat(game.roundRulesFor(id, "")).isEmpty();
        assertThat(game.updateRoundRules(id, "", rules)).isFalse();
        assertThat(game.updateRoundRules(id, "alice", null)).isFalse();
        registry.writeState(id, state -> {
            state.endGame(1);
            return null;
        });

        assertThat(game.updateRoundRules(id, "alice", rules)).isFalse();
    }

    @Test
    void winnerCanContinueTowardsFullConquest() {
        GameId id = createGameWithHost("alice");
        seedTwoHumanSeats(id, "alice", "bob");
        registry.writeState(id, state -> {
            state.endGame(1);
            return null;
        });

        assertThat(game.continueAfterVictory(id, "bob")).isFalse();
        assertThat(game.continueAfterVictory(id, "alice")).isTrue();
        boolean gameOver = registry.readState(id, GameState::gameOver);
        int victorySystemPercent = registry.readState(id, GameState::victorySystemPercent);
        assertThat(gameOver).isFalse();
        assertThat(victorySystemPercent).isEqualTo(100);
    }
}
