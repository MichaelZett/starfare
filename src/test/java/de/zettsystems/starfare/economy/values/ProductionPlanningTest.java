package de.zettsystems.starfare.economy.values;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ProductionPlanningTest {
    private static IndustryView industry(int capacity, int mining, int expansion, long remaining) {
        return new IndustryView(capacity, expansion, 0, 20, 0, false, 0, null,
                new MetalView(remaining, 3, mining, 0, Math.min(remaining, mining * 3L), capacity - mining - expansion, 0));
    }
    @Test void autarkSplitsOnlyFreeCapacityAndPreservesExpansion() {
        var twelve = industry(12, 0, 2, 100);
        assertThat(ProductionPreset.AUTARK.apply(twelve, ProductionPreset.saved(twelve), Set.of()).allocation())
                .isEqualTo(new AllocationSplit(6, 6, 0));
        var eleven = industry(11, 0, 0, 100);
        assertThat(ProductionPreset.AUTARK.apply(eleven, ProductionPreset.saved(eleven), Set.of()).allocation())
                .isEqualTo(new AllocationSplit(5, 6, 0));
        assertThat(ProductionPreset.AUTARK.apply(twelve, ProductionPreset.saved(twelve), Set.of(2)).allocation())
                .isEqualTo(new AllocationSplit(5, 5, 2));
    }
    @Test void rejectedPresetsPreserveTheInputAndExplainWhy() {
        var current = industry(10, 3, 2, 100);
        var saved = ProductionPreset.saved(current);
        var locked = ProductionPreset.AUTARK.apply(current, saved, Set.of(0));
        assertThat(locked.rejection()).isEqualTo(ProductionPreset.Rejection.LOCKED);
        assertThat(locked.allocation()).isEqualTo(saved);
        assertThat(ProductionPreset.METAL.apply(industry(10, 3, 2, 0), saved, Set.of()).rejection())
                .isEqualTo(ProductionPreset.Rejection.EMPTY_DEPOSIT);
        assertThat(ProductionPreset.AUTARK.apply(industry(1, 0, 0, 100), new AllocationSplit(1, 0, 0), Set.of()).rejection())
                .isEqualTo(ProductionPreset.Rejection.TOO_SMALL);
        assertThat(ProductionPreset.SHIPS.apply(current, saved, Set.of(0)).applicable()).isFalse();
    }
    @Test void evenSplitRespectsUnavailableAreasAndRestPointOrder() {
        var metal = industry(11, 0, 0, 100);
        assertThat(ProductionPreset.EVEN.apply(metal, ProductionPreset.saved(metal), Set.of()).allocation())
                .isEqualTo(new AllocationSplit(4, 4, 3));
        var noMetal = new IndustryView(11, 0, 0, 20, 0, false, 0);
        assertThat(ProductionPreset.EVEN.apply(noMetal, ProductionPreset.saved(noMetal), Set.of()).allocation())
                .isEqualTo(new AllocationSplit(6, 0, 5));
        var maximum = new IndustryView(EconomyRules.MAX_CAPACITY, 0, 0, 20, 0, false, 0);
        assertThat(ProductionPreset.INDUSTRY.apply(maximum, ProductionPreset.saved(maximum), Set.of()).rejection())
                .isEqualTo(ProductionPreset.Rejection.LIMIT);
    }
    @Test void everyApplicablePresetConservesCapacityAndLockedValues() {
        for (int capacity = 0; capacity <= 25; capacity++) {
            var industry = industry(capacity, 0, 0, 100);
            var current = ProductionPreset.saved(industry);
            for (var preset : ProductionPreset.values()) {
                for (var locks : java.util.List.of(Set.<Integer>of(), Set.of(0), Set.of(1), Set.of(2), Set.of(0, 1, 2))) {
                    var result = preset.apply(industry, current, locks);
                    if (result.applicable()) {
                        assertThat(result.allocation().total()).isEqualTo(capacity);
                        locks.forEach(index -> assertThat(result.allocation().at(index)).isEqualTo(current.at(index)));
                    } else { assertThat(result.allocation()).isEqualTo(current); }
                }
            }
        }
    }
    @Test void inputsKeepPinsAndViewStateAcrossSystemsButNotAcrossGames() {
        var inputs = new ProductionInputs(); var industry = industry(10, 0, 0, 100);
        inputs.reconcile("one", 1, Map.of(1, industry, 2, industry));
        var alpha = inputs.forSystem(1, 1, industry); alpha.pin(2, true); alpha.expand(); alpha.focus(1);
        inputs.forSystem(2, 1, industry).choose(new AllocationSplit(0, 10, 0));
        assertThat(inputs.forSystem(1, 1, industry).locks()).containsExactly(2);
        inputs.reconcile("one", 1, Map.of(1, industry, 2, industry));
        assertThat(alpha.expanded()).isTrue();
        assertThat(alpha.focus()).isEqualTo(1);
        assertThat(inputs.forSystem(2, 1, industry).changed()).isFalse();
        inputs.reconcile("two", 1, Map.of(1, industry));
        assertThat(inputs.forSystem(1, 1, industry).locks()).isEmpty();
    }
    @Test void acceptedAndRejectedSavesNeverLeaveAnUnsavedWish() {
        var inputs = new ProductionInputs(); var industry = industry(10, 0, 0, 100);
        var input = inputs.forSystem(1, 1, industry);
        input.adjust(1, 4);
        assertThat(input.changed()).isTrue();
        assertThat(input.allocation()).isEqualTo(new AllocationSplit(6, 4, 0));
        var shortened = new de.zettsystems.starfare.game.values.RelocationAdjustment(3, 1, 2, "Vega", 6, 4);
        input.accepted(java.util.List.of(shortened));
        assertThat(input.changed()).isFalse();
        assertThat(input.saved()).isEqualTo(new AllocationSplit(6, 4, 0));
        assertThat(input.status()).isEqualTo(ProductionInputs.Status.SAVED);
        assertThat(input.relocations()).containsExactly(shortened);
        input.adjust(1, 8);
        assertThat(input.status()).isEqualTo(ProductionInputs.Status.NONE);
        input.rejected();
        assertThat(input.allocation()).isEqualTo(new AllocationSplit(6, 4, 0));
        assertThat(input.status()).isEqualTo(ProductionInputs.Status.NOT_SAVED);
    }
    @Test void serverUpdatesWinAndANewRoundClearsFeedback() {
        var inputs = new ProductionInputs(); var industry = industry(10, 0, 0, 100);
        inputs.reconcile("one", 1, Map.of(1, industry));
        var input = inputs.forSystem(1, 1, industry);
        input.adjust(1, 3); input.accepted(java.util.List.of()); input.focus(4);
        inputs.reconcile("one", 1, Map.of(1, industry(10, 1, 0, 100)));
        assertThat(input.allocation()).isEqualTo(new AllocationSplit(9, 1, 0));
        assertThat(input.status()).isEqualTo(ProductionInputs.Status.SAVED);
        inputs.reconcile("one", 2, Map.of(1, industry(8, 1, 0, 100)));
        assertThat(input.allocation()).isEqualTo(new AllocationSplit(7, 1, 0));
        assertThat(input.status()).isEqualTo(ProductionInputs.Status.NONE);
        assertThat(input.focus()).isNull();
        assertThat(input.turn()).isEqualTo(2);
        inputs.reconcile("one", 2, Map.of());
        assertThat(inputs.forSystem(1, 2, industry).locks()).isEmpty();
    }
    @Test void outcomesSeparatePointsYieldAndActualResults() {
        var plan = new IndustryView(6, 0, 8, 12, 0, false, 0, null, new MetalView(100, 3, 2, 10, 6, 4, 4));
        var outcome = ProductionOutcome.of(new AllocationSplit(4, 2, 0), plan);
        assertThat(outcome.ships()).isEqualTo(4);
        assertThat(outcome.metal()).isEqualTo(6);
        assertThat(outcome.yieldPerPoint()).isEqualTo(3);
        assertThat(outcome.shipsShort()).isFalse();
        assertThat(outcome.depositLimited()).isFalse();
        var short_ = ProductionOutcome.of(new AllocationSplit(4, 2, 0),
                new IndustryView(6, 0, 8, 12, 0, false, 0, null, new MetalView(1, 3, 2, 0, 1, 4, 2)));
        assertThat(short_.shipsShort()).isTrue();
        assertThat(short_.plannedShips()).isEqualTo(4);
        assertThat(short_.ships()).isEqualTo(2);
        assertThat(short_.depositLimited()).isTrue();
        var growth = ProductionOutcome.of(new AllocationSplit(3, 0, 2),
                new IndustryView(5, 2, 8, 12, 0, false, 0));
        assertThat(growth.progress()).isEqualTo(8);
        assertThat(growth.nextProgress()).isEqualTo(10);
        assertThat(growth.cost()).isEqualTo(12);
        assertThat(ProductionOutcome.of(new AllocationSplit(0, 0, 9), new IndustryView(10, 9, 8, 12, 0, false, 0)).nextProgress()).isEqualTo(12);
    }
    @Test void metalChangeIsTheRoundedStockDifference() {
        assertThat(new MetalBalance(24, 2, 1, 6, 21).formattedChange()).isEqualTo("(\u22123)");
        assertThat(new MetalBalance(10, 4, 0, 1, 13).formattedChange()).isEqualTo("(+3)");
        assertThat(new MetalBalance(5, 0, 0, 0, 5).formattedChange()).isEqualTo("(±0)");
        assertThat(new MetalBalance(24, 2, 1, 6, 21).change()).isEqualTo(-3);
    }
}
