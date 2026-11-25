package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameListScope;
import de.zettsystems.starfare.game.values.GalaxyLayout;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.GameSummary;
import de.zettsystems.starfare.game.values.GameVisibility;
import de.zettsystems.starfare.game.values.RulesetCatalog;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Starfare im Plattform-Host: Die Dienste setzen die Stufe einer Variante selbst durch, nicht nur
 * die Oberfläche. Die Frage an die Plattform prüft {@link DefaultRulesetAccessTest}; hier darf jedes
 * Konto, das mit {@code tester} beginnt, die Alpha-Variante Spaceward nutzen.
 */
class TesterRulesetIntegrationTest extends AbstractIntegrationTest {
    private static final String TESTER = "tester";
    private static final String OTHER_TESTER = "tester-2";
    private static final String PLAYER = "player";
    private static final GameSetup CLASSIC = twoHumans();
    private static final GameSetup SPACEWARD = twoHumans().selectRuleset(RulesetCatalog.ECONOMY_CANDIDATE);

    /** Accounts whose Alpha role the platform took away during the test. */
    private static final Set<String> REVOKED = ConcurrentHashMap.newKeySet();

    @TestConfiguration
    static class HostAccess {
        @Bean
        @Primary
        RulesetAccess hostRulesetAccess() {
            return (ruleset, account) -> !ruleset.isSpaceward()
                    || (account != null && account.startsWith(TESTER) && !REVOKED.contains(account));
        }
    }

    @AfterEach
    void restoreRoles() {
        REVOKED.clear();
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
    void anAccountThatLosesTheVariantLosesItsRunningGameToo() {
        GameId id = publicGame(SPACEWARD, "Running Spaceward");
        int own = games.seatFor(id, TESTER).orElseThrow();
        int other = games.joinGame(id, OTHER_TESTER).orElseThrow();
        assertThat(games.startGame(id)).isTrue();
        assertThat(games.viewForAccount(id, OTHER_TESTER)).isPresent();

        REVOKED.add(OTHER_TESTER);

        assertThat(games.viewForAccount(id, OTHER_TESTER)).as("no map").isEmpty();
        assertThat(games.summaryFor(id, OTHER_TESTER)).isEmpty();
        assertThat(games.diplomacyFor(id, OTHER_TESTER)).isEmpty();
        assertThat(games.undoOrders(id, other, 1)).as("no planning").isFalse();
        assertThat(games.submitTurn(id, other, 1)).as("no turn").isFalse();
        assertThat(lobbyOf(OTHER_TESTER)).isEmpty();
        assertThat(games.viewForAccount(id, TESTER)).as("the others play on").isPresent();
        assertThat(games.submitTurn(id, own, 1)).isTrue();

        registry.writeState(id, state -> { state.endGame(own); return null; });
        assertThat(games.reviewFor(id, OTHER_TESTER)).as("no review").isEmpty();
        assertThat(games.reviewFor(id, TESTER)).isPresent();
        assertThat(games.visibleGamesFor(OTHER_TESTER, GameListScope.ARCHIVE)).extracting(GameSummary::gameId)
                .doesNotContain(id);

        REVOKED.remove(OTHER_TESTER);

        assertThat(games.reviewFor(id, OTHER_TESTER)).as("back with the role").isPresent();
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
        return GameSetup.defaults().seatPlayers(2, 0).chooseGalaxy(12, GalaxyLayout.EVEN).normalized();
    }
}
