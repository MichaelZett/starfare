package de.zettsystems.starfare.economy.values;

/** Only exposed for fully visible Spaceward systems, never for sensor estimates. */
public record IndustryView(int capacity, int expansionAllocation, int expansionProgress,
                           int expansionCost, int nextDelivery, boolean deliveryBottleneck,
                           @org.jspecify.annotations.Nullable ColonyView colony,
                           @org.jspecify.annotations.Nullable MetalView metal, int researchAllocation,
                           de.zettsystems.starfare.research.values.@org.jspecify.annotations.Nullable ShipCohorts composition) {
    public IndustryView(int capacity, int expansionAllocation, int expansionProgress,
                        int expansionCost, int nextDelivery, boolean deliveryBottleneck,
                        @org.jspecify.annotations.Nullable ColonyView colony, @org.jspecify.annotations.Nullable MetalView metal) {
        this(capacity, expansionAllocation, expansionProgress, expansionCost, nextDelivery, deliveryBottleneck,
                colony, metal, 0, null);
    }
    public IndustryView(int capacity, int expansionAllocation, int expansionProgress,
                        int expansionCost, int nextDelivery, boolean deliveryBottleneck,
                        @org.jspecify.annotations.Nullable ColonyView colony) {
        this(capacity, expansionAllocation, expansionProgress, expansionCost, nextDelivery, deliveryBottleneck, colony, null);
    }
    public IndustryView(int capacity, int expansionAllocation, int expansionProgress,
                        int expansionCost, int nextDelivery, boolean deliveryBottleneck) {
        this(capacity, expansionAllocation, expansionProgress, expansionCost, nextDelivery,
                deliveryBottleneck, null);
    }
    public int usableCapacity() { return (colony == null || !colony.laborLimited() ? capacity : Math.min(capacity, colony.labor())) - researchAllocation; }
    public int shipbuilding() { return metal == null ? usableCapacity() - expansionAllocation : metal.actualShips(); }
    public boolean atMaximum() { return capacity == EconomyRules.MAX_CAPACITY; }
    public int turnsToExpansion() {
        return expansionAllocation == 0 || atMaximum() ? 0
                : Math.ceilDiv(expansionCost - expansionProgress, expansionAllocation);
    }
}
