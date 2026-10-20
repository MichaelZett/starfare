package de.zettsystems.starfare.simulation;

import de.zettsystems.starfare.ai.application.DefaultSpacewardPlanning;
import de.zettsystems.starfare.ai.values.AiStrategy;
import de.zettsystems.starfare.combat.application.DefaultCoalitionCombatService;
import de.zettsystems.starfare.combat.application.DefaultCombatService;
import de.zettsystems.starfare.economy.application.DefaultEconomyService;
import de.zettsystems.starfare.economy.domain.RecyclingOrders;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.game.application.DefaultPlayerViewBuilder;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GalaxyLayout;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.navigation.application.DefaultNavigationService;
import de.zettsystems.starfare.report.application.DefaultReportService;
import de.zettsystems.starfare.turn.application.RoundPipeline;
import de.zettsystems.starfare.turn.application.ShipResearchSpacewardTurnEngine;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.assertj.core.api.Assertions.assertThat;

class ResearchAiTest {
    @ParameterizedTest @EnumSource(AiStrategy.class)
    void everyProfileRunsResearchWithoutBreakingRestorationOrTheMaterialLedger(AiStrategy strategy) {
        var state = new Scenario(RulesetRef.SPACEWARD_SHIP_RESEARCH, GalaxyLayout.EVEN, 8, 42, 314159, 60, 10).create(2);
        state.players().replaceAll(player -> new de.zettsystems.starfare.game.values.Player(player.id(), player.name(), true,
                player.colorHex(), player.empireName(), strategy));
        var economy = new DefaultEconomyService();
        var fleets = new DefaultFleetService();
        var reports = new DefaultReportService();
        var navigation = new DefaultNavigationService();
        var diplomacy = new de.zettsystems.starfare.diplomacy.application.DefaultDiplomacyService();
        var random = new java.util.Random(123);
        var pipeline = new RoundPipeline(new DefaultCombatService(reports), reports, fleets, navigation, diplomacy,
                new DefaultCoalitionCombatService(reports, navigation, _ -> random.nextDouble()));
        var engine = new ShipResearchSpacewardTurnEngine(pipeline, economy,
                new DefaultSpacewardPlanning(economy, new DefaultPlayerViewBuilder()), fleets, reports);
        long balance = RecyclingOrders.balanceTenths(state);
        for (int round = 0; round < 60 && !state.gameOver(); round++) {
            engine.advanceTurn(state);
            assertThat(RecyclingOrders.balanceTenths(state)).isEqualTo(balance);
            state = GameState.fromSnapshot(GameState.toSnapshot(state));
        }
        assertThat(state.metals().orElseThrow().research().empires().values())
                .allMatch(research -> research.percent() == (strategy == AiStrategy.RUSH ? 5 : 10));
        assertThat(state.turn()).isGreaterThan(1);
    }
}
