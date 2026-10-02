package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.domain.GameResultEntity;
import de.zettsystems.starfare.game.values.RulesetRef;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class RulesetStatisticsTest extends AbstractIntegrationTest {
    private static final String ACCOUNT = "ruleset-stats-user";
    @Autowired private GameResultRepository results;
    @Autowired private GameStatisticsService statistics;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void legacySqlDefaultsAndVariantFilterKeepTotalsAndOpponentsConsistent() {
        jdbc.update("insert into game_results (game_id, game_name, finished_at) values (?, ?, ?)",
                "ruleset-legacy", "Legacy", java.sql.Timestamp.from(Instant.now()));
        assertThat(results.findById("ruleset-legacy").orElseThrow().getRuleset()).isEqualTo(RulesetRef.SECTOR_FORCES);
        results.save(new GameResultEntity("ruleset-stats-classic", "Classic", ACCOUNT, Instant.now(),
                Set.of(ACCOUNT, "classic-opponent"), Set.of("Classic AI")));
        var future = new GameResultEntity("ruleset-stats-third", "Other", "other-opponent", Instant.now(),
                Set.of(ACCOUNT, "other-opponent"), Set.of("Other AI"));
        future.recordRuleset(new RulesetRef("test-third", "4.0.0"));
        results.saveAndFlush(future);

        var all = statistics.statisticsFor(ACCOUNT);
        assertThat(all.games()).isEqualTo(2);
        assertThat(all.wins()).isEqualTo(1);
        assertThat(all.losses()).isEqualTo(1);
        var classic = statistics.statisticsFor(ACCOUNT, RulesetRef.SECTOR_FORCES.variant());
        assertThat(classic.games()).isEqualTo(1);
        assertThat(classic.wins()).isEqualTo(1);
        assertThat(classic.losses()).isZero();
        assertThat(classic.opponents()).extracting(row -> row.opponentId()).containsExactly("classic-opponent");
        assertThat(classic.completedGames()).singleElement().satisfies(game -> {
            assertThat(game.ruleset()).isEqualTo(RulesetRef.SECTOR_FORCES);
            assertThat(game.aiOpponentNames()).containsExactly("Classic AI");
        });
        var other = statistics.statisticsFor(ACCOUNT, "test-third");
        assertThat(other.games()).isEqualTo(1);
        assertThat(other.losses()).isEqualTo(1);
        assertThat(other.completedGames()).singleElement().satisfies(game ->
                assertThat(game.ruleset()).isEqualTo(new RulesetRef("test-third", "4.0.0")));
        assertThat(statistics.statisticsFor(ACCOUNT, "unknown").games()).isZero();
    }
}
