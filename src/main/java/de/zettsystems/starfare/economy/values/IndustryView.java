package de.zettsystems.starfare.economy.values;

/** Only exposed for fully visible systems of the extended rules, never for sensor estimates. */
public record IndustryView(int capacity, int expansionAllocation, int expansionProgress,
                           int expansionCost, int nextDelivery, boolean deliveryBottleneck) {
    public int shipbuilding() { return capacity - expansionAllocation; }
    public boolean atMaximum() { return capacity == EconomyRules.MAX_CAPACITY; }
    /** Expansion progress after the next round, capped at the price of the current level. */
    public int nextProgress() { return Math.min(expansionCost, expansionProgress + expansionAllocation); }
    /** Rounds until the current level completes with this allocation; 0 without allocation or at the maximum. */
    public int turnsToExpansion() {
        return expansionAllocation == 0 || atMaximum() ? 0
                : Math.ceilDiv(expansionCost - expansionProgress, expansionAllocation);
    }
}
