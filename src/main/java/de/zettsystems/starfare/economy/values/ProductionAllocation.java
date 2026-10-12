package de.zettsystems.starfare.economy.values;

/** Client command bound to the round visible when its editor was opened. */
public record ProductionAllocation(int systemId, int turn, int expansionPoints, int miningPoints,
                                    @org.jspecify.annotations.Nullable AllocationSplit expected) {
    public ProductionAllocation(int systemId, int turn, int expansionPoints, int miningPoints) {
        this(systemId, turn, expansionPoints, miningPoints, null);
    }
    public ProductionAllocation(int systemId, int turn, int expansionPoints) { this(systemId, turn, expansionPoints, -1); }
}
