package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.config.GameArchiveCleanupProperties;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultGameArchiveCleanupServiceTest {

    private static final Instant FINISHED_AT = Instant.parse("2026-01-01T00:00:00Z");

    private final GameArchiveStore archives = mock(GameArchiveStore.class);
    private final GameSessionStore sessions = mock(GameSessionStore.class);
    private final BattleAcknowledgementService acknowledgements = mock(BattleAcknowledgementService.class);

    @Test
    void previewsFinishedGamesAndArchiveEntriesWithoutAStoredGame() {
        GameId finished = new GameId("finished");
        GameId archiveOnly = new GameId("archive-only");
        GameId continued = new GameId("continued");
        when(archives.idsFinishedBefore(any())).thenReturn(List.of(finished, archiveOnly, continued));
        when(sessions.load(finished)).thenReturn(Optional.of(session(finished, true)));
        when(sessions.load(archiveOnly)).thenReturn(Optional.empty());
        when(sessions.load(continued)).thenReturn(Optional.of(session(continued, false)));

        assertThat(service().preview()).containsExactly(finished, archiveOnly);
    }

    @Test
    void removesTheStoredGameItsArchiveEntryAndItsAcknowledgementsTogether() {
        GameId finished = new GameId("finished");
        GameId archiveOnly = new GameId("archive-only");
        GameId continued = new GameId("continued");
        when(archives.idsFinishedBefore(any())).thenReturn(List.of(finished, archiveOnly, continued));
        when(sessions.load(finished)).thenReturn(Optional.of(session(finished, true)));
        when(sessions.load(archiveOnly)).thenReturn(Optional.empty());
        when(sessions.load(continued)).thenReturn(Optional.of(session(continued, false)));

        int removed = service().removeExpired();

        assertThat(removed).isEqualTo(2);
        for (GameId id : List.of(finished, archiveOnly)) {
            verify(sessions).delete(id);
            verify(archives).delete(id);
            verify(acknowledgements).forgetGame(id);
        }
        verify(sessions, never()).delete(continued);
        verify(archives, never()).delete(continued);
    }

    @Test
    void looksBackByTheRetention() {
        when(archives.idsFinishedBefore(any())).thenReturn(List.of());
        Instant before = Instant.now().minus(Duration.ofDays(90));

        service().preview();

        verify(archives).idsFinishedBefore(org.mockito.ArgumentMatchers.argThat(cutoff ->
                !cutoff.isBefore(before) && !cutoff.isAfter(Instant.now().minus(Duration.ofDays(90)))));
    }

    private DefaultGameArchiveCleanupService service() {
        return new DefaultGameArchiveCleanupService(archives, sessions, acknowledgements,
                new GameArchiveCleanupProperties(true, Duration.ofDays(90)));
    }

    private static GameSession session(GameId id, boolean completed) {
        GameState state = new GameState();
        if (completed) {
            state.endGame(null, FINISHED_AT);
        }
        return new GameSession(id, id.value(), "host", FINISHED_AT.minusSeconds(60), state);
    }
}
