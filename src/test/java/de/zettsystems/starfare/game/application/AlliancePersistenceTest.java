package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.diplomacy.values.*;
import de.zettsystems.starfare.game.domain.*;
import de.zettsystems.starfare.game.values.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import static org.assertj.core.api.Assertions.*;

class AlliancePersistenceTest extends AbstractIntegrationTest {
    @Autowired ObjectMapper mapper;
    @Autowired GameStatisticsService statistics;
    @Autowired GameService games;
    @Autowired GameSessionStore store;
    @Autowired GameTemplateService templates;

    @Test void legacyJsonKeepsEachVariantsDefaultsAndSingleWinner() {
        for (var ref : List.of(RulesetRef.SECTOR_FORCES, RulesetRef.SPACEWARD)) {
            var state = sample(ref, false); state.endGame(1);
            var json = (ObjectNode) mapper.valueToTree(GameState.toSnapshot(state));
            json.remove(List.of("victoryRules", "outcome"));
            ((ObjectNode) json.get("originalSetup")).remove("victoryRules");
            var restored = GameState.fromSnapshot(mapper.treeToValue(json, GameStateSnapshot.class));
            assertThat(restored.ruleset()).isEqualTo(ref);
            assertThat(restored.victoryRules().alliancesAllowed()).isEqualTo(ref.spaceward());
            assertThat(restored.victoryRules().allianceVictoryAllowed()).isFalse();
            assertThat(restored.outcome().winnerIds()).containsExactly(1);
            restored.resumeForFullConquest(Instant.now());
            var continued = GameState.fromSnapshot(mapper.readValue(mapper.writeValueAsString(GameState.toSnapshot(restored)), GameStateSnapshot.class));
            assertThat(continued.ruleset()).isEqualTo(ref);
            assertThat(continued.victoryRules().individualSystemPercent()).isEqualTo(100);
            assertThat(continued.victoryRules().allianceVictoryAllowed()).isFalse();
        }
    }

    @Test void groupOutcomeSetupAndReportsSurviveJson() {
        var state = sample(RulesetRef.SPACEWARD_ALLIANCE, true);
        state.winAsAlliance(1, Set.of(1, 2));
        var reports = new de.zettsystems.starfare.report.application.DefaultReportService();
        reports.appendEvent(state, 2, new de.zettsystems.starfare.report.values.TurnEvent.Victory(2, 80, 1, List.of("P1", "P2")));
        reports.appendEvent(state, 3, new de.zettsystems.starfare.report.values.TurnEvent.Defeat(1, "P1, P2", List.of(1, 2), 1));
        state.captureReplayFrame();
        var restored = GameState.fromSnapshot(mapper.readValue(mapper.writeValueAsString(GameState.toSnapshot(state)), GameStateSnapshot.class));
        assertThat(restored.outcome()).isEqualTo(state.outcome());
        assertThat(restored.victoryRules()).isEqualTo(state.victoryRules());
        assertThat(restored.originalSetup()).isEqualTo(state.originalSetup());
        assertThat(restored.reports()).isEqualTo(state.reports());
        assertThat(restored.replayFrames().get(1).outcome()).isEqualTo(state.outcome());
        assertThat(new DefaultPlayerViewBuilder().forPlayer(restored, 2).wonBy(2)).isTrue();
    }

    @Test void bothHumanWinnersCountAsWinsAndEitherCanContinueOnlyOnce() {
        var state = sample(RulesetRef.SPACEWARD_ALLIANCE, true);
        state.seatByUser().putAll(Map.of("ally-one", 1, "ally-two", 2, "loser", 3));
        state.joinedHumanPlayerIds().addAll(Set.of(1, 2, 3));
        state.originalHumanPlayerIds().addAll(Set.of(1, 2, 3));
        state.winAsAlliance(1, Set.of(1, 2));
        var id = GameId.newId(); store.save(new GameSession(id, "Group result", "ally-one", Instant.now(), state));
        statistics.recordFinishedGame(id);
        assertThat(statistics.statisticsFor("ally-one").wins()).isEqualTo(1);
        assertThat(statistics.statisticsFor("ally-two").wins()).isEqualTo(1);
        assertThat(statistics.statisticsFor("loser").losses()).isEqualTo(1);
        assertThat(games.summaryFor(id, "ally-two").orElseThrow().outcome().wonBy(2)).isTrue();
        assertThat(templates.template(id, "ally-two").orElseThrow().setup().victoryRules()).isEqualTo(state.victoryRules());
        assertThat(games.reviewFor(id, "ally-two", 2, true).orElseThrow().wonBy(2)).isTrue();
        assertThat(games.continueAfterVictory(id, "loser")).isFalse();
        assertThat(games.continueAfterVictory(id, "ally-two")).isTrue();
        assertThat(games.continueAfterVictory(id, "ally-one")).isFalse();
        int groupPercent = registry.readState(id, s -> s.victoryRules().allianceSystemPercent());
        assertThat(groupPercent).isEqualTo(100);
        statistics.recordFinishedGame(id);
        assertThat(statistics.statisticsFor("ally-two").wins()).isEqualTo(1);
    }

    private static GameState sample(RulesetRef ref, boolean group) {
        var state = new GameState(); state.resetForNewGame();
        var setup = GameSetup.defaults().selectRuleset(ref);
        if (group) { setup = setup.chooseVictoryRules(new VictoryRules(90, true, true, 80)); }
        state.rememberSetup(setup);
        for (int player = 1; player <= 3; player++) {
            state.players().add(new Player(player, "P" + player, false, GameConfig.PLAYER_PALETTE.get(player - 1)));
            state.systems().add(new StarSystem(player, "S" + player, player * 100, 100, player, 5, 2, false));
        }
        state.start();
        if (ref.spaceward()) { state.agreeTreaties(new DiplomacyState(2,
                List.of(new AllianceGroup(1, Set.of(1, 2), 3, Map.of())), List.of())); }
        return state;
    }
}
