package de.zettsystems.starfare.simulation;

import de.zettsystems.starfare.ai.domain.AiPolicy;
import de.zettsystems.starfare.ai.values.AiObservation;
import de.zettsystems.starfare.ai.values.AiStrategy;
import de.zettsystems.starfare.economy.application.DefaultEconomyService;
import de.zettsystems.starfare.game.application.DefaultPlayerViewBuilder;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GalaxyLayout;
import de.zettsystems.starfare.game.values.RulesetRef;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EconomyBenchmarkTest {
    private static Scenario scenario() { return new Scenario(RulesetRef.SPACEWARD_RECYCLING, GalaxyLayout.EVEN, 8, 42, 314159, 30, 10); }

    @ParameterizedTest @EnumSource(AiStrategy.class)
    void existingProfilesRemainLegalWithAllEnvironments(AiStrategy strategy) {
        for (var environment : EconomyMaps.Environment.values()) {
            var result = EconomyBenchmark.play(scenario(), environment, List.of(
                    new EconomyProfiles.Profile(strategy, EconomyProfiles.Policy.STANDARD),
                    new EconomyProfiles.Profile(AiStrategy.CONCENTRATION, EconomyProfiles.Policy.STANDARD)), 1, new StringBuilder(), new StringBuilder());
            assertThat(result.rounds()).isBetween(1, 30);
            assertThat(result.digest()).hasSize(64);
        }
    }

    @ParameterizedTest @EnumSource(EconomyProfiles.Policy.class)
    void policiesAndRecordedEvidenceAreDeterministic(EconomyProfiles.Policy policy) {
        var profiles = List.of(new EconomyProfiles.Profile(AiStrategy.CONCENTRATION, policy),
                new EconomyProfiles.Profile(AiStrategy.CONCENTRATION, EconomyProfiles.Policy.STANDARD));
        var metrics = new StringBuilder(); var orders = new StringBuilder();
        var first = EconomyBenchmark.play(scenario(), EconomyMaps.Environment.EXHAUSTED, profiles, 1, metrics, orders);
        var repeatedMetrics = new StringBuilder(); var repeatedOrders = new StringBuilder();
        var second = EconomyBenchmark.play(scenario(), EconomyMaps.Environment.EXHAUSTED, profiles, 1, repeatedMetrics, repeatedOrders);
        assertThat(second).isEqualTo(first);
        assertThat(repeatedMetrics).hasToString(metrics.toString());
        assertThat(repeatedOrders).hasToString(orders.toString());
    }

    @Test void pairedMapsIsolateResourcesAndGrowth() {
        var poor = scenario().create(2, EconomyMaps.Environment.POOR);
        var rich = scenario().create(2, EconomyMaps.Environment.RICH);
        var harsh = scenario().create(2, EconomyMaps.Environment.HARSH);
        var fertile = scenario().create(2, EconomyMaps.Environment.FERTILE);
        assertThat(poor.systems()).isEqualTo(rich.systems());
        poor.colonies().forEach((id, colony) -> {
            assertThat(colony.quality().suitability()).isEqualTo(rich.colonies().get(id).quality().suitability());
            assertThat(colony.colony()).isEqualTo(rich.colonies().get(id).colony());
            assertThat(poor.metals().orElseThrow().deposits().get(id).remaining()).isLessThan(rich.metals().orElseThrow().deposits().get(id).remaining());
            assertThat(harsh.colonies().get(id).quality().metalRichness()).isEqualTo(fertile.colonies().get(id).quality().metalRichness());
            assertThat(harsh.metals().orElseThrow().deposits()).containsEntry(id, fertile.metals().orElseThrow().deposits().get(id));
            assertThat(harsh.colonies().get(id).quality().suitability()).isNotEqualTo(fertile.colonies().get(id).quality().suitability());
        });
        assertThat(EconomyBenchmark.canonical(GameState.fromSnapshot(GameState.toSnapshot(poor)))).isEqualTo(EconomyBenchmark.canonical(poor));
    }

    @Test void industryPolicyAccountsForMiningBeforeCommandValidation() {
        var state = scenario().create(2);
        var economy = new DefaultEconomyService();
        int home = state.systems().stream().filter(system -> Integer.valueOf(1).equals(system.ownerId())).findFirst().orElseThrow().id();
        assertThat(economy.allocateProduction(state, 1, home, 0, 4)).isTrue();
        var view = new DefaultPlayerViewBuilder().forPlayer(state, 1);
        var orders = AiPolicy.plan(AiStrategy.INDUSTRY, new AiObservation(1, view, Map.of(), Set.of()));
        assertThat(orders.expansion()).containsEntry(home, 1);
        assertThat(economy.allocateExpansion(state, 1, home, orders.expansion().get(home))).isTrue();
    }

    @Test void rejectsUnknownDuplicateAndUnboundedExperimentOptions() {
        assertThatThrownBy(() -> EconomyBenchmark.Options.parse(new String[]{"--seeds=0"})).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EconomyBenchmark.Options.parse(new String[]{"--seeds=1", "--seeds=2"})).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EconomyBenchmark.Options.parse(new String[]{"--secret=3"})).isInstanceOf(IllegalArgumentException.class);
        assertThat(EconomyBenchmark.lineups("comparison")).hasSize(16);
        assertThat(EconomyBenchmark.lineups("profiles")).hasSize(16);
    }
}
