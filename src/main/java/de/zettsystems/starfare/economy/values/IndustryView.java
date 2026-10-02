package de.zettsystems.starfare.economy.values;

/** Only exposed for fully visible Spaceward systems, never for sensor estimates. */
public record IndustryView(int capacity, int expansionAllocation, int expansionProgress,
                           int expansionCost, int nextDelivery, boolean deliveryBottleneck, int reserveShortfall) {
    public int shipbuilding() { return capacity - expansionAllocation; }
    public boolean atMaximum() { return capacity == EconomyRules.MAX_CAPACITY; }
    public int turnsToExpansion() {
        return expansionAllocation == 0 || atMaximum() ? 0
                : Math.ceilDiv(expansionCost - expansionProgress, expansionAllocation);
    }
}
