package de.zettsystems.starfare.economy.domain;

import de.zettsystems.starfare.economy.values.EconomyRules;

/** Immutable per-system industry, separate from Classic ship production. */
public record Industry(int capacity, int expansionAllocation, int expansionProgress) {
    public Industry {
        if (capacity < 1 || capacity > EconomyRules.MAX_CAPACITY || expansionAllocation < 0
                || expansionAllocation > capacity || expansionProgress < 0
                || expansionProgress >= EconomyRules.expansionCost(capacity)
                || (capacity == EconomyRules.MAX_CAPACITY && expansionAllocation != 0)) {
            throw new IllegalArgumentException("Invalid industrial capacity, allocation or progress");
        }
    }
    public static Industry establish(int capacity) { return new Industry(capacity, 0, 0); }
    public int shipbuilding() { return capacity - expansionAllocation; }
    public int expansionCost() { return EconomyRules.expansionCost(capacity); }
    public Industry allocateExpansion(int points) { return new Industry(capacity, points, expansionProgress); }
    public Industry captured() { return new Industry(capacity, 0, expansionProgress); }
    public Industry expandForOneTurn() {
        if (capacity == EconomyRules.MAX_CAPACITY || expansionAllocation == 0) { return this; }
        int progress = expansionProgress + expansionAllocation;
        if (progress < expansionCost()) { return new Industry(capacity, expansionAllocation, progress); }
        int nextCapacity = capacity + 1;
        return new Industry(nextCapacity, nextCapacity == EconomyRules.MAX_CAPACITY ? 0 : expansionAllocation,
                progress - expansionCost());
    }
}
