package de.zettsystems.starfare.simulation;

import de.zettsystems.starfare.economy.values.IndustryView;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IndustryPolicyTest {
    @ParameterizedTest
    @EnumSource(value = Strategy.class, names = {"INDUSTRY_LIGHT", "INDUSTRY_ADAPTIVE"})
    void immediateThreatAndMaximumCapacityKeepAllProductionForShips(Strategy strategy) {
        assertThat(IndustryPolicy.allocation(strategy, industry(12, 0, 0), 2, 100)).isZero();
        assertThat(IndustryPolicy.allocation(strategy, industry(50, 0, 0), 10, 100)).isZero();
    }

    @Test
    void conservativeAllocationRetainsThreeQuartersForShips() {
        assertThat(IndustryPolicy.allocation(Strategy.INDUSTRY_LIGHT, industry(12, 0, 0), 5, 100)).isEqualTo(3);
    }

    @Test
    void adaptiveAllocationRespondsToVisibleDistanceAndMilitaryStock() {
        var industry = industry(12, 0, 0);
        assertThat(IndustryPolicy.allocation(Strategy.INDUSTRY_ADAPTIVE, industry, 5, 24)).isEqualTo(6);
        assertThat(IndustryPolicy.allocation(Strategy.INDUSTRY_ADAPTIVE, industry, 4, 24)).isEqualTo(3);
        assertThat(IndustryPolicy.allocation(Strategy.INDUSTRY_ADAPTIVE, industry, 5, 23)).isZero();
        assertThat(IndustryPolicy.allocation(Strategy.INDUSTRY_ADAPTIVE, industry(12, 0, 1), 5, 24)).isZero();
    }

    @Test
    void adaptiveAllocationOnlyFinishesTheNextExpansionWhenNearlyPaid() {
        assertThat(IndustryPolicy.allocation(Strategy.INDUSTRY_ADAPTIVE, industry(12, 23, 0), 5, 100)).isEqualTo(1);
    }

    @Test
    void explicitProfilesAreValidatedWithoutChangingHistoricalDefaults() {
        var options = BenchmarkOptions.parse(new String[]{"--rules=spaceward-1.1", "--profiles=INDUSTRY_LIGHT,INDUSTRY_ADAPTIVE"});
        assertThat(options.selectedProfiles()).containsExactly(Strategy.INDUSTRY_LIGHT, Strategy.INDUSTRY_ADAPTIVE);
        assertThat(Strategy.forRules(options.rules().getFirst())).doesNotContain(Strategy.INDUSTRY_LIGHT, Strategy.INDUSTRY_ADAPTIVE);
        for (String profiles : List.of("RUSH", "RUSH,RUSH", "RUSH,UNKNOWN", "")) {
            assertThatThrownBy(() -> BenchmarkOptions.parse(new String[]{"--profiles=" + profiles}))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> BenchmarkOptions.parse(new String[]{"--rules=classic", "--profiles=RUSH,INDUSTRY_ADAPTIVE"}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BenchmarkOptions.parse(new String[]{"--mode=decision", "--rules=spaceward-1.1", "--profiles=RUSH,INDUSTRY_LIGHT"}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BenchmarkOptions.parse(new String[]{"--mode=multiplayer", "--rules=spaceward-1.1",
                "--profiles=BASELINE,RUSH,EXPANSION,CONCENTRATION,DEFENSE,INDUSTRY,INDUSTRY_LIGHT,INDUSTRY_ADAPTIVE"}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static IndustryView industry(int capacity, int progress, int reserveShortfall) {
        return new IndustryView(capacity, 0, progress, capacity * 2, 0, false, reserveShortfall);
    }
}
