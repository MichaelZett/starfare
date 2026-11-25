package de.zettsystems.starfare.economy.values;

/** Industry expansion prices of the extended rules: cheap first levels, twice the capacity from 20 on. */
public final class EconomyRules {
    public static final int MAX_CAPACITY = 50;
    private static final int MIN_EXPANSION_COST = 2;
    private EconomyRules() { }

    /** Price of the next level: {@code min(2C, max(2, ceil(C²/10)))}. */
    public static int expansionCost(int capacity) {
        int full = 2 * capacity;
        return Math.clamp(Math.ceilDiv(capacity * capacity, 10), Math.min(MIN_EXPANSION_COST, full), full);
    }
}
