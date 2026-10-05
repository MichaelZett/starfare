package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.domain.GameResultEntity;
import jakarta.persistence.EntityManager;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.session_factory.statement_inspector="
        + "de.zettsystems.starfare.game.application.GameStatisticsFetchingTest$SqlCapture")
@Transactional
class GameStatisticsFetchingTest extends AbstractIntegrationTest {
    @Autowired private GameResultRepository repository;
    @Autowired private GameStatisticsService statistics;
    @Autowired private EntityManager entityManager;

    @Test
    void loadsPopulatedStatisticsWithThreeQueriesAndNoCollectionProduct() {
        for (int i = 0; i < 4; i++) {
            var game = new GameResultEntity("fetch-" + i, "Game " + i, "fetch-me", Instant.now(),
                    Set.of("fetch-me", "fetch-other", "fetch-third"), Set.of("AI one", "AI two"));
            game.recordWinners(Set.of("fetch-me", "fetch-other"));
            repository.save(game);
        }
        entityManager.flush();
        entityManager.clear();
        SqlCapture.SQL.get().clear();
        try {
            var result = statistics.statisticsFor("fetch-me");
            assertThat(result.games()).isEqualTo(4);
            assertThat(result.completedGames()).allSatisfy(game -> {
                assertThat(game.opponentIds()).containsExactly("fetch-other", "fetch-third");
                assertThat(game.aiOpponentNames()).containsExactly("AI one", "AI two");
            });
            var selects = SqlCapture.SQL.get().stream().filter(sql -> sql.startsWith("select")).toList();
            assertThat(selects).hasSize(3).noneSatisfy(sql -> {
                assertThat(sql).contains("game_result_participants");
                assertThat(sql).contains("game_result_ai_opponents");
            });
            assertThat(selects).allSatisfy(sql -> assertThat(java.util.stream.Stream.of(
                    "game_result_participants", "game_result_ai_opponents", "game_result_winners")
                    .filter(sql::contains).count()).isLessThanOrEqualTo(1));
        } finally {
            SqlCapture.SQL.remove();
        }
    }

    public static class SqlCapture implements StatementInspector {
        private static final long serialVersionUID = 1L;
        static final ThreadLocal<List<String>> SQL = ThreadLocal.withInitial(ArrayList::new);

        @Override
        public String inspect(String sql) {
            SQL.get().add(sql.toLowerCase(Locale.ROOT));
            return sql;
        }
    }
}
