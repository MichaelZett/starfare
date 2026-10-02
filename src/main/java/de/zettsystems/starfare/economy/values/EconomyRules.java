package de.zettsystems.starfare.economy.values;

/** First Spaceward rule version; changing balance requires a new stored rule version. */
public final class EconomyRules {
    public static final int MAX_CAPACITY = 50;
    public static final int EXPANSION_COST_FACTOR = 2;
    private EconomyRules() { }
    public static int expansionCost(int capacity) { return capacity * EXPANSION_COST_FACTOR; }
}
