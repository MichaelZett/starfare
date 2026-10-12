package de.zettsystems.starfare.economy.values;

/** Exact resources are shown only with complete system visibility. */
public record MetalView(long remaining, int yieldPerPoint, int miningPoints, long stock,
                        long extraction, int plannedShips, int actualShips,
                        @org.jspecify.annotations.Nullable RecyclingView recycling) {
    public MetalView(long remaining, int yieldPerPoint, int miningPoints, long stock,
                     long extraction, int plannedShips, int actualShips) {
        this(remaining, yieldPerPoint, miningPoints, stock, extraction, plannedShips, actualShips, null);
    }
}
