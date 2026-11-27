package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.game.values.StarSystem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** M7 acceptance: a released Extended game runs through the regular lobby, round and template services. */
class ExtendedReleaseIntegrationTest extends AbstractIntegrationTest {
    private static final String HOST = "extended-host";

    @Autowired private GameService games;
    @Autowired private GameTemplateService templates;

    @Test
    void newExtendedGameStartsWithIndustryAndRangeAndResolvesRounds() {
        GameId id = games.newGame(GameSetup.defaults().selectRuleset(RulesetRef.EXTENDED), HOST, "Extended release");
        int seat = games.joinGame(id, HOST).orElseThrow();
        assertThat(games.startGame(id)).isTrue();

        assertThat(games.summaryOf(id).ruleset()).isEqualTo(RulesetRef.EXTENDED);
        assertExtendedState(id);
        int startTurn = registry.readState(id, GameState::turn);
        for (int round = 1; round <= 3 && !registry.readState(id, GameState::gameOver); round++) {
            assertThat(games.submitTurn(id, seat)).isTrue();
            assertThat(registry.readState(id, GameState::turn).intValue()).isEqualTo(startTurn + round);
        }
        assertExtendedState(id);
        assertThat(games.viewFor(id, seat).systems()).anyMatch(system -> system.industry() != null);

        GameId rematch = templates.create(id, HOST, "Extended rematch", true).orElseThrow();
        assertThat(registry.readState(rematch, GameState::ruleset)).isEqualTo(RulesetRef.EXTENDED);
        assertExtendedState(rematch);
    }

    private void assertExtendedState(GameId id) {
        registry.readState(id, state -> {
            assertThat(state.ruleset()).isEqualTo(RulesetRef.EXTENDED);
            assertThat(state.navigationSettings()).isPresent();
            Set<Integer> systemIds = state.systems().stream().map(StarSystem::id).collect(Collectors.toSet());
            assertThat(state.industries().keySet()).isEqualTo(systemIds);
            return null;
        });
    }
}
