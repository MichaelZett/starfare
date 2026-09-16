package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameResultEntity;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.values.OpponentStatistics;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;

class DefaultGameStatisticsServiceTest {

    @Test
    void aggregatesWinsLossesAndOpponentRecords() {
        GameResultRepository results = mock(GameResultRepository.class);
        DefaultGameStatisticsService service = new DefaultGameStatisticsService(mock(GameRegistry.class), results);
        when(results.findForParticipant("me")).thenReturn(List.of(
                result("one", "me", Set.of("me", "alice")),
                result("two", "bob", Set.of("me", "bob")),
                result("three", "me", Set.of("me", "alice", "bob"))));

        var statistics = service.statisticsFor("me");

        assertThat(statistics.games()).isEqualTo(3);
        assertThat(statistics.wins()).isEqualTo(2);
        assertThat(statistics.losses()).isOne();
        assertThat(statistics.opponents()).containsExactly(
                new OpponentStatistics("alice", 2, 2, 0),
                new OpponentStatistics("bob", 2, 1, 1));
    }

    @Test
    void blankAccountHasNoStatistics() {
        DefaultGameStatisticsService service = new DefaultGameStatisticsService(mock(GameRegistry.class),
                mock(GameResultRepository.class));

        assertThat(service.statisticsFor(" ")).isEqualTo(de.zettsystems.starfare.game.values.PlayerStatistics.empty());
    }

    @Test
    void recordsFinishedGameOnceWithAccountsAndWinner() {
        GameRegistry registry = mock(GameRegistry.class);
        GameResultRepository results = mock(GameResultRepository.class);
        GameSession session = mock(GameSession.class);
        de.zettsystems.starfare.game.domain.GameState state = mock(de.zettsystems.starfare.game.domain.GameState.class);
        de.zettsystems.starfare.game.values.GameId id = new de.zettsystems.starfare.game.values.GameId("finished");
        when(registry.find(id)).thenReturn(Optional.of(session));
        when(session.name()).thenReturn("Finished game");
        when(session.readState(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Function<de.zettsystems.starfare.game.domain.GameState, Object> reader = invocation.getArgument(0);
            return reader.apply(state);
        });
        when(state.gameOver()).thenReturn(true);
        when(state.seatByUser()).thenReturn(java.util.Map.of("alice", 1, "bob", 2));
        when(state.winnerId()).thenReturn(2);
        Instant finishedAt = Instant.parse("2026-09-16T10:00:00Z");
        when(state.finishedAt()).thenReturn(finishedAt);

        new DefaultGameStatisticsService(registry, results).recordFinishedGame(id);

        org.mockito.ArgumentCaptor<GameResultEntity> saved = org.mockito.ArgumentCaptor.forClass(GameResultEntity.class);
        verify(results).save(saved.capture());
        assertThat(saved.getValue().getId()).isEqualTo(id.value());
        assertThat(saved.getValue().getWinnerPlayerId()).isEqualTo("bob");
        assertThat(saved.getValue().getParticipants()).containsExactlyInAnyOrder("alice", "bob");
    }

    @Test
    void doesNotRecordExistingMissingActiveOrParticipantlessGame() {
        GameRegistry registry = mock(GameRegistry.class);
        GameResultRepository results = mock(GameResultRepository.class);
        DefaultGameStatisticsService service = new DefaultGameStatisticsService(registry, results);
        de.zettsystems.starfare.game.values.GameId existing = new de.zettsystems.starfare.game.values.GameId("existing");
        when(results.existsById(existing.value())).thenReturn(true);
        service.recordFinishedGame(existing);

        de.zettsystems.starfare.game.values.GameId missing = new de.zettsystems.starfare.game.values.GameId("missing");
        when(registry.find(missing)).thenReturn(Optional.empty());
        service.recordFinishedGame(missing);

        GameSession activeSession = mock(GameSession.class);
        de.zettsystems.starfare.game.values.GameId active = new de.zettsystems.starfare.game.values.GameId("active");
        when(registry.find(active)).thenReturn(Optional.of(activeSession));
        when(activeSession.readState(any())).thenReturn(null);
        service.recordFinishedGame(active);

        verify(results, never()).save(any());
    }

    private static GameResultEntity result(String id, String winner, Set<String> participants) {
        return new GameResultEntity(id, "Game " + id, winner, Instant.parse("2026-09-15T00:00:00Z"), participants);
    }
}
