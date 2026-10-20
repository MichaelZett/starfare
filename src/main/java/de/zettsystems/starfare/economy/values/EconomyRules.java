package de.zettsystems.starfare.economy.values;

import de.zettsystems.starfare.game.values.RulesetRef;

/** Versioned Spaceward expansion prices; stored games retain their original curve. */
public final class EconomyRules {
    public static final int MAX_CAPACITY = 50;
    public static final int EXPANSION_COST_FACTOR = 2;
    private EconomyRules() { }
    public static int expansionCost(int capacity, RulesetRef rules) {
        return rules.usesCheapEarlyIndustry()
                ? Math.min(expansionCost(capacity), Math.max(2, Math.ceilDiv(capacity * capacity, 10)))
                : expansionCost(capacity);
    }
    public static int expansionCost(int capacity) { return capacity * EXPANSION_COST_FACTOR; }
}
