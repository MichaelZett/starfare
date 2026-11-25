package de.zettsystems.starfare.economy.values;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AllocationSplitTest {
    private static IndustryView industry(int capacity, int expansion) {
        return new IndustryView(capacity, expansion, 0, EconomyRules.expansionCost(capacity), 0, false);
    }

    @Test void theSavedSplitIsShipbuildingAndExpansionOfTheIndustry() {
        assertThat(AllocationSplit.saved(industry(10, 3))).isEqualTo(new AllocationSplit(7, 3));
    }

    @Test void raisingOneShareTakesThePointsFromTheOther() {
        var split = new AllocationSplit(7, 3);
        int[] limits = AllocationSplit.limits(industry(10, 3));
        assertThat(split.adjust(AllocationSplit.EXPANSION, 6, limits)).isEqualTo(new AllocationSplit(4, 6));
        assertThat(split.adjust(AllocationSplit.SHIPS, 10, limits)).isEqualTo(new AllocationSplit(10, 0));
    }

    @Test void requestsStayWithinTheTotalAndTheLimits() {
        var split = new AllocationSplit(7, 3);
        int[] limits = AllocationSplit.limits(industry(10, 3));
        assertThat(split.adjust(AllocationSplit.EXPANSION, 99, limits)).isEqualTo(new AllocationSplit(0, 10));
        assertThat(split.adjust(AllocationSplit.EXPANSION, -4, limits)).isEqualTo(new AllocationSplit(10, 0));
    }

    @Test void anIndustryAtItsMaximumCannotExpand() {
        var full = industry(EconomyRules.MAX_CAPACITY, 0);
        assertThat(AllocationSplit.limits(full)).containsExactly(EconomyRules.MAX_CAPACITY, 0);
        assertThat(AllocationSplit.saved(full).adjust(AllocationSplit.EXPANSION, 5, AllocationSplit.limits(full)))
                .isEqualTo(new AllocationSplit(EconomyRules.MAX_CAPACITY, 0));
    }

    @Test void sharesAreReadByIndexAndMustNotBeNegative() {
        var split = new AllocationSplit(4, 2);
        assertThat(split.at(AllocationSplit.SHIPS)).isEqualTo(4);
        assertThat(split.at(AllocationSplit.EXPANSION)).isEqualTo(2);
        assertThat(split.total()).isEqualTo(6);
        assertThatThrownBy(() -> split.at(2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AllocationSplit(-1, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
