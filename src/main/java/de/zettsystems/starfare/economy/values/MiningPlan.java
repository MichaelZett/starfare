package de.zettsystems.starfare.economy.values;

import java.util.Map;

/** Whole-empire mining proposal, including unmet reserve and the last visible assignment changes. */
public record MiningPlan(Map<Integer, Integer> allocations, Map<Integer, Change> changes,
                         long demand, long target, long stock, long shortfall) {
    public record Change(int before, int after) { }
    public static final MiningPlan EMPTY = new MiningPlan(Map.of(), Map.of(), 0, 0, 0, 0);
    public MiningPlan {
        allocations = Map.copyOf(allocations);
        changes = Map.copyOf(changes);
    }
}
