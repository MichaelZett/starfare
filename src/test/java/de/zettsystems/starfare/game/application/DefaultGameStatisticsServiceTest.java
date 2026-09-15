package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameResultEntity;
import de.zettsystems.starfare.game.values.OpponentStatistics;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    private static GameResultEntity result(String id, String winner, Set<String> participants) {
        return new GameResultEntity(id, "Game " + id, winner, Instant.parse("2026-09-15T00:00:00Z"), participants);
    }
}
