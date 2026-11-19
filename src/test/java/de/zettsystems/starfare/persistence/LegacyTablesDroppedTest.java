package de.zettsystems.starfare.persistence;

import de.zettsystems.starfare.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/** V2_14: results, statistics and the social graph live on the platform; Starfare keeps only the end of a game. */
class LegacyTablesDroppedTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void legacySocialAndResultTablesAreGone() {
        assertThat(jdbc.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = current_schema() AND table_name IN ('game_result_participants',
                    'game_result_winners', 'game_result_ai_opponents', 'friendships', 'direct_messages',
                    'user_preferences')
                """, String.class)).isEmpty();
    }

    @Test
    void gameResultsKeepOnlyTheEndOfAGame() {
        assertThat(jdbc.queryForList("""
                SELECT column_name FROM information_schema.columns
                WHERE table_schema = current_schema() AND table_name = 'game_results'
                """, String.class)).containsExactlyInAnyOrder("game_id", "finished_at", "version");
    }
}
