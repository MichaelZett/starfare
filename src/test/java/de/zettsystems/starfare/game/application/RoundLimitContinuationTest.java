package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RoundLimitContinuationTest extends AbstractIntegrationTest {
    @Autowired GameService games;
    @Autowired GameSessionStore store;

    private GameId finishedAfter(int rounds, int roundLimit) {
        GameState state = new GameState();
        state.resetForNewGame();
        state.rememberSetup(GameSetup.defaults().withSectorOptions(
                new SectorOptions(EventFrequency.OFF, null, false, roundLimit)));
        state.players().add(new Player(1, "P1", false, GameConfig.PLAYER_PALETTE.getFirst()));
        state.players().add(new Player(2, "P2", true, GameConfig.PLAYER_PALETTE.get(1)));
        state.systems().add(new StarSystem(1, "S1", 100, 100, 1, 5, 2, false));
        state.systems().add(new StarSystem(2, "S2", 300, 100, 2, 5, 2, false));
        state.seatByUser().putAll(Map.of("winner", 1));
        state.joinedHumanPlayerIds().addAll(Set.of(1));
        state.originalHumanPlayerIds().addAll(Set.of(1));
        state.start();
        while (state.turn() < rounds) {
            state.nextTurn();
        }
        state.endGame(1);
        GameId id = GameId.newId();
        store.save(new GameSession(id, "Round limit", "winner", Instant.now(), state));
        return id;
    }

    @Test
    void aGameEndedByTheRoundLimitCannotBeContinued() {
        assertThat(games.continueAfterVictory(finishedAfter(SectorOptions.MIN_ROUND_LIMIT + 1, SectorOptions.MIN_ROUND_LIMIT), "winner"))
                .isFalse();
    }

    @Test
    void anEarlierThresholdVictoryCanStillBeContinued() {
        assertThat(games.continueAfterVictory(finishedAfter(3, SectorOptions.MIN_ROUND_LIMIT), "winner")).isTrue();
        assertThat(games.continueAfterVictory(finishedAfter(30, 0), "winner")).isTrue();
    }
}
