package de.zettsystems.starfare.economy.domain;

import de.zettsystems.starfare.economy.values.EconomyRules;
import de.zettsystems.starfare.game.values.RulesetRef;

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
    public int expansionCost(RulesetRef rules) { return EconomyRules.expansionCost(capacity, rules); }
    public Industry expandForOneTurn() { return expandForOneTurn(RulesetRef.SPACEWARD); }
    public Industry expandForOneTurn(RulesetRef rules) {
        if (capacity == EconomyRules.MAX_CAPACITY || expansionAllocation == 0) { return this; }
        int progress = expansionProgress + expansionAllocation;
        int nextCapacity = capacity;
        while (nextCapacity < EconomyRules.MAX_CAPACITY && progress >= EconomyRules.expansionCost(nextCapacity, rules)) {
            progress -= EconomyRules.expansionCost(nextCapacity, rules);
            nextCapacity++;
        }
        return new Industry(nextCapacity, nextCapacity == EconomyRules.MAX_CAPACITY ? 0 : expansionAllocation, progress);
    }
}
