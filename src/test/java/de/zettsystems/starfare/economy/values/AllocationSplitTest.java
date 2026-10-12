package de.zettsystems.starfare.economy.values;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AllocationSplitTest {
    @Test void shipbuildingIsAnExplicitShareOfTheBudget() {
        var split = new AllocationSplit(0, 2, 2);
        assertThat(split.adjust(0, 2, new boolean[3], new int[]{4, 4, 4}))
                .isEqualTo(new AllocationSplit(2, 1, 1));
        assertThat(new AllocationSplit(2, 1, 1).adjust(1, 4, new boolean[3], new int[]{4, 4, 4}))
                .isEqualTo(new AllocationSplit(0, 4, 0));
    }

    @Test void fixedSharesStayFixedAndTiesFavorShipbuilding() {
        var split = new AllocationSplit(2, 2, 2);
        assertThat(split.adjust(1, 4, new boolean[]{false, false, true}, new int[]{6, 6, 6}))
                .isEqualTo(new AllocationSplit(0, 4, 2));
        assertThat(split.adjust(1, 5, new boolean[]{true, false, true}, new int[]{6, 6, 6})).isEqualTo(split);
        assertThat(split.adjust(2, 3, new boolean[3], new int[]{6, 6, 6}))
                .isEqualTo(new AllocationSplit(2, 1, 3));
    }

    @Test void disabledExpansionAndEmptyBudgetAreSafe() {
        var split = new AllocationSplit(3, 3, 0);
        assertThat(split.adjust(0, 4, new boolean[3], new int[]{6, 6, 0})).isEqualTo(new AllocationSplit(4, 2, 0));
        assertThat(new AllocationSplit(0, 0, 0).adjust(0, 9, new boolean[3], new int[3]))
                .isEqualTo(new AllocationSplit(0, 0, 0));
        assertThat(new AllocationSplit(0, 0, 6).adjust(2, 0, new boolean[3], new int[]{6, 6, 6}))
                .isEqualTo(new AllocationSplit(6, 0, 0));
    }

    @Test void everySmallBudgetAndLockCombinationPreservesAllCapacity() {
        for (int capacity = 0; capacity <= 12; capacity++) {
            for (int ships = 0; ships <= capacity; ships++) {
                for (int mining = 0; mining <= capacity - ships; mining++) {
                    verify(new AllocationSplit(ships, mining, capacity - ships - mining));
                }
            }
        }
    }

    private static void verify(AllocationSplit split) {
        for (int mask = 0; mask < 8; mask++) {
            boolean[] locks = {(mask & 1) != 0, (mask & 2) != 0, (mask & 4) != 0};
            for (int index = 0; index < 3; index++) {
                for (int value = 0; value <= split.total() + 1; value++) {
                    var changed = split.adjust(index, value, locks, new int[]{split.total(), split.total(), split.total()});
                    assertThat(changed.total()).isEqualTo(split.total());
                    for (int area = 0; area < 3; area++) {
                        assertThat(changed.at(area)).isBetween(0, split.total());
                        if (locks[area]) { assertThat(changed.at(area)).isEqualTo(split.at(area)); }
                    }
                }
            }
        }
    }
}
