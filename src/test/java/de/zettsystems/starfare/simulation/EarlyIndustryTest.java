package de.zettsystems.starfare.simulation;

import de.zettsystems.starfare.economy.application.DefaultEconomyService;
import de.zettsystems.starfare.economy.domain.Industry;
import de.zettsystems.starfare.economy.domain.RecyclingOrders;
import de.zettsystems.starfare.game.application.DefaultPlayerViewBuilder;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.testsupport.ColonyStates;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.*;

class EarlyIndustryTest {
    private static final RulesetRef W7 = RulesetRef.SPACEWARD_EARLY_INDUSTRY;

    @ParameterizedTest
    @CsvSource({"1,2", "2,2", "3,2", "4,2", "5,3", "10,10", "19,37", "20,40", "25,50", "49,98"})
    void pricesMatchTheAcceptedCurveAndKeepW6Fixed(int capacity, int price) {
        var industry = Industry.establish(capacity);
        assertThat(industry.expansionCost(W7)).isEqualTo(price);
        assertThat(industry.expansionCost(RulesetRef.SPACEWARD_EQUAL_METAL)).isEqualTo(2 * capacity);
        assertThat(industry.expansionCost()).isEqualTo(2 * capacity);
    }

    @Test void productionMatchesTheStudiedModelForAllValidCapacitiesAllocationsAndProgress() {
        var quality = ColonyStates.ready().colonies().get(1).quality();
        for (int cap = 1; cap <= 50; cap++) {
            int price = Industry.establish(cap).expansionCost(W7);
            for (int progress = 0; progress < price; progress++) {
                for (int allocation = 0; allocation <= (cap == 50 ? 0 : cap); allocation++) {
                    var industry = new Industry(cap, allocation, progress);
                    assertThat(industry.expandForOneTurn(W7)).isEqualTo(InvestmentModel.EARLY_COST.grow(industry, quality));
                }
            }
        }
        assertThat(new Industry(4, 4, 1).expandForOneTurn(W7)).isEqualTo(new Industry(6, 4, 0));
        assertThat(new Industry(49, 49, 97).expandForOneTurn(W7)).isEqualTo(new Industry(50, 0, 48));
    }

    @Test void realRoundRestorationAndHistoricalViewsUseTheNewPriceWithoutCreatingMetal() {
        var original = ColonyStates.ready(W7);
        var industries = new HashMap<>(original.industries());
        industries.put(2, new Industry(4, 0, 1));
        var state = InvestmentSnapshots.industry(original, industries);
        state.start();
        var economy = new DefaultEconomyService();
        assertThat(economy.allocateProduction(state, 1, 2, 4, 0)).isTrue();
        var views = new DefaultPlayerViewBuilder();
        assertThat(views.forPlayer(state, 1).systems().stream().filter(s -> s.id() == 2)
                .findFirst().orElseThrow().industry().expansionCost()).isEqualTo(2);
        var frame = state.replayFrames().get(1);
        var forecast = state.metalPreview();
        long balance = RecyclingOrders.balanceTenths(state);
        var outcomes = economy.produce(state, Map.of());
        assertThat(state.industries().get(2)).isEqualTo(new Industry(6, 4, 0));
        assertThat(state.getSystem(2).garrison()).isEqualTo(3);
        assertThat(state.getSystem(2).productionPerTurn()).isEqualTo(2);
        assertThat(outcomes.stream().filter(p -> p.systemId() == 2).findFirst().orElseThrow().capacityGained()).isEqualTo(2);
        assertThat(state.metals().orElseThrow()).isEqualTo(forecast.economy());
        assertThat(RecyclingOrders.balanceTenths(state)).isEqualTo(balance);
        var restored = GameState.fromSnapshot(GameState.toSnapshot(state));
        assertThat(restored.ruleset()).isEqualTo(W7);
        assertThat(restored.metalPreview()).isEqualTo(state.metalPreview());
        assertThat(views.forReplay(state, frame, 1).systems().stream().filter(s -> s.id() == 2)
                .findFirst().orElseThrow().industry().expansionCost()).isEqualTo(2);
    }

    @Test void snapshotsValidateProgressAgainstTheirStoredRuleVersion() {
        for (var rules : new RulesetRef[] {W7, RulesetRef.SPACEWARD_EQUAL_METAL}) {
            var state = ColonyStates.ready(rules);
            var industries = new HashMap<>(state.industries());
            industries.put(2, new Industry(4, 0, 3));
            if (rules.equals(W7)) {
                assertThatThrownBy(() -> InvestmentSnapshots.industry(state, industries)).isInstanceOf(IllegalArgumentException.class);
            } else {
                assertThat(InvestmentSnapshots.industry(state, industries).industries().get(2).expansionProgress()).isEqualTo(3);
            }
        }
    }
}
