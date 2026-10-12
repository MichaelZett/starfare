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
    @Test void switchingSystemsRetainsDraftsAndLocksButChangingGamesDoesNot() {
        var drafts = new ProductionDrafts(); var industry = industry(10, 0, 0, 100);
        drafts.reconcile("one", 1, Map.of(1, industry, 2, industry));
        var alpha = drafts.forSystem(1, 1, industry); alpha.pin(2, true); alpha.choose(new AllocationSplit(5, 5, 0));
        drafts.forSystem(2, 1, industry).choose(new AllocationSplit(0, 10, 0));
        assertThat(drafts.forSystem(1, 1, industry).allocation()).isEqualTo(new AllocationSplit(5, 5, 0));
        assertThat(drafts.pending()).containsExactlyInAnyOrder(1, 2);
        alpha.discard(); assertThat(alpha.locks()).containsExactly(2);
        assertThat(drafts.pending()).containsExactly(2);
        drafts.reconcile("two", 1, Map.of(1, industry)); assertThat(drafts.pending()).isEmpty();
        assertThat(drafts.forSystem(1, 1, industry).locks()).isEmpty();
    }
    @Test void roundAndExternalPlanChangesRequireExplicitRecheck() {
        var drafts = new ProductionDrafts(); var industry = industry(10, 0, 0, 100);
        drafts.reconcile("one", 1, Map.of(1, industry));
        var draft = drafts.forSystem(1, 1, industry); draft.choose(new AllocationSplit(5, 5, 0));
        drafts.reconcile("one", 2, Map.of(1, industry));
        assertThat(draft.stale()).isTrue(); assertThat(draft.allocation()).isEqualTo(new AllocationSplit(5, 5, 0));
        assertThat(draft.recheck(2)).isTrue(); assertThat(draft.stale()).isFalse();
        drafts.reconcile("one", 2, Map.of(1, industry(10, 1, 0, 100)));
        assertThat(draft.stale()).isTrue();
        drafts.reconcile("one", 3, Map.of(1, industry(8, 1, 0, 100)));
        assertThat(draft.recheck(3)).isFalse(); assertThat(draft.stale()).isTrue();
        draft.adjust(1, 3);
        assertThat(draft.allocation().total()).isEqualTo(8);
        assertThat(draft.allocation().mining()).isEqualTo(3);
        assertThat(draft.stale()).isTrue();
        assertThat(draft.recheck(3)).isTrue();
        drafts.reconcile("one", 4, Map.of(1, industry(8, 1, 0, 100)));
        draft.discard(); draft.choose(new AllocationSplit(4, 4, 0));
        drafts.reconcile("one", 4, Map.of(1, industry(8, 1, 0, 100)));
        assertThat(draft.stale()).isFalse();
        drafts.reconcile("one", 4, Map.of()); assertThat(drafts.pending()).isEmpty();
    }
}
