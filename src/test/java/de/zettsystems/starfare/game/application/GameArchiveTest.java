package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameListScope;
import de.zettsystems.starfare.game.values.GameSummary;
import de.zettsystems.starfare.game.values.GameSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class GameArchiveTest extends AbstractIntegrationTest {
    @Autowired private GameService games;
    @Autowired private GameSessionRepository repository;

    @Test
    void reviewSelectionRequiresFinishedAccessibleGameAndExistingParticipant() {
        var id = games.newGame(GameSetup.defaults(), "host", "Review");
        games.joinGame(id, "host");
        assertThat(games.reviewFor(id, "host", 1, false)).isEmpty();
        games.startGame(id);
        assertThat(games.reviewFor(id, "host", 2, true)).isEmpty();
        registry.writeState(id, state -> { state.endGame(1); return null; });
        var snapshot = registry.readState(id, GameState::toSnapshot);
        long version = repository.findById(id.value()).orElseThrow().getVersion();
        assertThat(games.reviewFor(id, "stranger", 1, false)).isEmpty();
        assertThat(games.reviewFor(id, "", 1, true)).isEmpty();
        assertThat(games.reviewFor(id, "host", -1, false)).isEmpty();
        assertThat(games.reviewFor(id, "host", 999, true)).isEmpty();
        assertThat(games.reviewFor(id, "host", 1, true)).isPresent();
        assertThat(games.reviewFor(id, "host", 2, true)).isPresent();
        assertThat(games.reviewFor(id, "host", 2, false)).isPresent();
        assertThat(registry.readState(id, GameState::toSnapshot)).isEqualTo(snapshot);
        assertThat(repository.findById(id.value()).orElseThrow().getVersion()).isEqualTo(version);
    }

    @Test
    void finishedGameMovesToArchiveAndReviewDoesNotWrite() {
        var id = games.newGame(GameSetup.defaults(), "host", "Finished");
        assertThat(games.joinGame(id, "host")).isPresent();
        assertThat(games.startGame(id)).isTrue();
        registry.writeState(id, state -> { state.endGame(1); return null; });
        var before = registry.readState(id, GameState::toSnapshot);
        long version = repository.findById(id.value()).orElseThrow().getVersion();
        assertThat(games.visibleGamesFor("host", GameListScope.LOBBY)).isEmpty();
        assertThat(games.visibleGamesFor("host", GameListScope.ARCHIVE))
                .extracting(GameSummary::gameId)
                .contains(id);
        assertThat(games.reviewFor(id, "host")).isPresent();
        assertThat(games.reviewFor(id, "stranger")).isEmpty();
        assertThat(registry.readState(id, GameState::toSnapshot)).isEqualTo(before);
        assertThat(repository.findById(id.value()).orElseThrow().getVersion()).isEqualTo(version);
    }

    @Test
    void staleCommandsCannotChangeFinishedGame() {
        var id = games.newGame(GameSetup.defaults(), "host", "Finished");
        games.joinGame(id, "host");
        games.startGame(id);
        registry.writeState(id, state -> { state.endGame(1); return null; });
        var before = registry.readState(id, GameState::toSnapshot);
        assertThat(games.sendFleet(id, 1, 1, 2, 1)).isFalse();
        assertThat(games.addStandingOrder(id, 1, 1, 2, 1)).isFalse();
        assertThat(games.removeStandingOrder(id, 1, 1)).isFalse();
        assertThat(games.removeStandingOrderFrom(id, 1, 1)).isFalse();
        assertThat(games.cancelOrder(id, 1, 0)).isFalse();
        assertThat(games.setFleetWait(id, 1, 1)).isFalse();
        assertThat(games.disbandFleet(id, 1, 1)).isFalse();
        assertThat(games.submitTurn(id, 1)).isFalse();
        assertThat(games.abortGame(id, "host")).isFalse();
        assertThat(games.leaveObserve(id, "host")).isFalse();
        assertThat(games.revokeInvite(id, "guest")).isEmpty();
        assertThat(games.startGame(id)).isFalse();
        assertThat(registry.readState(id, GameState::toSnapshot)).isEqualTo(before);
    }
}
