package de.zettsystems.starfare.economy.values;

import de.zettsystems.starfare.game.values.RulesetRef;

/** Versioned Spaceward expansion prices; stored games retain their original curve. */
public final class EconomyRules {
    public static final int MAX_CAPACITY = 50;
    public static final int EXPANSION_COST_FACTOR = 2;
    private EconomyRules() { }
    public static int expansionCost(int capacity, RulesetRef rules) {
        int full = expansionCost(capacity);
        // The floor of 2 never exceeds the regular cost; a zero-capacity colony stays at 0.
        return rules.usesCheapEarlyIndustry() ? Math.clamp(Math.ceilDiv(capacity * capacity, 10), Math.min(2, full), full) : full;
    }
    public static int expansionCost(int capacity) { return capacity * EXPANSION_COST_FACTOR; }
}
