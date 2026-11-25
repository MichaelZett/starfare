package de.zettsystems.starfare.economy.values;

/** Client command bound to the round and the split visible when its editor was opened. */
public record ProductionAllocation(int systemId, int turn, int expansionPoints,
                                   @org.jspecify.annotations.Nullable AllocationSplit expected) {
    public ProductionAllocation(int systemId, int turn, int expansionPoints) { this(systemId, turn, expansionPoints, null); }
}
