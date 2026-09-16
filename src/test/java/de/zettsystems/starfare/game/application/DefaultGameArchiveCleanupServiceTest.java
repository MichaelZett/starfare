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

    @Test
    void previewReturnsOnlyArchivedGamesWithAnOperativeSession() {
        GameArchiveStore archives = mock(GameArchiveStore.class);
        GameSessionStore sessions = mock(GameSessionStore.class);
        GameId present = new GameId("present");
        GameId alreadyRemoved = new GameId("removed");
        when(archives.finishedBefore(any())).thenReturn(List.of(archived(present), archived(alreadyRemoved)));
        when(sessions.load(present)).thenReturn(Optional.of(session(present, true)));
        when(sessions.load(alreadyRemoved)).thenReturn(Optional.empty());

        var service = service(archives, sessions);

        assertThat(service.preview()).containsExactly(present);
    }

    @Test
    void removesOnlySessionsThatAreStillCompleted() {
        GameArchiveStore archives = mock(GameArchiveStore.class);
        GameSessionStore sessions = mock(GameSessionStore.class);
        GameId completed = new GameId("completed");
        GameId noLongerCompleted = new GameId("active");
        when(archives.finishedBefore(any())).thenReturn(List.of(archived(completed), archived(noLongerCompleted)));
        when(sessions.load(completed)).thenReturn(Optional.of(session(completed, true)));
        when(sessions.load(noLongerCompleted)).thenReturn(Optional.of(session(noLongerCompleted, false)));

        int removed = service(archives, sessions).removeExpired();

        assertThat(removed).isOne();
        verify(sessions).delete(completed);
        verify(sessions, never()).delete(noLongerCompleted);
    }

    private static DefaultGameArchiveCleanupService service(GameArchiveStore archives, GameSessionStore sessions) {
        return new DefaultGameArchiveCleanupService(archives, sessions,
                new GameArchiveCleanupProperties(true, Duration.ofDays(90)));
    }

    private static GameArchiveStore.ArchivedGame archived(GameId id) {
        GameState state = new GameState();
        state.endGame(null, FINISHED_AT);
        return new GameArchiveStore.ArchivedGame(id, id.value(), "host", FINISHED_AT, state);
    }

    private static GameSession session(GameId id, boolean completed) {
        GameState state = new GameState();
        if (completed) {
            state.endGame(null, FINISHED_AT);
        }
        return new GameSession(id, id.value(), "host", FINISHED_AT.minusSeconds(60), state);
    }
}
