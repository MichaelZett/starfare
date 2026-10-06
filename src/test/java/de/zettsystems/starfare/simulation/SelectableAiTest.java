package de.zettsystems.starfare.simulation;

import de.zettsystems.starfare.ai.application.DefaultAiService;
import de.zettsystems.starfare.ai.application.DefaultSpacewardPlanning;
import de.zettsystems.starfare.ai.values.AiStrategy;
import de.zettsystems.starfare.economy.application.DefaultEconomyService;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.game.application.DefaultPlayerViewBuilder;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.assertj.core.api.Assertions.assertThat;

class SelectableAiTest {
    @ParameterizedTest
    @EnumSource(AiStrategy.class)
    void gamePlanningMatchesBenchmarkCommandsForEverySupportedRuleset(AiStrategy strategy) {
        for (var rules : List.of(RulesetRef.SECTOR_FORCES, RulesetRef.SPACEWARD, RulesetRef.SPACEWARD_ALLIANCE)) {
            if (!strategy.supports(rules)) { continue; }
            var state = new Scenario(rules, GalaxyLayout.EVEN, 16, 101, 314159, 30, 10).create(2);
            state.players().replaceAll(p -> new Player(p.id(), p.name(), true, p.colorHex(), p.empireName(), strategy));
            var views = new DefaultPlayerViewBuilder();
            var expected = state.players().stream().map(p -> strategy == AiStrategy.BASELINE
                    ? BaselinePolicy.plan(state, p.id(), views)
                    : HeuristicPolicy.plan(Strategy.valueOf(strategy.name()), Observation.capture(state, p.id(), views))).toList();
            plan(state);
            for (int index = 0; index < expected.size(); index++) {
                int player = index + 1;
                assertThat(state.pendingOrders().getOrDefault(player, List.of())).containsExactlyElementsOf(expected.get(index).sends());
                expected.get(index).expansion().forEach((system, points) ->
                        assertThat(state.industries().get(system).expansionAllocation()).isEqualTo(points));
            }
        }
    }

    @ParameterizedTest
    @EnumSource(AiStrategy.class)
    void selectedGameAiDoesNotReactToHiddenGarrisonsOrEnemyOrders(AiStrategy strategy) {
        var state = new GameState();
        state.rememberSetup(GameSetup.defaults().selectRuleset(RulesetRef.SPACEWARD_ALLIANCE));
        state.players().addAll(List.of(new Player(1, "AI", true, "#fff", "AI", strategy),
                new Player(2, "Enemy", false, "#000")));
        state.systems().addAll(List.of(new StarSystem(1, "Home", 0, 0, 1, 80, 5, false),
                new StarSystem(2, "Hidden", 10000, 0, 2, 5, 5, false),
                new StarSystem(3, "Neutral", 100, 0, null, 2, 3, true)));
        state.start();
        var before = GameState.copyOf(state);
        state.updateSystem(2, system -> system.reinforce(9999));
        state.pendingOrders().put(2, List.of(new de.zettsystems.starfare.fleet.values.FleetOrder.Send(2, 2, 1, 10)));
        plan(before);
        plan(state);
        assertThat(state.pendingOrders().getOrDefault(1, List.of())).isEqualTo(before.pendingOrders().getOrDefault(1, List.of()));
        assertThat(state.industries().get(1)).isEqualTo(before.industries().get(1));
    }

    @Test
    void rushAndDefenseProduceDifferentRealGameOrders() {
        var state = new Scenario(RulesetRef.SECTOR_FORCES, GalaxyLayout.EVEN, 8, 101, 314159, 20, 10).create(2);
        state.players().replaceAll(p -> new Player(p.id(), p.name(), p.id() == 1, p.colorHex(), p.empireName(), AiStrategy.RUSH));
        var defensive = GameState.copyOf(state);
        defensive.players().replaceAll(p -> new Player(p.id(), p.name(), p.ai(), p.colorHex(), p.empireName(), AiStrategy.DEFENSE));
        plan(state);
        plan(defensive);
        assertThat(state.pendingOrders().get(1)).isNotEmpty().isNotEqualTo(defensive.pendingOrders().get(1));
    }

    private static void plan(GameState state) {
        var fleets = new DefaultFleetService();
        if (state.ruleset().spaceward()) {
            new DefaultSpacewardPlanning(new DefaultEconomyService(), new DefaultPlayerViewBuilder()).plan(state, fleets);
        } else {
            new DefaultAiService().planAiTurns(state, fleets);
        }
    }
}
