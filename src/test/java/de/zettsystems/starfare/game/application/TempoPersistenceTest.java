package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.AttackOrder;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameTempo;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.RoundRules;
import de.zettsystems.starfare.game.values.StarSystem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/** The tempo lives only in the JSON game state, so a new platform tempo needs no migration (API 0.6.0). */
class TempoPersistenceTest extends AbstractIntegrationTest {
    @Autowired GameSessionStore store;
    @Autowired JdbcTemplate jdbc;

    @Test
    void aGameEveryOtherDaySurvivesSavingAndLoadingFromTheDatabase() {
        GameState state = new GameState();
        state.resetForNewGame();
        state.players().add(new Player(1, "P1", false, "#ffffff"));
        state.players().add(new Player(2, "P2", false, "#000000"));
        state.systems().add(new StarSystem(1, "S1", 100, 100, 1, 5, 2, false));
        state.systems().add(new StarSystem(2, "S2", 300, 100, 2, 5, 2, false));
        state.configureRoundRules(RoundRules.forTempo(GameTempo.EVERY_OTHER_DAY, AttackOrder.RANDOM));
        state.start();
        GameId id = GameId.newId();

        store.save(new GameSession(id, "Every other day", null, Instant.now(), state));
        store.invalidate(id);

        String json = jdbc.queryForObject("SELECT state_json FROM game_sessions WHERE id = ?", String.class, id.value());
        assertThat(json).contains("\"EVERY_OTHER_DAY\"");
        GameState loaded = store.load(id).orElseThrow().readState(current -> current);
        assertThat(loaded.roundRules().tempo()).isEqualTo(GameTempo.EVERY_OTHER_DAY);
        assertThat(loaded.roundRules().roundLimit()).isEqualTo(Duration.ofHours(48));
        assertThat(loaded.roundRules().stragglerLimit()).isEqualTo(Duration.ofHours(12));
        store.delete(id);
    }
}
