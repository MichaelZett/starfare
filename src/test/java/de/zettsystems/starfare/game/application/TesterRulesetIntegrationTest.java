package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameListScope;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.GameSummary;
import de.zettsystems.starfare.game.values.GameVisibility;
import de.zettsystems.starfare.game.values.RulesetCatalog;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Starfare im Plattform-Host: Die Dienste setzen die Testvariante Spaceward selbst durch, nicht nur
 * die Oberfläche. Die Rollenregel prüft {@link DefaultRulesetAccessTest}; hier gilt jedes Konto,
 * das mit {@code tester} beginnt, als Tester.
 */
class TesterRulesetIntegrationTest extends AbstractIntegrationTest {
    private static final String TESTER = "tester";
    private static final String OTHER_TESTER = "tester-2";
    private static final String PLAYER = "player";
    private static final GameSetup CLASSIC = twoHumans();
    private static final GameSetup SPACEWARD = twoHumans().selectRuleset(RulesetCatalog.ECONOMY_CANDIDATE);

    @TestConfiguration
    static class HostAccess {
        @Bean
        @Primary
        RulesetAccess hostRulesetAccess(RulesetCatalog catalog) {
            return (ruleset, account) -> !catalog.testing(ruleset) || (account != null && account.startsWith(TESTER));
        }
    }

    @Autowired private GameService games;
    @Autowired private GameTemplateService templates;

    @Test
    void onlyTestersCreateSpacewardGames() {
        int before = games.listGames().size();
        assertThatThrownBy(() -> games.newGame(SPACEWARD, PLAYER, "Refused"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> games.newGame(SPACEWARD)).as("no host, no tester")
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(games.listGames()).hasSize(before);
        games.newGame(SPACEWARD, TESTER, "Tested");
        games.newGame(CLASSIC, PLAYER, "Classic");
        assertThat(games.listGames()).hasSize(before + 2);
    }

    @Test
    void foreignOpenSpacewardGamesStayHiddenAndClosedForNonTesters() {
        GameId spaceward = publicGame(SPACEWARD, "Open Spaceward");
        GameId classic = publicGame(CLASSIC, "Open SectorForces");
        assertThat(lobbyOf(PLAYER)).containsExactly(classic);
        assertThat(lobbyOf(OTHER_TESTER)).containsExactlyInAnyOrder(spaceward, classic);
        assertThat(games.joinGame(spaceward, PLAYER)).isEmpty();
        assertThat(games.joinGame(spaceward, PLAYER, "Player", "Empire")).isEmpty();
        assertThat(games.observeGame(spaceward, PLAYER)).isFalse();
        assertThat(games.inviteUser(spaceward, PLAYER)).isEmpty();
        assertThat(games.summaryOf(spaceward).seatByPlayer()).doesNotContainKey(PLAYER);
        assertThat(games.joinGame(classic, PLAYER)).isPresent();
        assertThat(games.observeGame(spaceward, OTHER_TESTER)).isTrue();
        assertThat(games.inviteUser(spaceward, OTHER_TESTER)).isPresent();
        assertThat(games.joinGame(spaceward, OTHER_TESTER)).isPresent();
    }

    @Test
    void existingParticipationInASpacewardGameKeepsWorking() {
        GameId id = games.newGame(SPACEWARD, TESTER, "Before the restriction");
        assertThat(games.joinGame(id, TESTER)).isPresent();
        int seat = registry.writeState(id, state -> {
            int free = state.players().stream().filter(player -> !player.ai()
                    && !state.joinedHumanPlayerIds().contains(player.id())).findFirst().orElseThrow().id();
            state.seatByUser().put(PLAYER, free);
            return free;
        });
        assertThat(lobbyOf(PLAYER)).containsExactly(id);
        assertThat(games.joinGame(id, PLAYER)).hasValue(seat);
        assertThat(games.startGame(id)).isTrue();
        assertThat(games.viewForAccount(id, PLAYER)).isPresent();
        assertThat(lobbyOf(PLAYER)).containsExactly(id);
    }

    @Test
    void templatesAndRematchesOfSpacewardGamesAreForTestersOnly() {
        GameId source = games.newGame(SPACEWARD, TESTER, "Finished");
        registry.writeState(source, state -> {
            state.seatByUser().put(TESTER, 1);
            state.seatByUser().put(PLAYER, 2);
            state.endGame(1);
            return null;
        });
        assertThat(templates.template(source, PLAYER)).isEmpty();
        assertThat(templates.create(source, PLAYER, "Copy", false)).isEmpty();
        GameId rematch = templates.create(source, TESTER, "Again", true).orElseThrow();
        Map<String, Integer> reserved = registry.readState(rematch, state -> Map.copyOf(state.invitedSeats()));
        assertThat(reserved).containsOnlyKeys(TESTER);
    }

    private GameId publicGame(GameSetup setup, String name) {
        GameId id = games.newGame(setup, TESTER, name);
        assertThat(games.joinGame(id, TESTER)).isPresent();
        registry.writeState(id, state -> { state.configureLobby(true, true); return null; });
        assertThat(games.changeVisibility(id, TESTER, GameVisibility.PUBLIC)).isTrue();
        return id;
    }

    private List<GameId> lobbyOf(String account) {
        return games.visibleGamesFor(account, GameListScope.LOBBY).stream().map(GameSummary::gameId).toList();
    }

    private static GameSetup twoHumans() {
        GameSetup defaults = GameSetup.defaults();
        return new GameSetup(12, 2, 0, List.of(4, 7), 2, 5, 15, true, true,
                defaults.seatColorHexes(), defaults.productionDistribution(), defaults.galaxyLayout(), false,
                20, defaults.roundRules(), 90).normalized();
    }
}
