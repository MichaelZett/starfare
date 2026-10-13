package de.zettsystems.starfare.economy.application;

import de.zettsystems.starfare.ai.application.DefaultSpacewardPlanning;
import de.zettsystems.starfare.ai.values.AiStrategy;
import de.zettsystems.starfare.combat.application.DefaultCombatService;
import de.zettsystems.starfare.economy.domain.MetalEconomy;
import de.zettsystems.starfare.economy.values.EconomyIssues;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.game.application.DefaultPlayerViewBuilder;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.game.values.VisibleSystem;
import de.zettsystems.starfare.report.application.DefaultReportService;
import de.zettsystems.starfare.testsupport.ColonyStates;
import de.zettsystems.starfare.turn.application.DirectEconomySpacewardTurnEngine;
import de.zettsystems.starfare.turn.application.RoundPipeline;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.assertj.core.api.Assertions.*;

class DirectEconomyTest {
    private final DefaultEconomyService economy = new DefaultEconomyService();
    private final DefaultPlayerViewBuilder views = new DefaultPlayerViewBuilder();

    private static GameState running(RulesetRef rules) {
        var state = ColonyStates.ready(rules, DirectEconomyTest::ampleMetal);
        state.start();
        return state;
    }

    private static MetalEconomy ampleMetal(MetalEconomy original) {
        var stocks = new HashMap<>(original.stocks());
        stocks.replaceAll((_, _) -> 100L);
        return new MetalEconomy(original.deposits(), stocks, original.mining(), original.recycling());
    }

    @Test void laborNoLongerLimitsTheCandidateWhileW4KeepsItsRule() {
        var limited = running(RulesetRef.SPACEWARD_ECONOMY_PREVIEW);
        var direct = running(RulesetRef.SPACEWARD_DIRECT_ECONOMY);
        assertThat(limited.colonies().get(1).colony().labor()).isEqualTo(6);
        assertThat(limited.industries().get(1).capacity()).isEqualTo(10);
        assertThat(limited.usableIndustrialCapacity(1)).isEqualTo(6);
        assertThat(direct.usableIndustrialCapacity(1)).isEqualTo(10);
        assertThat(limited.metalPreview().ships()).containsEntry(1, 6);
        assertThat(direct.metalPreview().ships()).containsEntry(1, 10);
        assertThat(economy.allocateProduction(limited, 1, 1, 3, 4)).isFalse();
        assertThat(economy.allocateProduction(direct, 1, 1, 3, 4)).isTrue();
        assertThat(direct.metalPreview().ships()).containsEntry(1, 3);
    }

    @Test void resolutionMatchesThePreviewAndGrowthAloneAddsNoIndustry() {
        var state = running(RulesetRef.SPACEWARD_DIRECT_ECONOMY);
        int garrison = state.getSystem(1).garrison();
        int population = state.colonies().get(1).colony().population();
        int expected = state.metalPreview().ships().get(1);
        var produced = economy.produce(state, Map.of());
        assertThat(produced.stream().filter(result -> result.systemId() == 1).findFirst().orElseThrow().ships()).isEqualTo(expected).isEqualTo(10);
        assertThat(state.getSystem(1).garrison()).isEqualTo(garrison + 10);
        assertThat(state.colonies().get(1).colony().population()).isGreaterThan(population);
        assertThat(state.industries().get(1).capacity()).isEqualTo(10);
        assertThat(state.getSystem(1).productionPerTurn()).isEqualTo(10);
    }

    @Test void bothVersionsSurviveSaveAndLoadWithTheirOwnCapacity() {
        for (var rules : new RulesetRef[] {RulesetRef.SPACEWARD_ECONOMY_PREVIEW, RulesetRef.SPACEWARD_DIRECT_ECONOMY}) {
            var state = running(rules);
            var restored = GameState.fromSnapshot(GameState.toSnapshot(state));
            assertThat(restored.ruleset()).isEqualTo(rules);
            assertThat(restored.usableIndustrialCapacity(1)).isEqualTo(state.usableIndustrialCapacity(1));
            assertThat(restored.metalPreview().ships()).isEqualTo(state.metalPreview().ships());
        }
    }

    @Test void theViewKeepsPopulationAsAtmosphereWithoutLaborWarnings() {
        var limited = own(views.forPlayer(running(RulesetRef.SPACEWARD_ECONOMY_PREVIEW), 1).systems());
        var direct = own(views.forPlayer(running(RulesetRef.SPACEWARD_DIRECT_ECONOMY), 1).systems());
        assertThat(direct.industry().colony().population()).isEqualTo(690);
        assertThat(direct.industry().colony().laborLimited()).isFalse();
        assertThat(direct.industry().usableCapacity()).isEqualTo(10);
        assertThat(limited.industry().usableCapacity()).isEqualTo(6);
        assertThat(reasons(limited)).contains(EconomyIssues.Kind.LABOR);
        assertThat(reasons(direct)).doesNotContain(EconomyIssues.Kind.LABOR);
    }

    @ParameterizedTest @EnumSource(AiStrategy.class)
    void everyAiProfileKeepsLegalAllocationsAcrossRounds(AiStrategy profile) {
        var state = running(RulesetRef.SPACEWARD_DIRECT_ECONOMY);
        state.players().replaceAll(player -> new Player(player.id(), player.name(), true,
                player.colorHex(), player.empireName(), profile));
        var fleets = new DefaultFleetService();
        var reports = new DefaultReportService();
        var ai = new DefaultSpacewardPlanning(economy, views);
        var engine = new DirectEconomySpacewardTurnEngine(new RoundPipeline(new DefaultCombatService(reports), reports, fleets),
                economy, ai, fleets, reports);
        engine.advanceTurn(state);
        var restored = GameState.fromSnapshot(GameState.toSnapshot(state));
        engine.advanceTurn(restored);
        assertThat(restored.turn()).isEqualTo(3);
        assertThat(restored.industries().get(1).expansionAllocation()).isLessThanOrEqualTo(restored.usableIndustrialCapacity(1));
    }

    private static VisibleSystem own(java.util.List<VisibleSystem> systems) {
        return systems.stream().filter(system -> system.id() == 1).findFirst().orElseThrow();
    }

    private static java.util.List<EconomyIssues.Kind> reasons(VisibleSystem system) {
        return EconomyIssues.forSystems(java.util.List.of(system), 1, Set.of(), Set.of()).stream()
                .flatMap(issue -> issue.reasons().stream()).toList();
    }
}
