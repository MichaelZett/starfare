package de.zettsystems.starfare.simulation;

import de.zettsystems.starfare.ai.values.AiStrategy;
import de.zettsystems.starfare.economy.domain.Industry;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GalaxyLayout;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.testsupport.ColonyStates;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InvestmentTest {
    @Test void baselineMatchesProductiveGrowthIncludingProgressAndMaximum() {
        var quality = ColonyStates.ready().colonies().get(1).quality();
        for (int cap = 1; cap <= 50; cap++) {
            for (int progress = 0; progress < 2 * cap; progress++) {
                var industry = new Industry(cap, cap == 50 ? 0 : cap / 2, progress);
                assertThat(InvestmentModel.BASELINE.grow(industry, quality)).isEqualTo(industry.expandForOneTurn());
            }
        }
        assertThat(InvestmentModel.EARLY_COST.grow(new Industry(5, 5, 0), quality))
                .isEqualTo(new Industry(6, 5, 2));
        assertThat(InvestmentModel.EARLY_COST.cost(5, quality)).isEqualTo(3);
        assertThat(InvestmentModel.EARLY_COST.cost(25, quality)).isEqualTo(50);
    }

    @Test void ordinaryCostExperimentsAreByteEquivalentToTheExistingReferenceEngine() {
        var scenario = new Scenario(RulesetRef.SPACEWARD_EQUAL_METAL, GalaxyLayout.EVEN, 8, 42, 314159, 30, 10);
        var profiles = List.of(new EconomyProfiles.Profile(AiStrategy.CONCENTRATION, EconomyProfiles.Policy.STANDARD),
                new EconomyProfiles.Profile(AiStrategy.CONCENTRATION, EconomyProfiles.Policy.STANDARD));
        var expected = EconomyBenchmark.play(scenario, EconomyMaps.Environment.NATURAL, profiles, 1, new StringBuilder(), new StringBuilder());
        for (var model : List.of(InvestmentModel.BASELINE, InvestmentModel.HALF_COST, InvestmentModel.EARLY_COST, InvestmentModel.QUALITY_COST)) {
            var actual = InvestmentMatch.play(scenario, model, InvestmentPolicy.SHIPS, AiStrategy.CONCENTRATION, 1, 1, new StringBuilder());
            assertThat(actual.digest()).isEqualTo(expected.digest());
        }
    }

    @Test void qualityChangesOnlyTheExperimentalPriceAndEarlyDiscountEndsAtLargeIndustry() {
        var fertile = ColonyStates.ready().colonies().get(1).quality();
        var harsh = new de.zettsystems.starfare.economy.values.SystemQuality(fertile.star(), fertile.luminosity(),
                fertile.orbitAu(), fertile.body(), fertile.moon(),
                de.zettsystems.starfare.economy.values.SystemQuality.Atmosphere.NONE, fertile.metalRichness());
        assertThat(InvestmentModel.QUALITY_COST.cost(10, fertile)).isEqualTo(8);
        assertThat(InvestmentModel.QUALITY_COST.cost(10, harsh)).isEqualTo(13);
        assertThat(InvestmentModel.EARLY_COST.cost(10, harsh)).isEqualTo(10);
        assertThat(InvestmentModel.BASELINE.cost(10, fertile)).isEqualTo(20);
        assertThat(InvestmentModel.EARLY_COST.cost(25, fertile)).isEqualTo(50);
    }

    @Test void lateInvestmentActuallyStartsInRoundForty() {
        var state = ColonyStates.ready(RulesetRef.SPACEWARD_EQUAL_METAL);
        state.start();
        var targets = new HashMap<Integer, Integer>();
        InvestmentPolicy.LATE.plan(state, 1, InvestmentModel.BASELINE, targets);
        assertThat(state.industries().get(1).expansionAllocation()).isZero();
        for (int round = 1; round < 40; round++) { state.nextTurn(); }
        InvestmentPolicy.LATE.plan(state, 1, InvestmentModel.BASELINE, targets);
        assertThat(state.industries().get(1).expansionAllocation()).isEqualTo(2);
    }

    @Test void changedCostsMatchPreviewAndPreserveMetalThroughValidatedRestoration() {
        var state = ColonyStates.ready(RulesetRef.SPACEWARD_EQUAL_METAL);
        state.start();
        var economy = new InvestmentEconomy(InvestmentModel.HALF_COST);
        assertThat(economy.allocateProduction(state, 1, 1, 10, 0)).isTrue();
        var forecast = state.metalPreview();
        var produced = economy.produce(state, Map.of());
        var changed = economy.finish(state);
        assertThat(changed.industries().get(1).capacity()).isEqualTo(11);
        assertThat(produced.stream().filter(p -> p.systemId() == 1).findFirst().orElseThrow().capacityGained()).isEqualTo(1);
        assertThat(changed.getSystem(1).garrison()).isEqualTo(state.getSystem(1).garrison());
        assertThat(changed.metals().orElseThrow()).isEqualTo(forecast.economy());
        assertThat(GameState.fromSnapshot(GameState.toSnapshot(changed)).metalPreview()).isEqualTo(changed.metalPreview());
    }

    @Test void conquestInterventionChangesInfrastructureOnlyAfterNormalProduction() {
        var state = ColonyStates.ready(RulesetRef.SPACEWARD_EQUAL_METAL);
        var economy = new InvestmentEconomy(InvestmentModel.HALF_CAPTURE);
        economy.produce(state, Map.of());
        int garrison = state.getSystem(2).garrison();
        state.updateSystem(2, system -> system.captureBy(2, system.garrison()));
        var changed = economy.finish(state);
        assertThat(changed.industries().get(2).capacity()).isEqualTo(2);
        assertThat(changed.getSystem(2).garrison()).isEqualTo(garrison);
        assertThat(changed.metals()).isEqualTo(state.metals());
        assertThat(economy.acquired).containsEntry(2, 2);
    }

    @Test void latePolicyAndItsMilitaryReferenceStayIdenticalBeforeRoundForty() {
        var scenario = new Scenario(RulesetRef.SPACEWARD_EQUAL_METAL, GalaxyLayout.EVEN, 8, 42, 314159, 30, 10);
        var ships = InvestmentMatch.play(scenario, InvestmentModel.EARLY_COST, InvestmentPolicy.SHIPS, AiStrategy.RUSH, 1, 1, new StringBuilder());
        var late = InvestmentMatch.play(scenario, InvestmentModel.EARLY_COST, InvestmentPolicy.LATE, AiStrategy.RUSH, 1, 1, new StringBuilder());
        assertThat(late).isEqualTo(ships);
    }

    @Test void allModelsAndPoliciesAreLegalAndRepeatable() {
        var scenario = new Scenario(RulesetRef.SPACEWARD_EQUAL_METAL, GalaxyLayout.EVEN, 8, 42, 314159, 55, 10);
        for (var model : InvestmentModel.values()) {
            for (var policy : InvestmentPolicy.values()) {
                var firstRows = new StringBuilder();
                var first = InvestmentMatch.play(scenario, model, policy, AiStrategy.CONCENTRATION, 1, 1, firstRows);
                var repeatedRows = new StringBuilder();
                assertThat(InvestmentMatch.play(scenario, model, policy, AiStrategy.CONCENTRATION, 1, 1, repeatedRows)).isEqualTo(first);
                assertThat(repeatedRows.toString()).isEqualTo(firstRows.toString());
            }
        }
    }
}
