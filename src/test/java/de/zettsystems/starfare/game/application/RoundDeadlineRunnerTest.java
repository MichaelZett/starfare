package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.values.GameId;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoundDeadlineRunnerTest {
    private final GameService games = mock(GameService.class);
    private final RoundDeadlineRunner runner = new RoundDeadlineRunner(games);
    private final GameId first = GameId.of("first");
    private final GameId second = GameId.of("second");

    @Test
    void checksEveryLoadedGameAndThenUnloadsIdleOnes() {
        when(games.loadedGames()).thenReturn(List.of(first, second));

        runner.enforceRoundDeadlines();

        InOrder order = inOrder(games);
        order.verify(games).enforceRoundDeadline(eq(first), any(Instant.class));
        order.verify(games).enforceRoundDeadline(eq(second), any(Instant.class));
        order.verify(games).unloadInactiveSingleHumanGames();
    }

    @Test
    void aFailingGameDoesNotStopTheOthers() {
        when(games.loadedGames()).thenReturn(List.of(first, second));
        when(games.enforceRoundDeadline(eq(first), any(Instant.class))).thenThrow(new IllegalArgumentException("deleted"));

        runner.enforceRoundDeadlines();

        InOrder order = inOrder(games);
        order.verify(games).enforceRoundDeadline(eq(second), any(Instant.class));
        order.verify(games).unloadInactiveSingleHumanGames();
    }
}
