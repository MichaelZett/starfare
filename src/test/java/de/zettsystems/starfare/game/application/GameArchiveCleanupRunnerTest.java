package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.config.GameArchiveCleanupProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class GameArchiveCleanupRunnerTest {

    @Test
    void invokesCleanupWhenEnabled() {
        GameArchiveCleanupService cleanup = mock(GameArchiveCleanupService.class);

        new GameArchiveCleanupRunner(cleanup,
                new GameArchiveCleanupProperties(true, Duration.ofDays(90))).removeExpiredGames();

        verify(cleanup).removeExpired();
    }

    @Test
    void skipsCleanupWhenDisabled() {
        GameArchiveCleanupService cleanup = mock(GameArchiveCleanupService.class);

        new GameArchiveCleanupRunner(cleanup,
                new GameArchiveCleanupProperties(false, Duration.ofDays(90))).removeExpiredGames();

        verify(cleanup, never()).removeExpired();
    }
}
